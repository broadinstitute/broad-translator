package broadtranslator.engine.mock

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.{GroupAndVariables, ModelId, VarValueSet, VariableGroup, VariableGroupId, VariableId}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  override def getAvailableModelIds: Seq[ModelId] = Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId)

  val apples = Seq("Gala", "Granny Smith", "Fuji", "Pink Lady")

  override def getModelSignature(modelId: ModelId): Seq[VariableGroup] = Seq(
    VariableGroup(modelId, VariableGroupId("apples"), asConstraints = true, asOutputs = false,
      VarValueSet.StringList(apples)),
    VariableGroup(modelId, VariableGroupId("oranges"), asConstraints = false, asOutputs = true,
      VarValueSet.StringList(Seq("Navel", "Clementine")))
  )

  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): GroupAndVariables =
    GroupAndVariables(VariableGroup(modelId, groupId, asConstraints = true, asOutputs = false,
      VarValueSet.StringList(apples)), Seq(VariableId("apple one"), VariableId("apple two")))
}
