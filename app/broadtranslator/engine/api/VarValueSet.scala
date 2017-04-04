package broadtranslator.engine.api

import broadtranslator.engine.api.VarValueSet.ValueType

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
trait VarValueSet {
  def valueType: ValueType

  def valuesOpt: Option[Seq[Any]]
}

object VarValueSet {

  trait Typed[T] extends VarValueSet {
    def valuesOpt: Option[Seq[T]]
  }

  case class ValueType(name: String)

  val stringType = ValueType("String")
  val numberType = ValueType("Number")
  val booleanType = ValueType("Boolean")

  trait ValuesList[T] extends Typed[T] {
    def values: Seq[T]

    def valuesOpt: Some[Seq[T]] = Some(values)
  }

  trait NotAValuesList[T] extends Typed[T] {
    override val valuesOpt: Option[Seq[T]] = None
  }

  trait StringValues extends Typed[String] {
    override val valueType: ValueType = stringType
  }

  object AnyString extends StringValues with NotAValuesList[String]

  case class StringList(values: Seq[String]) extends StringValues with ValuesList[String]

  trait NumberValues extends Typed[Double] {
    override val valueType: ValueType = numberType
  }

  case class NumberList(values: Seq[Double]) extends NumberValues with ValuesList[Double]

  case class NumberInterval(min: Double, max: Double) extends NumberValues with NotAValuesList[Double]

  object Boolean extends Typed[Boolean] {
    override def valueType: ValueType = booleanType

    override def valuesOpt: Option[Seq[Boolean]] = Some(Seq(false, true))
  }

}
