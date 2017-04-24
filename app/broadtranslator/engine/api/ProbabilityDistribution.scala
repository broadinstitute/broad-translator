package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
sealed trait ProbabilityDistribution {

}

object ProbabilityDistribution {

  sealed trait Typed[T] extends ProbabilityDistribution

  case class Discrete[T](probabilities: Map[T, Double]) extends Typed[T]

  case class Gaussian(mean: Double, sigma: Double) extends Typed[Double]

  def apply(probabilities: Iterable[ValueProbability]): Discrete[String] = {
    val probsMap = probabilities.map(valueProbability => (valueProbability.value, valueProbability.probability)).toMap
    Discrete(probsMap)
  }

}
