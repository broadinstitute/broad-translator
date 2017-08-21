package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.signature._
import broadtranslator.engine.api.signature.ValueList.{ NumberList, StringList }
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import TranslatorIdsJsonReading.{ variableIdReads, groupIdReads, modelIdReads }

object SignatureJsonReading {

  implicit val probabilityDistributionNameReads: Reads[ProbabilityDistributionName] = implicitly[Reads[String]].map(ProbabilityDistributionName.apply)
  implicit val variableUriReads: Reads[VariableURI] = implicitly[Reads[String]].map(VariableURI.apply)

  implicit val valueTypeReads: Reads[ValueType] = new Reads[ValueType] {
    override def reads(json: JsValue): JsResult[ValueType] = json match {
      case JsString(string) => string match {
        case "Boolean" => JsSuccess(BooleanType)
        case "Number"  => JsSuccess(NumberType)
        case "String"  => JsSuccess(StringType)
        case _         => JsError(s"Expected Boolean, Number or String, but got $json.")
      }
      case _ => JsError(s"Expected Boolean, Number or String, but got $json.")
    }
  }

  private def all(values: Seq[Boolean]) = (values :\ true) { _ && _ }

  private def jsonToNumber(json: JsValue): Double = (json: @unchecked) match {
    case JsNumber(value) => value.toDouble
  }

  private def jsonToString(json: JsValue): String = (json: @unchecked) match {
    case JsString(value) => value
  }

  implicit val valueListReads: Reads[ValueList] = new Reads[ValueList] {
    override def reads(json: JsValue): JsResult[ValueList] = json match {
      case JsArray(members) if all(members.map(_.isInstanceOf[JsNumber])) => JsSuccess(NumberList(members.map(jsonToNumber)))
      case JsArray(members) if all(members.map(_.isInstanceOf[JsString])) => JsSuccess(StringList(members.map(jsonToString)))
      case _ => JsError(s"Expected array of Boolean, Number or String, but got $json.")
    }
  }

  implicit val modelVariableReads: Reads[ModelVariableSignature] = {
    ((JsPath \ "variableID").read[VariableId] and
      (JsPath \ "authorityURI").readNullable[VariableURI] and
      (JsPath \ "variableDistribution").readNullable[ProbabilityDistributionName] and
      (JsPath \ "variableType").readNullable[ValueType] and
      (JsPath \ "variableValue").readNullable[ValueList])(ModelVariableSignature)
  }

  implicit val groupSignatureReads: Reads[GroupSignature] = {
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "authorityURL").readNullable[VariableURI] and
      (JsPath \ "asInput").read[Boolean] and
      (JsPath \ "asOutput").read[Boolean] and
      (JsPath \ "groupDistribution").readNullable[ProbabilityDistributionName] and
      (JsPath \ "valueType").readNullable[ValueType] and
      (JsPath \ "groupValue").readNullable[ValueList] and
      (JsPath \ "modelVariable").read[Seq[ModelVariableSignature]])(GroupSignature)
  }

  implicit val modelSignatureReads: Reads[ModelSignatureResult] = {
    ((JsPath \ "modelID").read[ModelId] and
      (JsPath \ "variableGroup").read[Seq[GroupSignature]])(ModelSignatureResult(_, _))
  }

}

