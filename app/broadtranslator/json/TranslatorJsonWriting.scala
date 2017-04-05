package broadtranslator.json

import broadtranslator.engine.api.VarValueSet.{NumberInterval, NumberList, StringList, ValueType}
import broadtranslator.engine.api.{EntityId, ModelListResult, ModelSignatureResult, VarValueSet, VariableGroup, VariablesByGroupResult}
import play.api.libs.json.{JsObject, JsString, Json, Writes}

/**
  * broadtranslator
  * Created by oliverr on 4/5/2017.
  */
object TranslatorJsonWriting {

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsObject = Json.obj(
      "models" -> result.modelIds
    )
  }

  implicit val valueTypeWrites: Writes[ValueType] = new Writes[ValueType] {
    override def writes(valueType: ValueType): JsString = JsString(valueType.name)
  }

  implicit val valueSetWrites: Writes[VarValueSet] = new Writes[VarValueSet] {
    override def writes(valueSet: VarValueSet): JsObject = {
      val jsonType = Json.obj("type" -> valueSet.valueType)
      val jsonValues = valueSet match {
        case StringList(values) => Json.obj("values" -> values)
        case NumberList(values) => Json.obj("values" -> values)
        case NumberInterval(min, max) => Json.obj("min" -> min, "max" -> max)
        case _ => Json.obj()
      }
      jsonType ++ jsonValues
    }
  }

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(group: VariableGroup): JsObject = {
      val jsonCore = Json.obj(
        "id" -> group.id,
        "asConstraints" -> group.asConstraints,
        "asOutputs" -> group.asOutputs
      )
      val jsonValues = valueSetWrites.writes(group.valueSet).asInstanceOf[JsObject]
      jsonCore ++ jsonValues
    }
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsObject = Json.obj(
      "model" -> result.modelId,
      "groups" -> result.groups.values
    )
  }

  implicit val variablesByGroupResultWrites: Writes[VariablesByGroupResult] = new Writes[VariablesByGroupResult] {
    override def writes(result: VariablesByGroupResult): JsObject =
      variableGroupWrites.writes(result.group).asInstanceOf[JsObject] ++ Json.obj("variables" -> result.variableIds)
  }

}
