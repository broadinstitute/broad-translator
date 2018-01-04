package util

import play.api.libs.json._
import broadtranslator.engine.api.signature._
import broadtranslator.json.SignatureJsonReading._
import broadtranslator.json.SignatureJsonWriting._
import broadtranslator.engine.api.id._
import scala.io.Source
import java.io.PrintWriter

object GenerateVariantToGeneModelSignature {

  def main(args: Array[String]): Unit = {
    val sig = ModelSignatureResult(ModelId("VariantToGene"), Seq(variantsSignature, geneSignature))
    val jsonStr = Json.prettyPrint(Json.toJson(sig))
    val out = new PrintWriter("models/VariantToGene/modelSignature.json")
    out.print(jsonStr)
    out.close()
  }

  // Variant signature
  
  def variantsSignature: GroupSignature = (loadVariantsSignature: @unchecked) match {
    case Some(GroupSignature(id, url, in, out, dist, types, vals, vars)) => GroupSignature(id, url, true, false, dist, types, vals, vars)
  }

  def loadVariantsSignature: Option[GroupSignature] = {
    val json = Json.parse(new java.io.FileInputStream("models/PhenotypeToVariant/modelSignature.json"))
    (json.validate[ModelSignatureResult]) match {
      case JsSuccess(signature, _) => signature.variableGroup.get(VariableGroupId("Variant"))
      case JsError(_)              => None
    }
  }

  // Gene signature
  
  def geneSignature = GroupSignature(
    VariableGroupId("Genes"),
    None,
    false: Boolean,
    true: Boolean,
    Some(ProbabilityDistributionName.discrete),
    Some(NumberType),
    Some(ValueList.NumberList(Seq(1))),
    loadGenes)

  def loadGenes: Seq[ModelVariableSignature] = {
    Source.fromFile("models/VariantToGene/GeneIds.txt").getLines().map(geneToVariableSignature).toSeq
  }

  def geneToVariableSignature(line: String): ModelVariableSignature = {
    ModelVariableSignature(VariableId(line.split("\t")(0)), Some(VariableURI("http://mygene.info/v3/gene/"+line.split("\t")(1))), None, None, None)
  }

}