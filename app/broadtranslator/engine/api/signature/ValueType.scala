package broadtranslator.engine.api.signature

sealed abstract class ValueType(val name: String)

case object StringType extends ValueType("String")

case object NumberType extends ValueType("Number")

case object BooleanType extends ValueType("Boolean")
