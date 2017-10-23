package broadtranslator.json


import broadtranslator.engine.api.evaluate._
import broadtranslator.json.TranslatorIdsJsonWriting.entityIdWrites
import play.api.libs.json.{ JsNumber, JsString, JsArray, JsValue, Json, Writes }
import java.io.File
import java.io.PrintWriter
import java.io.FileWriter

object EvaluateRequestJsonWriting {

  private[json] def valueToJson(value: Any): JsValue = value match {
    case string: String => JsString(string)
    case double: Double => JsNumber(double)
    case true           => JsNumber(1)
    case false          => JsNumber(0)
    case other          => JsString(other.toString)
  }

  implicit val discreteDistributionWrites: Writes[(Any, Double)] = new Writes[(Any, Double)] {
    override def writes(probability: (Any, Double)): JsValue = Json.obj(
      "variableValue" -> valueToJson(probability._1),
      "priorProbability" -> probability._2)
  }

  implicit val gaussianDistributionWrites: Writes[ProbabilityDistribution.Gaussian] = new Writes[ProbabilityDistribution.Gaussian] {
    override def writes(distribution: ProbabilityDistribution.Gaussian): JsValue = Json.obj(
      "distributionMean" -> distribution.mean,
      "distributionStDev" -> distribution.sigma)
  }

  implicit val poissonDistributionWrites: Writes[ProbabilityDistribution.Poisson] = new Writes[ProbabilityDistribution.Poisson] {
    override def writes(distribution: ProbabilityDistribution.Poisson): JsValue = Json.obj(
      "lambdaParameter" -> distribution.lambda)
  }

  implicit val empiricalDistributionWrites: Writes[ProbabilityDistribution.Empirical] = new Writes[ProbabilityDistribution.Empirical] {
    override def writes(distribution: ProbabilityDistribution.Empirical): JsValue = Json.obj(
      "distributionMean" -> distribution.mean,
      "distributionStDev" -> distribution.sigma,
      "distributionPercentile" -> distribution.percentile)
  }

  implicit val probabilityDistributionWrites: Writes[ProbabilityDistribution] = new Writes[ProbabilityDistribution] {
    override def writes(distribution: ProbabilityDistribution): JsValue = distribution match {
      case ProbabilityDistribution.Scalar(value) => Json.obj("scalarValue" -> Json.obj("variableValue" -> valueToJson(value)))
      case ProbabilityDistribution.Discrete(probabilities) => Json.obj("discreteDistribution" -> probabilities)
      case gaussian: ProbabilityDistribution.Gaussian => Json.obj("GaussianDistribution" -> gaussianDistributionWrites.writes(gaussian))
      case poisson: ProbabilityDistribution.Poisson => Json.obj("PoissonDistribution" -> poissonDistributionWrites.writes(poisson))
      case empirical: ProbabilityDistribution.Empirical => Json.obj("empiricalDistribution" -> empiricalDistributionWrites.writes(empirical))
      case _ => Json.obj()
    }
  }

  implicit val modelVariableWrites: Writes[ModelVariable] = new Writes[ModelVariable] {
    override def writes(modelVariable: ModelVariable): JsValue = Json.obj(
      "variableID" -> modelVariable.variableId,
      "priorDistribution" -> modelVariable.probabilityDistribution)
  }

  implicit val variableGroupWrites: Writes[VariableGroup] = new Writes[VariableGroup] {
    override def writes(variableGroup: VariableGroup): JsValue = Json.obj(
      "variableGroupID" -> variableGroup.groupId,
      "modelVariable" -> variableGroup.modelVariable)
  }

  implicit val outputGroupWrites: Writes[OutputGroup] = new Writes[OutputGroup] {
    override def writes(group: OutputGroup): JsValue = Json.obj(
      "variableGroupID" -> group.groupId,
      "variableID" -> group.variableId,
      "rawOutput" -> group.rawOutput)
  }

  implicit val evaluateRequestWrites: Writes[EvaluateModelRequest] = new Writes[EvaluateModelRequest] {
    override def writes(request: EvaluateModelRequest): JsValue = Json.obj(
      "modelID" -> request.modelId,
      "modelInput" -> request.modelInput,
      "modelOutput" -> request.modelOutput)
  }

  def saveEvaluateRequest(request: EvaluateModelRequest, file: File): Unit = {
    val out = new PrintWriter(new FileWriter(file))
    out.println(Json.prettyPrint(Json.toJson(request)))
    out.close()
  }
}