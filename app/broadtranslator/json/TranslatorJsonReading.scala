package broadtranslator.json

import broadtranslator.engine.api._
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

/**
  * broadtranslator
  * Created by oliverr on 4/4/2017.
  */
object TranslatorJsonReading {

  implicit val modelIdReads: Reads[ModelId] = implicitly[Reads[String]].map(ModelId)
  implicit val variableIdReads: Reads[VariableId] = implicitly[Reads[String]].map(VariableId)
  implicit val groupIdReads: Reads[VariableGroupId] = implicitly[Reads[String]].map(VariableGroupId)

  implicit val outputGroupReads: Reads[OutputGroup] = (
    (JsPath \ "group").read[VariableGroupId] and (JsPath \ "variables").read[Seq[VariableId]])(OutputGroup)

  implicit val constraintReads: Reads[VariableConstraint] = new Reads[VariableConstraint] {
    override def reads(json: JsValue): JsResult[VariableConstraint] = json match {
      case JsBoolean(boolean) => JsSuccess(VariableConstraint.Equals(boolean))
      case JsNumber(number) => JsSuccess(VariableConstraint.Equals(number))
      case JsString(string) => JsSuccess(VariableConstraint.Equals(string))
      case _ => JsError(s"Expected Boolean, Number or String, but got $json.")
    }
  }

  implicit val valueProbabilityReads: Reads[ValueProbability] = ??? // TODO

  implicit val probabilityDistributionReads: Reads[ProbabilityDistribution] = ??? // TODO

  implicit val variableWithProbabilitiesReads: Reads[VariableWithProbabilities] =
    ((JsPath \"variableId").read[VariableId] and
      (JsPath \"probabilities").read[ProbabilityDistribution])(VariableWithProbabilities)

  implicit val groupWithProbabilitiesReads: Reads[GroupWithProbabilities] =
    ((JsPath \ "variableGroupId").read[VariableGroupId] and
      (JsPath \ "probabilities").read[Seq[VariableWithProbabilities]])(GroupWithProbabilities)

  implicit val variableAndConstraintReads: Reads[VariableAndConstraint] =
    ((JsPath \ "variable").read[VariableId] and
      (JsPath \ "value").read[VariableConstraint])(VariableAndConstraint)

  implicit val constraintGroupReads: Reads[ConstraintGroup] = (
    (JsPath \ "group").read[VariableGroupId] and
      (JsPath \ "constraints").read[Seq[VariableAndConstraint]])(ConstraintGroup)

  implicit val evaluateRequestReads: Reads[EvaluateRequest] =
    ((JsPath \ "model").read[ModelId] and
      (JsPath \ "outputs").read[Seq[OutputGroup]] and
      (JsPath \ "priors").read[Seq[GroupWithProbabilities]]) (EvaluateRequest)

}
