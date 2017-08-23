package broadtranslator.engine.api.evaluate

import play.api.libs.json.JsError

/**
 * broadtranslator
 * Created by oliverr on 4/4/2017.
 */
sealed trait ProbabilityDistribution {

}

object ProbabilityDistribution {

  sealed trait Typed[T] extends ProbabilityDistribution

  case class Discrete[T](probabilities: Map[T, Double]) extends Typed[T]

  case class Gaussian(mean: Double, sigma: Double) extends ProbabilityDistribution

  case class Poisson(lambda: Double) extends ProbabilityDistribution

  case class Empirical(mean: Double, sigma: Double, percentile: Seq[Double]) extends ProbabilityDistribution

  case class Raw(distribution: Seq[Double]) extends ProbabilityDistribution

  object Discrete {
    def apply(probabilities: Iterable[ValueProbability]): Discrete[_] = {
      val probsMap = probabilities.map(valueProbability => (valueProbability.value, valueProbability.probability)).toMap
      probsMap.keys.head match {
        case VariableValue.StringValue(_) => Discrete[String](probsMap.keys.map(variableValue => variableValue match {
          case VariableValue.StringValue(value) => (value, probsMap(variableValue))
        }).toMap)
        case VariableValue.NumberValue(_) => Discrete[Double](probsMap.keys.map(variableValue => variableValue match {
          case VariableValue.NumberValue(value) => (value, probsMap(variableValue))
        }).toMap)
        case VariableValue.BooleanValue(_) => Discrete[Boolean](probsMap.keys.map(variableValue => variableValue match {
          case VariableValue.BooleanValue(value) => (value, probsMap(variableValue))
        }).toMap)
      }
    }
  }
  
  def apply(discrete: Option[Discrete[_]], gaussian: Option[Gaussian], poisson: Option[Poisson], empirical: Option[Empirical]): ProbabilityDistribution =
    apply(discrete, gaussian, poisson, empirical, None)

  def apply(discrete: Option[Discrete[_]], gaussian: Option[Gaussian], poisson: Option[Poisson], empirical: Option[Empirical], raw: Option[Raw]): ProbabilityDistribution = {
    add(add(add(add(add(Seq(), discrete), gaussian), poisson), empirical), raw).head
  }

  private def add(distributions: Seq[ProbabilityDistribution], dist: Option[ProbabilityDistribution]): Seq[ProbabilityDistribution] = dist match {
    case Some(distribution) => distributions :+ distribution
    case None               => distributions
  }
}
