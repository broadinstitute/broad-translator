package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import broadtranslator.json.TranslatorIdsJsonWriting.entityIdWrites
import play.api.libs.json.{ JsNumber, JsString, JsArray, JsValue, Json, Writes }

object EvaluateRequestJsonWriting {

  implicit val modelVariableWrites: Writes[ModelVariable] =
    new Writes[ModelVariable] {
      override def writes(modelVariable: ModelVariable): JsValue = {
        val varJson = Json.obj("variableID" -> modelVariable.variableId)
        val probJson = modelVariable.probabilityDistribution match {
          case ProbabilityDistribution.Discrete(probabilities) =>
            val probsJson = probabilities.map({
              case (value, probability) =>
                val valueJson = value match {
                  case string: String => JsString(string)
                  case double: Double => JsNumber(double)
                  case true           => JsNumber(1)
                  case false          => JsNumber(0)
                  case other          => JsString(other.toString)
                }
                Json.obj(
                  "variableValue" -> valueJson,
                  "priorProbability" -> probability)
            })
            Json.obj("priorDistribution" -> probsJson)
          case ProbabilityDistribution.Gaussian(mean, sigma) => Json.obj(
            "mean" -> mean,
            "sigma" -> sigma)
        }
        varJson ++ probJson
      }
    }

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(variableGroup: VariableGroup): JsValue = Json.obj(
      "variableGroupID" -> variableGroup.groupId,
      "modelVariable" -> variableGroup.modelVariable)
  }

  implicit val outputGroupWrites: Writes[OutputGroup] = new Writes[OutputGroup] {
    override def writes(group: OutputGroup): JsValue = Json.obj(
      "variableGroupID" -> group.groupId,
      "variableID" -> group.variableId,
      "rawOutput" -> group.rawOutput)
  }

  implicit val evaluateRequestWrites: Writes[EvaluateModelRequest] = new Writes[EvaluateModelRequest] {
    override def writes(request: EvaluateModelRequest): JsValue = Json.obj(
      "modelID" -> request.modelId,
      "modelInput" -> request.modelInput,
      "modelOutput" -> request.modelOutput)
  }

}