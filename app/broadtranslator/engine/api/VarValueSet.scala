package broadtranslator.engine.api


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



  trait ValuesList[T] extends Typed[T] {
    def values: Seq[T]

    def valuesOpt: Some[Seq[T]] = Some(values)
  }

  trait NotAValuesList[T] extends Typed[T] {
    override val valuesOpt: Option[Seq[T]] = None
  }

  trait StringValues extends Typed[String] {
    override val valueType: ValueType = StringType
  }

  object AnyString extends StringValues with NotAValuesList[String]

  case class StringList(values: Seq[String]) extends StringValues with ValuesList[String]

  trait NumberValues extends Typed[Double] {
    override val valueType: ValueType = NumberType
  }

  case class NumberList(values: Seq[Double]) extends NumberValues with ValuesList[Double]

  case class NumberInterval(min: Double, max: Double) extends NumberValues with NotAValuesList[Double]

  object Boolean extends Typed[Boolean] {
    override def valueType: ValueType = BooleanType

    override def valuesOpt: Option[Seq[Boolean]] = Some(Seq(false, true))
  }

}
