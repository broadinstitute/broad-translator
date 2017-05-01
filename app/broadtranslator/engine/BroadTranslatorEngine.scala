package broadtranslator.engine

import scala.sys.process._
import scala.collection.mutable.{Map => MMap, Set => MSet}

import java.io.File
import java.net.URI
import java.io.PrintWriter
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader

import broadtranslator.engine.api._
import broadtranslator.engine.api.smart.SmartSpecs

import play.api.Logger



class BroadTranslatorEngine extends TranslatorEngine {
  
  
  private val logger = Logger(getClass)
  private val processLoger = ProcessLogger(out => logger.info("[R]: "+out), err => logger.error("[R]: "+err))
  
  private val modelFolder = "models"

  
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
      if File(File(File(modelFolder), folder), "main.R").exists;
      if File(File(File(modelFolder), folder), "modelSignature.txt").exists
    ) yield folder
    ModelListResult(models.map(ModelId(_)))
  }
    

  override def getModelSignature(modelId: ModelId): ModelSignatureResult = {
    val (ioMap, varMap) = loadModelSignature(modelId)
    val response = createSignatureResult(modelId.string, ioMap, varMap)
    return response
  }

  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): VariablesByGroupResult = {
    val (ioMap, varMap) = loadModelSignature(modelId)
    val group = createVariableMap(modelId.string, groupId.string, ioMap(groupId.string), varMap(groupId.string))
    val variables = for ((variable, properties) <- varMap(groupId.string)) yield VariableId(variable)
    return VariablesByGroupResult(group, variables.toSeq)
  }
    
  override def evaluate(request: EvaluateRequest): EvaluateResult = {
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
  
  
  private def loadModelSignature(modelId: ModelId): (MMap[String,MSet[String]], MMap[String,MMap[String,VariableSignature]]) = {
    val file = File(modelFolder+"/"+modelId.string+"/modelSignature.txt")
    logger.info("loading model signature for "+modelId.string+" from "+file)
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine()) 
    val ioMap = MMap[String,MSet[String]]()
    val varMap = MMap[String,MMap[String,VariableSignature]]()
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
      group(variableName) = VariableSignature(uri, valueType, values)
      
      line = input.readLine()
    }
    input.close()
    (ioMap, varMap)
  }
  
  
  private def createSignatureResult(modelId: String, ioMap: MMap[String,MSet[String]], varMap: MMap[String,MMap[String,VariableSignature]]): ModelSignatureResult = {
    val groups = for ((groupId, io) <- ioMap) yield ( VariableGroupId(groupId) -> createVariableMap(modelId, groupId, ioMap(groupId), varMap(groupId)))
    return ModelSignatureResult(ModelId(modelId), groups.toMap)
  }

  
  private def createVariableMap(modelId: String, groupId: String, ioSet: MSet[String], variables: MMap[String, VariableSignature]): VariableGroup = {
    val asInput = ioSet.contains("input") || ioSet.contains("input;output")
    val asOutput = ioSet.contains("output") || ioSet.contains("input;output")
    val uriSet = variables.values.map(_.uri).toSet
    val uri = if (uriSet.size == 1) Some(uriSet.toSeq(0)) else None
    val typeSet = variables.values.map(_.valueType).toSet
    val valueType = if (typeSet.size == 1) Some(typeSet.toSeq(0)) else None
    val valuesSet = variables.values.map(_.values).toSet
    val valuesString = if (valuesSet.size == 1) Some(valuesSet.toSeq(0)) else None
    logger.info("Homogenious variable properties (model=" + modelId + " group=" + groupId + "): URI=" + uri + " type=" + valueType + " values=" + valuesString)
    val valueSet: VarValueSet = valuesString match {
      case None => VarValueSet.AnyString
      case Some(string) => valueType match {
        case None            => VarValueSet.StringList(string.split(";"))
        case Some(NumberType)  => VarValueSet.NumberList(string.split(";").map(_.toDouble))
        case Some(BooleanType) => VarValueSet.Boolean
        case _               => VarValueSet.StringList(string.split(";"))
      }
    }
    return VariableGroup(ModelId(modelId), VariableGroupId(groupId), VariableURI(uri), asInput, asOutput, valueSet)
  }
    
  
  private def exportRequest(request: EvaluateRequest, file: File): Unit = {
    val out = new PrintWriter(new FileWriter(file))
    out.println("io\tvariableGroup\tvariableName\tvariableValue\tprobability")
    for (
      group <- request.priors;
      variableGroup = group.groupId.string;
      variable <- group.varsWithProbs;
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
      group <- request.outputs;
      variableGroup = group.groupId.string;
      variable <- group.variableIds;
      variableName = variable.string
    ) {
      out.println("output\t" + variableGroup + "\t" + variableName + "\t\t")
    }
    out.close()
  }


  private def executeModelCall(modelId: ModelId, input: File, output: File): Unit = {
    val cmd = "Rscript main.R " + input + " " + output
    logger.info("executing model: " + cmd)
    val exitCode = Process(cmd, File(File(modelFolder), modelId.string)).!(processLoger)
    logger.info("executing model: exit code = "+exitCode)
    if (exitCode != 0) {
      logger.error("Failed to execute model "+modelId)
      throw new java.io.IOException("Failed to execute model "+modelId.string)
    }
  }
  
  
  private def importResponse(modelId: ModelId, file: File): EvaluateResult = {
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
 
  
  private def createEvaluateResult(response: MMap[String,MMap[String,MMap[String,Double]]], signature: MMap[String,MMap[String,VariableSignature]]): EvaluateResult = {
    var groupList = List[GroupWithProbabilities]()
    for ((groupName, group) <- response) {
      val variableList = for ((variableName, distribution) <- group) yield 
        VariableWithProbabilities(VariableId(variableName), createDistribution(distribution, signature(groupName)(variableName).valueType))
      groupList = GroupWithProbabilities(VariableGroupId(groupName), variableList.toSeq) :: groupList
    }
    return EvaluateResult(groupList)
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
  
  private case class VariableSignature(uri: String, valueType: ValueType, values: String)
  
}