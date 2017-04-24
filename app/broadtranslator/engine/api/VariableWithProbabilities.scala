package broadtranslator.engine.api

/**
  * LoamStream - Language for Omics Analysis Management
  * Created by oruebenacker on 4/6/17.
  */
case class VariableWithProbabilities(variableId: VariableId, probabilityDistribution: ProbabilityDistribution) {

}

object VariableWithProbabilities {
  def apply(variableId: VariableId, probabilities: Seq[ValueProbability]): VariableWithProbabilities =
    VariableWithProbabilities(variableId, ProbabilityDistribution(probabilities))

}
