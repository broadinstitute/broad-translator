package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.signature._
import play.api.libs.json.{ JsBoolean, JsNumber, JsObject, JsString, JsArray, JsValue, Json, Writes }
import TranslatorIdsJsonWriting.entityIdWrites

object SignatureJsonWriting {

  val EMPTY = Json.obj()

  /*
   * We encode None of an option as empty object and use this filter call to remove them from a sequence
   */
  private def filterOptions(fields: (String, Json.JsValueWrapper)*): Seq[(String, Json.JsValueWrapper)] = {
    fields.filter { case (key, value) => value != Json.toJsFieldJsValueWrapper(EMPTY) }
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsObject = Json.obj(
      "modelID" -> result.modelId)
  }

  implicit val valueTypeWrites: Writes[Option[ValueType]] = new Writes[Option[ValueType]] {
    override def writes(valueType: Option[ValueType]): JsValue = valueType match {
      case None            => EMPTY
      case Some(valueType) => JsString(valueType.name)
    }
  }

  implicit val probabilityDistributionNameWrites: Writes[Option[ProbabilityDistributionName]] = new Writes[Option[ProbabilityDistributionName]] {
    override def writes(name: Option[ProbabilityDistributionName]): JsValue = name match {
      case None                              => EMPTY
      case Some(probabilityDistributionName) => JsString(probabilityDistributionName.name)
    }
  }

  implicit val valueListWrites: Writes[Option[ValueList]] = new Writes[Option[ValueList]] {
    override def writes(valueList: Option[ValueList]): JsValue = valueList match {
      case None                               => EMPTY
      case Some(ValueList.StringList(values)) => JsArray(values.map(JsString(_)))
      case Some(ValueList.NumberList(values)) => JsArray(values.map(JsNumber(_)))
      case Some(unmatched)                    => println("value list not matched: " + unmatched); EMPTY
    }
  }

  implicit val variableGroupWrites: Writes[GroupSignature] = new Writes[GroupSignature] {
    override def writes(group: GroupSignature): JsObject =
      Json.obj(filterOptions(
        "variableGroupID" -> group.groupId,
        "authorityURL" -> group.authorityURL,
        "asInput" -> group.asInput,
        "asOutput" -> group.asOutput,
        "groupDistribution" -> group.groupDistribution,
        "groupType" -> group.valueType,
        "groupValue" -> group.groupValue,
        "modelVariable" -> group.modelVariable): _*)
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsObject = Json.obj(
      "modelID" -> result.modelId,
      "variableGroup" -> result.variableGroup.keys.map(_.string).toSeq.sorted.map(str => result.variableGroup(VariableGroupId(str))))
  }

  implicit val variablesByGroupResultWrites: Writes[GroupSignatureResult] = new Writes[GroupSignatureResult] {
    override def writes(result: GroupSignatureResult): JsObject =
      Json.obj(
        "modelID" -> result.modelId,
        "variableGroupID" -> result.groupId,
        "modelVariable" -> result.modelVariable)
  }

  implicit val uriWrites: Writes[Option[VariableURI]] = new Writes[Option[VariableURI]] {
    override def writes(result: Option[VariableURI]): JsValue = result match {
      case None      => EMPTY
      case Some(uri) => JsString(uri.uri)
    }
  }

  implicit val variableSignatureWrites: Writes[ModelVariableSignature] = new Writes[ModelVariableSignature] {
    override def writes(result: ModelVariableSignature): JsObject = {
      Json.obj(filterOptions(
        "variableID" -> result.variableId,
        "authorityURI" -> result.authorityURI,
        "variableType" -> result.variableType,
        "variableValue" -> result.variableValue): _*)
    }
  }
}