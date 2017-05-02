package broadtranslator.json

import broadtranslator.engine.api._
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, Json, Writes}
import util.MatchNumber

/**
  * broadtranslator
  * Created by oliverr on 4/5/2017.
  */
object TranslatorJsonWriting {
  
  val EMPTY = Json.obj()
  
  private def filterOptions(fields: (String, Json.JsValueWrapper)*): Seq[(String, Json.JsValueWrapper)] = {
    fields.filter {case (key, value) => value != Json.toJsFieldJsValueWrapper(EMPTY) }
  }

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsObject = Json.obj(
      "models" -> result.modelIds
    )
  }

  implicit val valueTypeWrites: Writes[Option[ValueType]] = new Writes[Option[ValueType]] {
    override def writes(valueType: Option[ValueType]): JsValue = valueType match {
      case None => EMPTY
      case Some(valueType) => JsString(valueType.name)
    }
  }

  implicit val valueListWrites: Writes[Option[ValueList]] = new Writes[Option[ValueList]] {
    override def writes(valueList: Option[ValueList]): JsObject = valueList match {
      case None                => EMPTY
      case Some(ValueList.StringList(values)) => Json.obj("values" -> values)
      case Some(ValueList.NumberList(values)) => Json.obj("values" -> values)
      case Some(unmatched)             => println("value list not matched: "+unmatched); EMPTY
    }
  }
  

  implicit val variableGroupWrites: Writes[GroupSignature] = new Writes[GroupSignature] {
    override def writes(group: GroupSignature): JsObject = 
      Json.obj(filterOptions(
        "id" -> group.groupId,
        "authorityURL" -> group.authorityURL,
        "asConstraints" -> group.asConstraints,
        "asOutputs" -> group.asOutputs,
        "type" -> group.valueType,
        "values" -> group.values
      ): _*)
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsObject = Json.obj(
      "model" -> result.modelId,
      "groups" -> result.groups.keys.map(_.string).toSeq.sorted.map(str => result.groups(VariableGroupId(str)))
    )
  }

  implicit val variablesByGroupResultWrites: Writes[VariablesByGroupResult] = new Writes[VariablesByGroupResult] {
    override def writes(result: VariablesByGroupResult): JsObject =
      Json.obj(
        "modelId" -> result.modelId,
        "variableGroupId" -> result.groupId,
        "variables" -> result.variables)
  }
  
  implicit val uriWrites: Writes[Option[VariableURI]] = new Writes[Option[VariableURI]] {
    override def writes(result: Option[VariableURI]): JsValue = result match {
      case None => EMPTY
      case Some(uri) => JsString(uri.uri)
    }
  }
  
  implicit val variableSignatureWrites: Writes[VariableSignature] = new Writes[VariableSignature] {
    override def writes(result: VariableSignature): JsObject ={
      Json.obj(filterOptions(
        "variableId" -> result.variableId,
        "uri" -> result.uri,
        "type" -> result.valueType,
        "values" -> result.values
      ): _*)
  }
}
  implicit val variableWithProbabilitiesWrites: Writes[VariableWithProbabilities] =
    new Writes[VariableWithProbabilities] {
      override def writes(varWithProbs: VariableWithProbabilities): JsValue = {
        val varJson = Json.obj("variable" -> varWithProbs.variableId)
        val probJson = varWithProbs.probabilityDistribution match {
          case ProbabilityDistribution.Discrete(probabilities) =>
            val probsJson = probabilities.map({ case (value, probability) =>
              val valueJson = value match {
                case string: String => JsString(string)
                case double: Double => JsNumber(double)
                case true => JsNumber(1)
                case false => JsNumber(0)
                case other => JsString(other.toString)
              }
              Json.obj(
                "value" -> valueJson,
                "probability" -> probability
              )
            })
            Json.obj("probabilities" -> probsJson)
          case ProbabilityDistribution.Gaussian(mean, sigma) => Json.obj(
            "mean" -> mean,
            "sigma" -> sigma
          )
        }
        varJson ++ probJson
      }
    }

  implicit val groupWithProbabilitiesWrites: Writes[GroupWithProbabilities] = new Writes[GroupWithProbabilities] {
    override def writes(groupWithProbs: GroupWithProbabilities): JsValue = Json.obj(
      "group" -> groupWithProbs.groupId,
      "probabilities" -> groupWithProbs.varsWithProbs
    )
  }

  implicit val evaluateResultWrites: Writes[EvaluateResult] = new Writes[EvaluateResult] {
    override def writes(result: EvaluateResult): JsValue = Json.obj(
      "probabilities" -> result.groupsWithProbs
    )
  }

}
