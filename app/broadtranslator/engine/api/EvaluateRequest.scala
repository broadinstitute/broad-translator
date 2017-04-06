package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class EvaluateRequest(modelId: ModelId, outputs: Seq[OutputGroup], constraints: Seq[ConstraintGroup]) {

}
