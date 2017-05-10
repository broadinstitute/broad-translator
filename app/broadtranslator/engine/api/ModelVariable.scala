package broadtranslator.engine.api

/**
 * Created by oruebenacker on 4/6/17.
 */
case class ModelVariable(variableId: VariableId, probabilityDistribution: ProbabilityDistribution)
    extends AnyRef with Ordered[ModelVariable] {
  
  def compare(that: ModelVariable) = this.variableId.string.compare(that.variableId.string)
}

object ModelVariable {
  def apply(variableId: VariableId, probabilityDistribution: Seq[ValueProbability]): ModelVariable =
    ModelVariable(variableId, ProbabilityDistribution(probabilityDistribution))

}
