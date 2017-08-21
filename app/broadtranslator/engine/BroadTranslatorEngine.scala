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
import broadtranslator.json.SignatureJsonReading

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
    loadModelSignature(modelId) match {
      case Some(modelSignature) => modelSignature
      case None => throw new java.io.IOException("failed to load model signature for "+modelId.string)
    }
  }


  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupSignatureResult = {
    val group = loadModelSignature(modelId) match {
      case Some(modelSignature) => modelSignature.variableGroup(groupId)
      case None => throw new java.io.IOException("failed to load model signature for "+modelId.string)
    }
    GroupSignatureResult(modelId, groupId, group.modelVariable)
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
  

  private def loadModelSignature(modelId: ModelId): Option[ModelSignatureResult] = {
    val file = modelFolder+"/"+modelId.string+"/modelSignature.json"
    logger.info("loading model signature for " + modelId.string + " from " + file)
    SignatureJsonReading.loadModelSignature(file)
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
  
    private def loadModelSignatureTxt(modelId: ModelId): (MMap[String,MSet[String]], MMap[String,MMap[String,VariableSig]]) = {
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
  
  

  private def importResponse(modelId: ModelId, file: File): EvaluateModelResult = {
    val (ioMap, varMap) = loadModelSignatureTxt(modelId)
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

