package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import broadtranslator.json.TranslatorIdsJsonWriting.entityIdWrites
import play.api.libs.json.{ JsNumber, JsString, JsArray, JsValue, Json, Writes }

/**
 * broadtranslator
 * Created by oliverr on 4/5/2017.
 */
object EvaluateResultJsonWriting {

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

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(group: VariableGroup): JsValue = Json.obj(
      "variableGroupID" -> group.groupId,
      "modelVariable" -> group.modelVariable)
  }

  implicit val evaluateResultWrites: Writes[EvaluateModelResult] = new Writes[EvaluateModelResult] {
    override def writes(result: EvaluateModelResult): JsValue = Json.obj(
      "posteriorProbabilities" -> result.posteriorProbabilities)
  }

}
