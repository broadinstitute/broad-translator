package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class VariableGroup(modelId: ModelId, id: VariableGroupId, authorityURL: Option[VariableURI],
    asConstraints: Boolean, asOutputs: Boolean, valueSet: VarValueSet) {

}

