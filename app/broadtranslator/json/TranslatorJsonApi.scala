package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api._
import broadtranslator.json.TranslatorJsonReading.{evaluateRequestReads, variablesByGroupRequestReads}
import broadtranslator.json.TranslatorJsonWriting.{evaluateResultWrites, modelListResultWrites, modelSignatureResultWrites, variablesByGroupResultWrites}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads, Writes}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class TranslatorJsonApi(engine: TranslatorEngine) {

  def getModelList: JsValue = Json.toJson(engine.getAvailableModelIds)

  def getModelSignature(modelId: ModelId): JsValue = Json.toJson(engine.getModelSignature(modelId))

  def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): JsValue =
    Json.toJson(engine.getVariablesByGroup(modelId, groupId))

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
