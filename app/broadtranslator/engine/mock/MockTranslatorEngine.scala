package broadtranslator.engine.mock

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.{EvaluateRequest, EvaluateResult, GroupAndVariables, ModelId, ProbabilityDistribution, VarValueSet, VariableGroup, VariableGroupId, VariableId}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  override def getAvailableModelIds: Seq[ModelId] = Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId)

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

  override def getModelSignature(modelId: ModelId): Seq[VariableGroup] = Seq(
    VariableGroup(modelId, applesGroup, asConstraints = true, asOutputs = false, applesList),
    VariableGroup(modelId, orangesGroup, asConstraints = false, asOutputs = true, orangesList)
  )

  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupAndVariables =
    GroupAndVariables(VariableGroup(modelId, groupId, asConstraints = true, asOutputs = false, applesList),
      Seq(appleOneVar, appleTwoVar))

  override def evaluate(request: EvaluateRequest): EvaluateResult =
    EvaluateResult(Map(orangesGroup ->
      Map(
        bigOrangeVar -> ProbabilityDistribution.Discrete(Map("Navel" -> 0.85, "Clementine" -> 0.15)),
        smallOrangeVar -> ProbabilityDistribution.Discrete(Map("Navel" -> 0.07, "Clementine" -> 0.93))
      )
    ))
}
