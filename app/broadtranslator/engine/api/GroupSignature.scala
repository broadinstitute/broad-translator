package broadtranslator.engine.api

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
case class GroupSignature(
    modelId: ModelId, 
    groupId: VariableGroupId, 
    authorityURL: Option[VariableURI],
    asInput: Boolean, 
    asOutput: Boolean, 
    valueType: Option[ValueType],
    groupValue: Option[ValueList]) {

}

