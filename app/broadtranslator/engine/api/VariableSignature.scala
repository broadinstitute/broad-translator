package broadtranslator.engine.api

case class VariableSignature(
  variableId: VariableId,
  uri: Option[VariableURI],
  valueType: Option[ValueType],
  values: Option[ValueList])
    extends AnyRef with Ordered[VariableSignature] {

  def this(variableId: VariableId) = this(variableId, None, None, None)
  
  def compare(that: VariableSignature) = this.variableId.string.compare(that.variableId.string)
}
