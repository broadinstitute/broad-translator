package broadtranslator.engine.api

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

  case class Gaussian(mean: Double, sigma: Double) extends Typed[Double]

  def apply(probabilities: Iterable[ValueProbability]): ProbabilityDistribution = {
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
