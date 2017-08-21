package broadtranslator.json

import broadtranslator.engine.api.id._

import broadtranslator.engine.api.evaluate._
import play.api.libs.json.{ JsBoolean, JsNumber, JsObject, JsString, JsArray, JsValue, Json, Writes }
import util.MatchNumber

/**
 * broadtranslator
 * Created by oliverr on 4/5/2017.
 */
object TranslatorJsonWriting {

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val variableWithProbabilitiesWrites: Writes[ModelVariable] =
    new Writes[ModelVariable] {
      override def writes(varWithProbs: ModelVariable): JsValue = {
        val varJson = Json.obj("variableID" -> varWithProbs.variableId)
        val probJson = varWithProbs.probabilityDistribution match {
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
                  "posteriorProbability" -> probability)
            })
            Json.obj("posteriorDistribution" -> probsJson)
          case ProbabilityDistribution.Gaussian(mean, sigma) => Json.obj(
            "mean" -> mean,
            "sigma" -> sigma)
        }
        varJson ++ probJson
      }
    }

  implicit val groupWithProbabilitiesWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(groupWithProbs: VariableGroup): JsValue = Json.obj(
      "variableGroupID" -> groupWithProbs.groupId,
      "modelVariable" -> groupWithProbs.modelVariable)
  }

  implicit val evaluateResultWrites: Writes[EvaluateModelResult] = new Writes[EvaluateModelResult] {
    override def writes(result: EvaluateModelResult): JsValue = Json.obj(
      "posteriorProbabilities" -> result.posteriorProbabilities)
  }

}
