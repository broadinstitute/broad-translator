package broadtranslator.engine

import scala.sys.process._
import scala.collection.mutable.{Map => MMap, Set => MSet}

import java.io.File
import java.net.URI
import java.io.PrintWriter
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.signature._
import broadtranslator.engine.api.evaluate._
import broadtranslator.engine.api.smart.SmartSpecs

import play.api.Logger



class BroadTranslatorEngine extends TranslatorEngine {
  
  
  private val logger = Logger(getClass)
  private val processLoger = ProcessLogger(out => logger.info("[R]: " + out), err => logger.error("[R]: " + err))
  
  private val modelFolder = "models"
  private val commandTXT = "command.txt"
  
  private object File {
    def apply(filename: String) = new File(filename)
    def apply(parent: File, filename: String) = new File(parent, filename)
    def createTempFile(prefix: String, suffix: String) = java.io.File.createTempFile(prefix, suffix)
  }
  

  override def getSmartSpecs(modelId: ModelId): SmartSpecs =
    SmartSpecs(modelId.string, new URI("http://www.broadinstitute.org/translator"))


    override def getAvailableModelIds: ModelListResult = {
    val models = for (
      folder <- File(modelFolder).list();
      if File(File(File(modelFolder), folder), commandTXT).exists;
      if File(File(File(modelFolder), folder), "modelSignature.txt").exists
    ) yield folder
    ModelListResult(models.sorted.map(ModelId(_)))
  }
    

  override def getModelSignature(modelId: ModelId): ModelSignatureResult = {
    val (ioMap, varMap) = loadModelSignature(modelId)
    val response = createSignatureResult(modelId.string, ioMap, varMap)
    return response
  }


  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupSignatureResult = {
    val (ioMap, varMap) = loadModelSignature(modelId)
    //val group = createVariableMap(modelId.string, groupId.string, ioMap(groupId.string), varMap(groupId.string))
    val uriMap = uniqueValueMap(varMap(groupId.string).values.map(_.uri))(VariableURI(_))
    val typeMap = uniqueValueMap(varMap(groupId.string).values.map(_.valueType))
    val valuesMap = uniqueValueMap(varMap(groupId.string).values.map(_.values))
    val variables = for ((variable, properties) <- varMap(groupId.string)) yield 
      ModelVariableSignature(VariableId(variable), uriMap(properties.uri), None, typeMap(properties.valueType), valuesMap(properties.values).map(valueList(properties.valueType)))
    return GroupSignatureResult(modelId, groupId, variables.toSeq.sorted)
  }
    
  
  override def evaluate(request: EvaluateModelRequest): EvaluateModelResult = {
    val inputFile = File.createTempFile("translatorRequest", ".txt")
    val outputFile = File.createTempFile("translatorResponse", ".txt")
    var success = false
    try {
      exportRequest(request, inputFile)
      executeModelCall(request.modelId, inputFile, outputFile)
      val response = importResponse(request.modelId, outputFile)
      success = true
      return response
    }
    finally {
      if (success) {
        inputFile.delete()
        outputFile.delete()

      }
      else {
        inputFile.deleteOnExit()
        outputFile.deleteOnExit()
      }
    }
    
  }
  

  private def loadModelSignature(modelId: ModelId): (MMap[String,MSet[String]], MMap[String,MMap[String,VariableSig]]) = {
    val file = File(modelFolder+"/"+modelId.string+"/modelSignature.txt")
    logger.info("loading model signature for "+modelId.string+" from "+file)
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine()) 
    val ioMap = MMap[String,MSet[String]]()
    val varMap = MMap[String,MMap[String,VariableSig]]()
    var line = input.readLine()
    while (line != null) {
      val row = line.split("\t")
      val io = row(header("io"))
      val groupName = row(header("variableGroup"))
      val variableName = row(header("variableName"))
      val uri = row(header("uri"))
      val valueType = row(header("valueType")) match {
        case "Number" => NumberType
        case "Boolean" => BooleanType
        case "String" => StringType
        case wrongType => {
          logger.error("incorrect value type "+wrongType+" (model="+modelId+", group="+groupName+", variable="+variableName)
          throw new IllegalArgumentException("incorrect value type "+wrongType+" (model="+modelId+", group="+groupName+", variable="+variableName)
        }
      }
      val values = row(header("allowedVariableValues"))

      if (!ioMap.contains(groupName)) ioMap(groupName) = MSet()
      ioMap(groupName) add io
      
      if (!varMap.contains(groupName)) varMap(groupName) = MMap()
      val group = varMap(groupName)
      group(variableName) = VariableSig(uri, valueType, values)
      
      line = input.readLine()
    }
    input.close()
    (ioMap, varMap)
  }
  
  
  private def createSignatureResult(modelId: String, ioMap: MMap[String,MSet[String]], varMap: MMap[String,MMap[String,VariableSig]]): ModelSignatureResult = {
    val groups = for ((groupId, io) <- ioMap) yield ( VariableGroupId(groupId) -> createVariableMap(modelId, groupId, ioMap(groupId), varMap(groupId)))
    return ModelSignatureResult(ModelId(modelId), groups.toMap)
  }

  
  private def createVariableMap(modelId: String, groupId: String, ioSet: MSet[String], variables: MMap[String, VariableSig]): GroupSignature = {
    val asInput = ioSet.contains("input") || ioSet.contains("input;output")
    val asOutput = ioSet.contains("output") || ioSet.contains("input;output")
    val uriSet = variables.values.map(_.uri).toSet
    val uri = if (uriSet.size == 1) Some(uriSet.toSeq(0)) else None
    val typeSet = variables.values.map(_.valueType).toSet
    val valueType = if (typeSet.size == 1) Some(typeSet.toSeq(0)) else None
    val valuesSet = variables.values.map(_.values).toSet
    val valuesString = if (valuesSet.size == 1) Some(valuesSet.toSeq(0)) else None
    logger.info("Homogenious variable properties (model=" + modelId + " group=" + groupId + "): URI=" + uri + " type=" + valueType + " values=" + valuesString)
    val values: Option[ValueList] = valueType match {
      case None => None
      case Some(valueType) => valuesString.map(valueList(valueType))
    }
    val variableSignatures = getVariablesByGroup(ModelId(modelId), VariableGroupId(groupId))
        //val variableSignatures = for ((variable, properties) <- variables) yield 
      //ModelVariableSignature(VariableId(variable), uriMap(properties.uri), None, typeMap(properties.valueType), valuesMap(properties.values).map(valueList(properties.valueType)))

    return GroupSignature(VariableGroupId(groupId), VariableURI(uri), asInput, asOutput, Some(ProbabilityDistributionName.discrete), valueType, values, variableSignatures.modelVariable)
  }

  
  private def valueList(valueType: ValueType)(sourceList: String): ValueList = valueType match {
    case NumberType => ValueList.NumberList(sourceList.split(";").map(_.toDouble))
    case BooleanType => ValueList.NumberList(List(0,1))
    case StringType => ValueList.StringList(sourceList.split(";"))
  }
  
  
  /* Create a property mapping function for individual variables:
   *   - if values are unique, map to None since property is defined on group level
   *   - if values are not unique, map to actual property
   */
  private def uniqueValueMap[S,T](src: Iterable[S])(implicit f: S => T): S => Option[T] = src.toSet.size match {
    case 1 => { (x: S) => None }
    case _ => { (x: S) =>
      x match {
        case null => None
        case ""   => None
        case _    => Some(f(x))
      }
    }
  }
  

  private def exportRequest(request: EvaluateModelRequest, file: File): Unit = {
    val out = new PrintWriter(new FileWriter(file))
    out.println("io\tvariableGroup\tvariableName\tvariableValue\tprobability")
    for (
      group <- request.modelInput;
      variableGroup = group.groupId.string;
      variable <- group.modelVariable;
      variableName = variable.variableId.string
    ) {
      variable.probabilityDistribution match {
        case ProbabilityDistribution.Discrete(distribution) => for ((value, probability) <- distribution) {
          out.println("input\t" + variableGroup + "\t" + variableName + "\t" + value + "\t" + probability)
        }
        case _ =>
      }
    }
    for (
      group <- request.modelOutput;
      variableGroup = group.groupId.string;
      variable <- group.variableId;
      variableName = variable.string
    ) {
      out.println("output\t" + variableGroup + "\t" + variableName + "\t\t")
    }
    out.close()
  }


  private def executeModelCall(modelId: ModelId, input: File, output: File): Unit = {
    val folder = File(File(modelFolder), modelId.string)
    val command = new BufferedReader(new FileReader(File(folder, commandTXT)))
    val cmd = command.readLine()+" " + input + " " + output
    command.close()
    logger.info("executing model: " + cmd)
    val exitCode = Process(cmd, folder).!(processLoger)
    logger.info("executing model: exit code = "+exitCode)
    if (exitCode != 0) {
      logger.error("Failed to execute model "+modelId)
      throw new java.io.IOException("Failed to execute model "+modelId.string)
    }
  }
  
  
  private def importResponse(modelId: ModelId, file: File): EvaluateModelResult = {
    val (ioMap, varMap) = loadModelSignature(modelId)
    val response: MMap[String, MMap[String, MMap[String, Double]]] = loadResponse(file)
    return createEvaluateResult(response, varMap)
  }
  
  
  private def loadResponse(file: File): MMap[String,MMap[String,MMap[String,Double]]] = {
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine())
    val groups = MMap[String, MMap[String, MMap[String, Double]]]()
    var line = input.readLine()
    while (line != null) {
      val row = line.split("\t")
      if (row(header("io")) == "output") {
        val groupName = row(header("variableGroup"))
        val variableName = row(header("variableName"))
        val value = row(header("variableValue"))
        val probability = row(header("probability")).toDouble
        if (!groups.contains(groupName)) groups(groupName) = MMap()
        val group = groups(groupName)
        if (!group.contains(variableName)) group(variableName) = MMap()
        val variable = group(variableName)
        variable(value) = probability
      }
      line = input.readLine()
    }
    input.close()
    return groups
  }
 
  
  private def createEvaluateResult(response: MMap[String,MMap[String,MMap[String,Double]]], signature: MMap[String,MMap[String,VariableSig]]): EvaluateModelResult = {
    var groupList = List[VariableGroup]()
    for ((groupName, group) <- response) {
      val variableList = for ((variableName, distribution) <- group) yield 
        ModelVariable(VariableId(variableName), createDistribution(distribution, signature(groupName)(variableName).valueType))
      groupList = VariableGroup(VariableGroupId(groupName), variableList.toSeq.sorted) :: groupList
    }
    return EvaluateModelResult(groupList)
  }

  
  private def createDistribution(distribution: MMap[String, Double], valueType: ValueType): ProbabilityDistribution = ProbabilityDistribution.Discrete(
    valueType match {
      case NumberType  => distribution.map(mapKey(_.toDouble)).toMap
      case BooleanType => distribution.map(mapKey(_.toBoolean)).toMap
      case StringType  => distribution.toMap
    })

    
  private def mapKey[T](f: String => T)(pair:(String, Double)) : (T, Double) = (f(pair._1),pair._2)
  

  private def parseHeader(header: String): Map[String,Int] = {
    val map = MMap[String, Int]()
    var i = 0;
    for (columnName <- header.split("\t")) {
      map.put(columnName, i)
      i = i + 1
    }
    return map.toMap
  }
  
  private case class VariableSig(uri: String, valueType: ValueType, values: String)
  
}

object BroadTranslatorEngine {

  import broadtranslator.json.TranslatorJsonWriting.modelSignatureResultWrites
  import play.api.libs.json.Json

  def main(args: Array[String]) {
    val folder = new java.io.File("models")
    for (modelId <- folder.list()) {
      println(modelId)
      signatureToJson(modelId)
    }
    //signatureToJson("GoodModel")
  }

  def signatureToJson(modelID: String) {
    val engine = new BroadTranslatorEngine
    val signature = engine.getModelSignature(ModelId(modelID))
    println(Json.prettyPrint(Json.toJson(signature)))
    val out = new PrintWriter(new FileWriter("models/" + modelID + "/modelSignature.json"))
    out.println(Json.prettyPrint(Json.toJson(signature)))
    out.close()
  }
}