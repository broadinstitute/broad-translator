package broadtranslator.engine.api

case class ModelVariableSignature(
  variableId: VariableId,
  authorityURI: Option[VariableURI],
  variableDistribution: Option[ProbabilityDistributionName],
  variableType: Option[ValueType],
  variableValue: Option[ValueList])
    extends AnyRef with Ordered[ModelVariableSignature] {

  def this(variableId: VariableId) = this(variableId, None, None, None, None)
  
  def compare(that: ModelVariableSignature) = this.variableId.string.compare(that.variableId.string)
}
