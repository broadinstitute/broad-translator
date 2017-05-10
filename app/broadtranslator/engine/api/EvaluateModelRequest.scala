package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class EvaluateModelRequest(modelId: ModelId, modelInput: Seq[VariableGroup], modelOutput: Seq[OutputGroup]) {

}
