package broadtranslator.engine

import broadtranslator.engine.api.smart.SmartSpecs
import broadtranslator.engine.api.evaluate.{ EvaluateModelRequest, EvaluateModelResult }
import broadtranslator.engine.api.id.ModelId
import broadtranslator.engine.api.signature.{ ModelListResult, ModelSignatureResult }

/**
 * broadtranslator
 * Created by oliverr on 3/31/2017.
 */

trait TranslatorEngine {
  def getAvailableModelIds: ModelListResult

  def getSmartSpecs(modelId: ModelId): SmartSpecs

  def getModelSignature(modelId: ModelId): ModelSignatureResult

  def evaluate(request: EvaluateModelRequest): EvaluateModelResult
}
