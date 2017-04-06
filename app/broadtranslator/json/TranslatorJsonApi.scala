package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.{EvaluateRequest, ModelSignatureRequest, ModelSignatureResult, VariablesByGroupRequest}
import broadtranslator.json.TranslatorJsonReading.{evaluateRequestReads, modelSignatureRequestReads, variablesByGroupRequestReads}
import broadtranslator.json.TranslatorJsonWriting.{evaluateResultWrites, modelListResultWrites, modelSignatureResultWrites, variablesByGroupResultWrites}
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json, Reads, Writes}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class TranslatorJsonApi(engine: TranslatorEngine) {

  def getModelList: JsValue = Json.toJson(engine.getAvailableModelIds)

  def getModelSignature(request: JsValue): JsValue =
    callWrapperJson[ModelSignatureRequest, ModelSignatureResult](request,
      request => engine.getModelSignature(request.modelId))

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

  def evaluate(requestJson: JsObject): JsValue = {
    requestJson.validate[EvaluateRequest] match {
      case success: JsSuccess[EvaluateRequest] =>
        val request = success.get
        val result = engine.evaluate(request)
        Json.toJson(result)
      case error: JsError =>
        JsError.toJson(error)
    }
  }

  def callWrapperJson[Q, A](request: JsValue, fun: Q => A)(implicit reads: Reads[Q], writes: Writes[A]): JsValue = {
    request.validate[Q] match {
      case success: JsSuccess[Q] => Json.toJson(fun(success.get))
      case error: JsError => JsError.toJson(error)
    }
  }

}
