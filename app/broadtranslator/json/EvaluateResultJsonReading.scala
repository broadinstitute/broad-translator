package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import TranslatorIdsJsonReading.{ variableIdReads, groupIdReads, modelIdReads }
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

object EvaluateResultJsonReading {

  implicit val variableValueReads: Reads[VariableValue] = new Reads[VariableValue] {
    override def reads(json: JsValue): JsResult[VariableValue] = json match {
      case JsBoolean(boolean) => JsSuccess(VariableValue.BooleanValue(boolean))
      case JsNumber(number)   => JsSuccess(VariableValue.NumberValue(number.toDouble))
      case JsString(string)   => JsSuccess(VariableValue.StringValue(string))
      case _                  => JsError(s"Expected Boolean, Number or String, but got $json.")
    }
  }

  implicit val valueProbabilityReads: Reads[ValueProbability] =
    ((JsPath \ "variableValue").read[VariableValue] and
      (JsPath \ "posteriorProbability").read[Double])(ValueProbability)

  implicit val modelVariableReads: Reads[ModelVariable] =
    ((JsPath \ "variableID").read[VariableId] and
      (JsPath \ "posteriorDistribution").read[Seq[ValueProbability]])(ModelVariable(_, _))

  implicit val variableGroupReads: Reads[VariableGroup] =
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "modelVariable").read[Seq[ModelVariable]])(VariableGroup)

  implicit val evaluateResultReads: Reads[EvaluateModelResult] =
    (JsPath \ "posteriorProbabilities").read[Seq[VariableGroup]].map[EvaluateModelResult](EvaluateModelResult)

}