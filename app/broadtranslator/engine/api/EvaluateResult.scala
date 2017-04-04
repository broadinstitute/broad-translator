package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class EvaluateResult(groupProbabilities: Map[VariableGroupId, Map[VariableId, ProbabilityDistribution]]) {

}
