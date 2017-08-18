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
    groupDistribution: Option[ProbabilityDistributionName],
    valueType: Option[ValueType],
    groupValue: Option[ValueList], 
    modelVariable: Seq[ModelVariableSignature]) {

}

