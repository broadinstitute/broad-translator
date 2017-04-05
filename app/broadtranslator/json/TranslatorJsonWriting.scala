package broadtranslator.json

import broadtranslator.engine.api.{EntityId, ModelListResult, ModelSignatureResult, VariableGroup}
import play.api.libs.json.{JsString, JsValue, Json, Writes}

/**
  * broadtranslator
  * Created by oliverr on 4/5/2017.
  */
object TranslatorJsonWriting {

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsValue = Json.obj(
      "models" -> result.modelIds
    )
  }

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(group: VariableGroup): JsValue = Json.obj(
      ???
    )
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsValue = Json.obj(
      "model" -> result.modelId,
      "groups" -> result.groups.values
    )
  }

}
