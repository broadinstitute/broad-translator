package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api._
import broadtranslator.json.TranslatorJsonReading.{evaluateRequestReads, modelSignatureRequestReads, variablesByGroupRequestReads}
import broadtranslator.json.TranslatorJsonWriting.{evaluateResultWrites, modelListResultWrites, modelSignatureResultWrites, variablesByGroupResultWrites}
import play.api.libs.json._

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class TranslatorJsonApi(engine: TranslatorEngine) {

  def getModelList: JsValue = Json.toJson(engine.getAvailableModelIds)

  def getModelSignature(request: JsValue): JsValue =
    callWrapperJson[ModelSignatureRequest, ModelSignatureResult](request,
      request => engine.getModelSignature(request.modelId))

  def getVariablesByGroup(request: JsValue): JsValue =
    callWrapperJson[VariablesByGroupRequest, VariablesByGroupResult](request, engine.getVariablesByGroup)

  def evaluate(request: JsValue): JsValue =
    callWrapperJson[EvaluateRequest, EvaluateResult](request, engine.evaluate)

  def callWrapperJson[Q, A](request: JsValue, fun: Q => A)
                           (implicit reads: Reads[Q], writes: Writes[A]): JsValue = {
    request.validate[Q] match {
      case success: JsSuccess[Q] => Json.toJson(fun(success.get))
      case error: JsError => JsError.toJson(error)
    }
  }

}
