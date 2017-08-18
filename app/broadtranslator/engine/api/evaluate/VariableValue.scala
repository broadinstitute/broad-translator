package broadtranslator.engine.api.evaluate

trait VariableValue {
  def value: Any
}

object VariableValue {

  trait Typed[T] extends VariableValue {
    def value: T
  }

  case class StringValue(value: String) extends Typed[String]

  case class NumberValue(value: Double) extends Typed[Double]

  case class BooleanValue(value: Boolean) extends Typed[Boolean]
}
