package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class EvaluateRequest(modelId: ModelId, outputs: Map[VariableGroupId, Seq[VariableId]],
                           constraints: Map[VariableGroupId, Map[VariableId, VariableConstraint]]) {

}
