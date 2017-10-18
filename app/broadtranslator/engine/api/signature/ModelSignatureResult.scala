package broadtranslator.engine.api.signature

import broadtranslator.engine.api.id._

/**
 * broadtranslator
 * Created by oliverr on 4/4/2017.
 */
case class ModelSignatureResult(modelId: ModelId, variableGroup: Map[VariableGroupId, GroupSignature]) {

}

object ModelSignatureResult {

  def apply(modelId: ModelId, variableGroup: Seq[GroupSignature]) = new ModelSignatureResult(modelId, variableGroup.map(g => (g.groupId, g)).toMap)

}