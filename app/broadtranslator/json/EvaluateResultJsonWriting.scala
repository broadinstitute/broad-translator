package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import broadtranslator.json.TranslatorIdsJsonWriting.entityIdWrites
import broadtranslator.json.EvaluateRequestJsonWriting.{ poissonDistributionWrites, gaussianDistributionWrites, empiricalDistributionWrites, valueToJson }
import play.api.libs.json.{ JsValue, Json, Writes }

/**
 * broadtranslator
 * Created by oliverr on 4/5/2017.
 */
object EvaluateResultJsonWriting {

  implicit val discreteDistributionWrites: Writes[(Any, Double)] = new Writes[(Any, Double)] {
    override def writes(probability: (Any, Double)): JsValue = Json.obj(
      "variableValue" -> valueToJson(probability._1),
      "posteriorProbability" -> probability._2)
  }

  implicit val probabilityDistributionWrites: Writes[ProbabilityDistribution] = new Writes[ProbabilityDistribution] {
    override def writes(distribution: ProbabilityDistribution): JsValue = distribution match {
      case ProbabilityDistribution.Discrete(probabilities) => Json.obj("discreteDistribution" -> probabilities)
      case gaussian: ProbabilityDistribution.Gaussian      => Json.obj("GaussianDistribution" -> gaussianDistributionWrites.writes(gaussian))
      case poisson: ProbabilityDistribution.Poisson        => Json.obj("PoissonDistribution" -> poissonDistributionWrites.writes(poisson))
      case empirical: ProbabilityDistribution.Empirical    => Json.obj("empiricalDistribution" -> empiricalDistributionWrites.writes(empirical))
      case raw: ProbabilityDistribution.Raw                => Json.obj("rawDistribution" -> raw.distribution)
      case ProbabilityDistribution.Scalar(value)           => Json.obj("scalarValue" -> ???)
    }
  }

  implicit val modelVariableWrites: Writes[ModelVariable] = new Writes[ModelVariable] {
    override def writes(modelVariable: ModelVariable): JsValue = Json.obj(
      "variableID" -> modelVariable.variableId,
      "posteriorDistribution" -> modelVariable.probabilityDistribution)
  }

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(group: VariableGroup): JsValue = Json.obj(
      "variableGroupID" -> group.groupId,
      "modelVariable" -> group.modelVariable)
  }

  implicit val evaluateResultWrites: Writes[EvaluateModelResult] = new Writes[EvaluateModelResult] {
    override def writes(result: EvaluateModelResult): JsValue = Json.obj(
      "posteriorProbability" -> result.posteriorProbabilities)
  }

}
