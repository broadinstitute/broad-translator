package broadtranslator.engine

import broadtranslator.engine.api.smart.SmartSpecs
import broadtranslator.engine.api.{EvaluateRequest, EvaluateResult, ModelId, ModelListResult, ModelSignatureResult, VariablesByGroupRequest, VariablesByGroupResult}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
trait TranslatorEngine {
  def getAvailableModelIds: ModelListResult

  def getSmartSpecs(modelId: ModelId): SmartSpecs

  def getModelSignature(modelId: ModelId): ModelSignatureResult

  def getVariablesByGroup(request: VariablesByGroupRequest): VariablesByGroupResult

  def evaluate(request: EvaluateRequest): EvaluateResult
}
