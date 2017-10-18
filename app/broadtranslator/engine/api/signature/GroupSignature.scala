package broadtranslator.engine.api.signature

import broadtranslator.engine.api.id._

/**
 * broadtranslator
 * Created by oliverr on 4/4/2017.
 */
case class GroupSignature(

    groupId: VariableGroupId,
    authorityURL: Option[VariableURI],
    asInput: Boolean,
    asOutput: Boolean,
    groupDistribution: Option[ProbabilityDistributionName],
    valueType: Option[ValueType],
    groupValue: Option[ValueList],
    modelVariable: Seq[ModelVariableSignature]) {

}

