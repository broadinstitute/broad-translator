package broadtranslator.engine.api.evaluate

import broadtranslator.engine.api.id.VariableId

/**
 * Created by oruebenacker on 4/6/17.
 */
case class ModelVariable(variableId: VariableId, probabilityDistribution: ProbabilityDistribution)
    extends AnyRef with Ordered[ModelVariable] {
  
  def compare(that: ModelVariable) = this.variableId.string.compare(that.variableId.string)
}

