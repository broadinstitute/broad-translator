package util

/**
  * broadtranslator
  * Created by oliverr on 4/11/2017.
  */
trait MatchNumber[N] {

  val fromDouble: Double => N
  val fromFloat: Float => N
  val fromLong: Long => N
  val fromInt: Int => N
  val fromShort: Short => N
  val fromChar: Char => N
  val fromByte: Byte => N

  def unapply(any: Any): Option[N] = any match {
    case double: Double => Some(fromDouble(double))
    case float: Float => Some(fromFloat(float))
    case long: Long => Some(fromLong(long))
    case int: Int => Some(fromInt(int))
    case short: Short => Some(fromShort(short))
    case char: Char => Some(fromChar(char))
    case byte: Byte => Some(fromByte(byte))
    case _ => None
  }

}

object MatchNumber {

  object AsDouble extends MatchNumber[Double] {
    override val fromDouble: (Double) => Double = identity
    override val fromFloat: (Float) => Double = _.toDouble
    override val fromLong: (Long) => Double = _.toDouble
    override val fromInt: (Int) => Double = _.toDouble
    override val fromShort: (Short) => Double = _.toDouble
    override val fromChar: (Char) => Double = _.toDouble
    override val fromByte: (Byte) => Double = _.toDouble
  }

}