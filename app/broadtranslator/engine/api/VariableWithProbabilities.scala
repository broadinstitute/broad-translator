package broadtranslator.engine.api

/**
 * Created by oruebenacker on 4/6/17.
 */
case class VariableWithProbabilities(variableId: VariableId, probabilityDistribution: ProbabilityDistribution)
    extends AnyRef with Ordered[VariableWithProbabilities] {
  
  def compare(that: VariableWithProbabilities) = this.variableId.string.compare(that.variableId.string)
}

object VariableWithProbabilities {
  def apply(variableId: VariableId, probabilities: Seq[ValueProbability]): VariableWithProbabilities =
    VariableWithProbabilities(variableId, ProbabilityDistribution(probabilities))

}
