package broadtranslator.engine.api

case class ProbabilityDistributionName(name: String) {

}

object ProbabilityDistributionName {

  def discrete = ProbabilityDistributionName("discrete")
  def empirical = ProbabilityDistributionName("empirical")
  def Gaussian = ProbabilityDistributionName("Gaussian")
  def Poisson = ProbabilityDistributionName("Poisson")

}
