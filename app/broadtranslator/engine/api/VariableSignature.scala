package broadtranslator.engine.api

case class VariableSignature(
    variableId: VariableId,
    uri: Option[VariableURI],
    valueType: Option[ValueType],
    values: Option[ValueList]) {

  def this(variableId: VariableId) = this(variableId, None, None, None)
}
