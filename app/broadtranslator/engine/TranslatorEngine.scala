package broadtranslator.engine

import broadtranslator.engine.api.smart.SmartSpecs
import broadtranslator.engine.api.{EvaluateRequest, EvaluateResult, ModelId, ModelListResult, ModelSignatureResult, VariableGroupId, GroupSignatureResult}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
trait TranslatorEngine {
  def getAvailableModelIds: ModelListResult

  def getSmartSpecs(modelId: ModelId): SmartSpecs

  def getModelSignature(modelId: ModelId): ModelSignatureResult

  def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupSignatureResult

  def evaluate(request: EvaluateRequest): EvaluateResult
}
