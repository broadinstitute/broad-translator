package broadtranslator.engine.api.evaluate

import broadtranslator.engine.api.id._

/**
 * broadtranslator
 * Created by oliverr on 4/4/2017.
 */
case class EvaluateModelRequest(modelId: ModelId, modelInput: Seq[VariableGroup], modelOutput: Seq[OutputGroup]) {

}
