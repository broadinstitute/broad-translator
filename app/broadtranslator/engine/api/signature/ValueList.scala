package broadtranslator.engine.api.signature

trait ValueList {
  def values: Seq[Any]
}

object ValueList {

  trait Typed[T] extends ValueList {
    def values: Seq[T]
  }

  case class StringList(values: Seq[String]) extends Typed[String]

  case class NumberList(values: Seq[Double]) extends Typed[Double]

}
