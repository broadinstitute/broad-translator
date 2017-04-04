package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.ModelSignatureRequest
import play.api.libs.json.Writes.StringWrites
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import broadtranslator.json.TranslatorJsonReading.modelSignatureRequestReads

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class TranslatorJsonApi(engine: TranslatorEngine) {

  def getModelList: JsObject = {
    val modelIdStrings = engine.getAvailableModelIds.map(_.string)
    Json.obj("models" -> modelIdStrings)
  }

  def getModelSignature(requestJson: JsObject): JsObject = {
    requestJson.validate[ModelSignatureRequest] match {
      case success: JsSuccess[ModelSignatureRequest] =>
        val request = success.get
        val modelId = request.modelId
        val x = engine.getModelSignature(modelId)
        ???
      case error: JsError =>
        ???
    }
  }

}
