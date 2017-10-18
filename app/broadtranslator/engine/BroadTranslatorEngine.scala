package broadtranslator.engine

import scala.sys.process._
import scala.collection.mutable.{Map => MMap, Set => MSet}

import java.io.File
import java.net.URI
import java.io.FileReader
import java.io.BufferedReader

import broadtranslator.engine.api.id.ModelId
import broadtranslator.engine.api.signature.ModelListResult
import broadtranslator.engine.api.signature.ModelSignatureResult
import broadtranslator.engine.api.evaluate.EvaluateModelRequest
import broadtranslator.engine.api.evaluate.EvaluateModelResult
import broadtranslator.engine.api.smart.SmartSpecs
import broadtranslator.json.SignatureJsonReading
import broadtranslator.json.EvaluateRequestJsonWriting
import broadtranslator.json.EvaluateResultJsonReading

import play.api.Logger


class BroadTranslatorEngine extends TranslatorEngine {
  
  
  private val logger = Logger(getClass)
  private val processLoger = ProcessLogger(out => logger.info("[out]: " + out), err => logger.error("[err]: " + err))
  
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
      if File(File(File(modelFolder), folder), "modelSignature.json").exists
    ) yield folder
    ModelListResult(models.sorted.map(ModelId(_)))
  }


  override def getModelSignature(modelId: ModelId): ModelSignatureResult = {
    loadModelSignature(modelId) match {
      case Some(modelSignature) => modelSignature
      case None => throw new java.io.IOException("failed to load model signature for "+modelId.string)
    }
  }


  override def evaluate(request: EvaluateModelRequest): EvaluateModelResult = {
    val inputFile = File.createTempFile("translatorRequest", ".json")
    val outputFile = File.createTempFile("translatorResponse", ".json")
    var success = false
    try {
      EvaluateRequestJsonWriting.saveEvaluateRequest(request, inputFile)
      executeModelCall(request.modelId, inputFile, outputFile)
      val response = EvaluateResultJsonReading.readEvaluateResult(request.modelId, outputFile)
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
  
}

