package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
trait ProbabilityDistribution {

}

object ProbabilityDistribution {

  trait Typed[T] extends ProbabilityDistribution

  case class Discrete[T](probabilities: Map[T, Double]) extends Typed[T]

  case class Gaussian(mean: Double, sigma: Double) extends Typed[Double]

}
