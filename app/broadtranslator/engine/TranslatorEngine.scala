package broadtranslator.engine

import broadtranslator.engine.api.{GroupAndVariables, ModelId, VariableGroup, VariableGroupId}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
trait TranslatorEngine {

  def getAvailableModelIds: Seq[ModelId]

  def getModelSignature(modelId: ModelId): Seq[VariableGroup]

  def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupAndVariables

}