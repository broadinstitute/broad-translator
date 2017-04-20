package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class VariableGroup(modelId: ModelId, id: VariableGroupId, asConstraints: Boolean, asOutputs: Boolean,
                         valueSet: VarValueSet) {

}

