package broadtranslator.engine.api.signature

import broadtranslator.engine.api.id._

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class GroupSignatureResult(modelId: ModelId, groupId: VariableGroupId, modelVariable: Seq[ModelVariableSignature]) {

}
