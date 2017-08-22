package broadtranslator.json

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.evaluate.{ EvaluateModelRequest, EvaluateModelResult }
import broadtranslator.engine.api.id.{ ModelId, VariableGroupId }
import broadtranslator.json.EvaluateRequestJsonReading.evaluateRequestReads
import broadtranslator.json.EvaluateResultJsonWriting.evaluateResultWrites
import broadtranslator.json.SignatureJsonWriting.{ modelListResultWrites, modelSignatureResultWrites, variablesByGroupResultWrites }
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json, Reads, Writes }

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
    callWrapperJson[EvaluateModelRequest, EvaluateModelResult](request, engine.evaluate)

  def callWrapperJson[Q, A](request: JsValue, fun: Q => A)(implicit reads: Reads[Q], writes: Writes[A]): JsValue = {
    request.validate[Q] match {
      case success: JsSuccess[Q] => Json.toJson(fun(success.get))
      case error: JsError        => JsError.toJson(error)
    }
  }

}
