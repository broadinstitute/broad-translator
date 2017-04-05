package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.{ModelSignatureRequest, VariablesByGroupRequest}
import broadtranslator.json.TranslatorJsonReading.{modelSignatureRequestReads, variablesByGroupRequestReads}
import broadtranslator.json.TranslatorJsonWriting.{modelListResultWrites, modelSignatureResultWrites, variablesByGroupResultWrites}
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
        val result = engine.getModelSignature(modelId)
        Json.toJson(result)
      case error: JsError =>
        JsError.toJson(error)
    }
  }

  def getVariablesByGroup(requestJson: JsObject): JsValue = {
    requestJson.validate[VariablesByGroupRequest] match {
      case success: JsSuccess[VariablesByGroupRequest] =>
        val request = success.get
        val result = engine.getVariablesByGroup(request)
        Json.toJson(result)
      case error: JsError =>
        JsError.toJson(error)
    }
  }

}
