package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.ModelSignatureRequest
import broadtranslator.json.TranslatorJsonReading.modelSignatureRequestReads
import broadtranslator.json.TranslatorJsonWriting.{modelListResultWrites, modelSignatureResultWrites}
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class TranslatorJsonApi(engine: TranslatorEngine) {

  def getModelList: JsValue = Json.toJson(engine.getAvailableModelIds)

  def getModelSignature(requestJson: JsObject): JsValue = {
    requestJson.validate[ModelSignatureRequest] match {
      case success: JsSuccess[ModelSignatureRequest] =>
        val request = success.get
        val modelId = request.modelId
        val modelSignatureResult = engine.getModelSignature(modelId)
        Json.toJson(modelSignatureResult)
      case error: JsError =>
        JsError.toJson(error)
    }
  }

}
