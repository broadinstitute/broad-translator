package broadtranslator.engine

import scala.sys.process._
import scala.collection.mutable.{Map => MMap, Set => MSet}

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
  
  private val modelFolder = "models"
  
  private def File(filename: String) = new java.io.File(filename)
  private def File(parent: java.io.File, filename: String) = new java.io.File(parent, filename)
  
  override def getSmartSpecs(modelId: ModelId): SmartSpecs =
    SmartSpecs(modelId.string, new URI("http://www.broadinstitute.org/translator"))

  override def getAvailableModelIds: ModelListResult = {
    val models = for (
      folder <- File(modelFolder).list();
      if File(File(File(modelFolder), folder), "main.R").exists;
      if File(File(File(modelFolder), folder), "modelSignature.txt").exists
    ) yield folder
    ModelListResult(models.map(ModelId))
  }
    

  override def getModelSignature(modelId: ModelId): ModelSignatureResult = {
    val (ioMap, varMap) = loadModelSignature(modelId.string)
    val response = createSignatureResult(modelId.string, ioMap, varMap)
    return response
  }

  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): VariablesByGroupResult = {
    val (ioMap, varMap) = loadModelSignature(modelId.string)
    val group = createVariableMap(modelId.string, groupId.string, ioMap(groupId.string), varMap(groupId.string))
    val variables = for ((variable, properties) <- varMap(groupId.string)) yield VariableId(variable)
    return VariablesByGroupResult(group, variables.toSeq)
  }
    
  override def evaluate(request: EvaluateRequest): EvaluateResult = {
    val inputFile = java.io.File.createTempFile("translatorRequest", ".txt")
    val outputFile = java.io.File.createTempFile("translatorResponse", ".txt")
    exportRequest(request, inputFile)
    executeModelCall(request.modelId.string, inputFile, outputFile)
    val response = importResponse(outputFile)
    outputFile.deleteOnExit()
    return response
  }
  
  
  private def loadModelSignature(modelId: String): (MMap[String,MSet[String]], MMap[String,MMap[String,(String, String, String)]]) = {
    val file = File(modelFolder+"/"+modelId+"/modelSignature.txt")
    logger.info("loading model signature for "+modelId+" from "+file)
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine()) 
    val ioMap = MMap[String,MSet[String]]()
    val varMap = MMap[String,MMap[String,(String, String, String)]]()
    var line = input.readLine()
    while (line != null) {
      val row = line.split("\t")
      val io = row(header("io"))
      val groupName = row(header("variableGroup"))
      val variableName = row(header("variableName"))
      val uri = row(header("uri"))
      val valueType = row(header("valueType"))
      val values = row(header("allowedVariableValues"))

      if (!ioMap.contains(groupName)) ioMap(groupName) = MSet()
      ioMap(groupName) add io
      
      if (!varMap.contains(groupName)) varMap(groupName) = MMap()
      val group = varMap(groupName)
      group(variableName) = (uri, valueType, values)
      
      line = input.readLine()
    }
    input.close()
    (ioMap, varMap)
  }
  
  
  private def createSignatureResult(modelId: String, ioMap: MMap[String,MSet[String]], varMap: MMap[String,MMap[String,(String, String, String)]]): ModelSignatureResult = {
    val groups = for ((groupId, io) <- ioMap) yield ( VariableGroupId(groupId) -> createVariableMap(modelId, groupId, ioMap(groupId), varMap(groupId)))
    return ModelSignatureResult(ModelId(modelId), groups.toMap)
  }

  
  private def createVariableMap(modelId: String, groupId: String, ioSet: MSet[String], variables: MMap[String, (String, String, String)]): VariableGroup = {
    val asInput = ioSet.contains("input") || ioSet.contains("input; output")
    val asOutput = ioSet.contains("output") || ioSet.contains("input; output")
    val uriSet = (for ((variable, (uri, valueType, values)) <- variables) yield uri).toSet
    val uri = if (uriSet.size == 1) Some(uriSet.toSeq(0)) else None
    val typeSet = (for ((variable, (uri, valueType, values)) <- variables) yield valueType).toSet
    val valueType = if (typeSet.size == 1) Some(typeSet.toSeq(0)) else None
    val valuesSet = (for ((variable, (uri, valueType, values)) <- variables) yield values).toSet
    val valuesString = if (valuesSet.size == 1) Some(valuesSet.toSeq(0)) else None
    logger.info("Homogenious variable properties (model=" + modelId + " group=" + groupId + "): URI=" + uri + " type=" + valueType + " values=" + valuesString)
    val valueSet: VarValueSet = valuesString match {
      case None => VarValueSet.AnyString
      case Some(string) => valueType match {
        case None            => VarValueSet.StringList(string.split(";"))
        case Some("Number")  => VarValueSet.NumberList(string.split(";").map(_.toDouble))
        case Some("Boolean") => VarValueSet.Boolean
        case _               => VarValueSet.StringList(string.split(";"))
      }
    }
    return VariableGroup(ModelId(modelId), VariableGroupId(groupId), asInput, asOutput, valueSet)
  }
    
  
  private def exportRequest(request: EvaluateRequest, file: java.io.File): Unit = {
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


  private def executeModelCall(modelId: String, input: java.io.File, output: java.io.File): Unit = {
    val cmd = "Rscript " + modelFolder + "/" + modelId + "/main.R " + input + " " + output
    logger.info("executing model: " + cmd)
    cmd.! //TODO capture stdout+stderr to logger
  }
  
  
  private def importResponse(file: java.io.File): EvaluateResult = {
    // TODO handle types for response
    val response: MMap[String, MMap[String, MMap[Int, Double]]] = loadResponse(file)(_.toInt)
    return createEvaluateResult(response)
  }
  
  
  private def loadResponse[T](file: java.io.File)(implicit f: String => T): MMap[String,MMap[String,MMap[T,Double]]] = {
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine())
    val groups = MMap[String, MMap[String, MMap[T, Double]]]()
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
 
  
  private def createEvaluateResult[T](response: MMap[String,MMap[String,MMap[T,Double]]]): EvaluateResult = {
    var groupList = List[GroupWithProbabilities]()
    for ((groupName, group) <- response) {
      val variableList = for ((variableName, variable) <- group) yield VariableWithProbabilities(VariableId(variableName), ProbabilityDistribution.Discrete(variable.toMap))
      groupList = GroupWithProbabilities(VariableGroupId(groupName), variableList.toSeq) :: groupList
    }
    return EvaluateResult(groupList)
  }


  private def parseHeader(header: String): Map[String,Int] = {
    val map = MMap[String, Int]()
    var i = 0;
    for (columnName <- header.split("\t")) {
      map.put(columnName, i)
      i = i + 1
    }
    return map.toMap
  }
  

}