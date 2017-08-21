package broadtranslator.json

import broadtranslator.engine.api.id.EntityId
import play.api.libs.json.{ JsBoolean, JsNumber, JsObject, JsString, JsArray, JsValue, Json, Writes }

object TranslatorIdsJsonWriting {

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }


}