package broadtranslator.engine.mock

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.{EvaluateRequest, EvaluateResult, ModelId, ModelListResult, ModelSignatureResult, ProbabilityDistribution, VarValueSet, VariableGroup, VariableGroupId, VariableId, VariablesByGroupRequest, VariablesByGroupResult}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  val applesNames = Seq("Gala", "Granny Smith", "Fuji", "Pink Lady")
  val applesList = VarValueSet.StringList(applesNames)
  val applesGroup = VariableGroupId("apples")
  val appleOneVar = VariableId("apple one")
  val appleTwoVar = VariableId("apple two")

  val orangesNames = Seq("Navel", "Clementine")
  val orangesList = VarValueSet.StringList(orangesNames)
  val orangesGroup = VariableGroupId("oranges")
  val bigOrangeVar = VariableId("big orange")
  val smallOrangeVar = VariableId("small orange")

  override def getAvailableModelIds: ModelListResult =
    ModelListResult(Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId))

  override def getModelSignature(modelId: ModelId): ModelSignatureResult =
    ModelSignatureResult(modelId, Map(
      applesGroup -> VariableGroup(modelId, applesGroup, asConstraints = true, asOutputs = false, applesList),
      orangesGroup -> VariableGroup(modelId, orangesGroup, asConstraints = false, asOutputs = true, orangesList)
    ))

  override def getVariablesByGroup(request: VariablesByGroupRequest): VariablesByGroupResult =
    VariablesByGroupResult(VariableGroup(request, asConstraints = true, asOutputs = false, applesList),
      Seq(appleOneVar, appleTwoVar))

  override def evaluate(request: EvaluateRequest): EvaluateResult =
    EvaluateResult(Map(orangesGroup ->
      Map(
        bigOrangeVar -> ProbabilityDistribution.Discrete(Map("Navel" -> 0.85, "Clementine" -> 0.15)),
        smallOrangeVar -> ProbabilityDistribution.Discrete(Map("Navel" -> 0.07, "Clementine" -> 0.93))
      )
    ))
}
