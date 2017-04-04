package broadtranslator.engine.mock

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api.{ModelId, VarValueSet, VariableGroup, VariableGroupId}

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  override def getAvailableModelIds: Seq[ModelId] = Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId)

  override def getModelSignature(modelId: ModelId): Seq[VariableGroup] = Seq(
    VariableGroup(modelId, VariableGroupId("apples"), asConstraints = true, asOutputs = false,
      VarValueSet.StringList(Seq("Gala", "Granny Smith"))),
    VariableGroup(modelId, VariableGroupId("oranges"), asConstraints = false, asOutputs = true,
      VarValueSet.StringList(Seq("Navel", "Clementine")))
  )
}
