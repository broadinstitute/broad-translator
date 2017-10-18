package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import broadtranslator.json.TranslatorIdsJsonReading.{ variableIdReads, groupIdReads, modelIdReads }
import broadtranslator.json.EvaluateRequestJsonReading.{ variableValueReads, gaussianDistributionReads, poissonDistributionReads, empiricalDistributionReads }
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import java.io.File
import java.io.FileInputStream

object EvaluateResultJsonReading {

  implicit val valueProbabilityReads: Reads[ValueProbability] =
    ((JsPath \ "variableValue").read[VariableValue] and
      (JsPath \ "posteriorProbability").read[Double])(ValueProbability)

  implicit val discreteDistributionReads: Reads[ProbabilityDistribution.Discrete[_]] =
    JsPath.read[Seq[ValueProbability]].map(ProbabilityDistribution.Discrete(_))

  implicit val rawDistributionReads: Reads[ProbabilityDistribution.Raw] =
    JsPath.read[Seq[Double]].map(ProbabilityDistribution.Raw)

  implicit val probabilityDistributionReads: Reads[ProbabilityDistribution] =
    ((JsPath \ "discreteDistribution").readNullable[ProbabilityDistribution.Discrete[_]] and
      (JsPath \ "GaussianDistribution").readNullable[ProbabilityDistribution.Gaussian] and
      (JsPath \ "PoissonDistribution").readNullable[ProbabilityDistribution.Poisson] and
      (JsPath \ "empiricalDistribution").readNullable[ProbabilityDistribution.Empirical] and
      (JsPath \ "rawDistribution").readNullable[ProbabilityDistribution.Raw])(ProbabilityDistribution(_, _, _, _, _))

  implicit val modelVariableReads: Reads[ModelVariable] =
    ((JsPath \ "variableID").read[VariableId] and
      (JsPath \ "posteriorDistribution").read[ProbabilityDistribution])(ModelVariable)

  implicit val variableGroupReads: Reads[VariableGroup] =
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "modelVariable").read[Seq[ModelVariable]])(VariableGroup)

  implicit val evaluateResultReads: Reads[EvaluateModelResult] =
    (JsPath \ "posteriorProbability").read[Seq[VariableGroup]].map[EvaluateModelResult](EvaluateModelResult)


  def readEvaluateResult(modelId: ModelId, file: File): EvaluateModelResult = {
    val input = new FileInputStream(file)
    val json = Json.parse(input)
    input.close()
    json.validate[EvaluateModelResult] match {
      case JsSuccess(result, _) => result
      case JsError(error) => throw new java.io.IOException("Faile to parse evaluate-model response JSON\n"+error)
    }
  }

}