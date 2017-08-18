package broadtranslator.engine

import broadtranslator.engine.api.smart.SmartSpecs
import broadtranslator.engine.api.evaluate.{EvaluateModelRequest, EvaluateModelResult}
import broadtranslator.engine.api.id.{ModelId, VariableGroupId}
import broadtranslator.engine.api.signature.{ModelListResult, ModelSignatureResult, GroupSignatureResult}
/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
trait TranslatorEngine {
  def getAvailableModelIds: ModelListResult

  def getSmartSpecs(modelId: ModelId): SmartSpecs

  def getModelSignature(modelId: ModelId): ModelSignatureResult

  def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupSignatureResult

  def evaluate(request: EvaluateModelRequest): EvaluateModelResult
}
