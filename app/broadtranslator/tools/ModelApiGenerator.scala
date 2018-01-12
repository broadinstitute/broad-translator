package broadtranslator.tools

import play.api.libs.json._
import broadtranslator.engine.api.signature.ModelSignatureResult
import broadtranslator.json.SignatureJsonReading.modelSignatureReads

object ModelApiGenerator {

  def main(args: Array[String]) {
    val modelName = args(0)
    println("Generate API for " + modelName)
    for (signature <- loadModelSignature(modelName)){
      println("Loaded modelSignature.json for "+modelName);
    }
  }

  def loadModelSignature(modelName: String): Option[ModelSignatureResult] = {
    val json = Json.parse(new java.io.FileInputStream("models/"+modelName+"/modelSignature.json"))
    (json.validate[ModelSignatureResult]) match {
      case JsSuccess(signature, _) => Some(signature)
      case JsError(_)              => None
    }
  }
}