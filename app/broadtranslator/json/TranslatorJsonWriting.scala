package broadtranslator.json

import broadtranslator.engine.api._
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsArray, JsValue, Json, Writes}
import util.MatchNumber

/**
  * broadtranslator
  * Created by oliverr on 4/5/2017.
  */
object TranslatorJsonWriting {
  
  val EMPTY = Json.obj()
  
  /*
   * We encode None of an option as empty object and use this filter call to remove them from a sequence
   */
  private def filterOptions(fields: (String, Json.JsValueWrapper)*): Seq[(String, Json.JsValueWrapper)] = {
    fields.filter {case (key, value) => value != Json.toJsFieldJsValueWrapper(EMPTY) }
  }

  implicit val entityIdWrites: Writes[EntityId] = new Writes[EntityId] {
    override def writes(entityId: EntityId): JsString = JsString(entityId.string)
  }

  implicit val modelListResultWrites: Writes[ModelListResult] = new Writes[ModelListResult] {
    override def writes(result: ModelListResult): JsObject = Json.obj(
      "modelID" -> result.modelIds
    )
  }

  implicit val valueTypeWrites: Writes[Option[ValueType]] = new Writes[Option[ValueType]] {
    override def writes(valueType: Option[ValueType]): JsValue = valueType match {
      case None => EMPTY
      case Some(valueType) => JsString(valueType.name)
    }
  }

  implicit val valueListWrites: Writes[Option[ValueList]] = new Writes[Option[ValueList]] {
    override def writes(valueList: Option[ValueList]): JsValue = valueList match {
      case None                => EMPTY
      case Some(ValueList.StringList(values)) => JsArray(values.map(JsString(_)))
      case Some(ValueList.NumberList(values)) => JsArray(values.map(JsNumber(_)))
      case Some(unmatched)             => println("value list not matched: "+unmatched); EMPTY
    }
  }
  

  implicit val variableGroupWrites: Writes[GroupSignature] = new Writes[GroupSignature] {
    override def writes(group: GroupSignature): JsObject = 
      Json.obj(filterOptions(
        "variableGroupID" -> group.groupId,
        "authorityURL" -> group.authorityURL,
        "asInput" -> group.asConstraints,
        "asOutput" -> group.asOutputs,
        "groupType" -> group.valueType,
        "groupValue" -> group.values
      ): _*)
  }

  implicit val modelSignatureResultWrites: Writes[ModelSignatureResult] = new Writes[ModelSignatureResult] {
    override def writes(result: ModelSignatureResult): JsObject = Json.obj(
      "modelID" -> result.modelId,
      "variableGroup" -> result.groups.keys.map(_.string).toSeq.sorted.map(str => result.groups(VariableGroupId(str)))
    )
  }

  implicit val variablesByGroupResultWrites: Writes[VariablesByGroupResult] = new Writes[VariablesByGroupResult] {
    override def writes(result: VariablesByGroupResult): JsObject =
      Json.obj(
        "modelID" -> result.modelId,
        "variableGroupID" -> result.groupId,
        "modelVariable" -> result.variables)
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
        "variableID" -> result.variableId,
        "authorityURI" -> result.uri,
        "variableType" -> result.valueType,
        "variableValue" -> result.values
      ): _*)
  }
}
  implicit val variableWithProbabilitiesWrites: Writes[VariableWithProbabilities] =
    new Writes[VariableWithProbabilities] {
      override def writes(varWithProbs: VariableWithProbabilities): JsValue = {
        val varJson = Json.obj("variableID" -> varWithProbs.variableId)
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
                "variableValue" -> valueJson,
                "posteriorProbability" -> probability
              )
            })
            Json.obj("posteriorDistribution" -> probsJson)
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
      "variableGroupID" -> groupWithProbs.groupId,
      "modelVariable" -> groupWithProbs.varsWithProbs
    )
  }

  implicit val evaluateResultWrites: Writes[EvaluateResult] = new Writes[EvaluateResult] {
    override def writes(result: EvaluateResult): JsValue = Json.obj(
      "posteriorProbabilities" -> result.groupsWithProbs
    )
  }

}
