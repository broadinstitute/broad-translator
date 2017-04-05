package broadtranslator.engine

import broadtranslator.engine.api.{EvaluateRequest, EvaluateResult, GroupAndVariables, ModelId, ModelListResult, ModelSignatureResult, VariableGroup, VariableGroupId}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
trait TranslatorEngine {
  def getAvailableModelIds: ModelListResult

  def getModelSignature(modelId: ModelId): ModelSignatureResult

  def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupAndVariables

  def evaluate(request: EvaluateRequest): EvaluateResult
}
