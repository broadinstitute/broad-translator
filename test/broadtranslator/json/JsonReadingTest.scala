package broadtranslator.json

import play.api.libs.json._
import broadtranslator.engine.api.signature._
import broadtranslator.json.SignatureJsonReading._

import org.scalatest.Matchers
import org.scalatest.FlatSpec

class JsonReadingTest extends FlatSpec with Matchers {

  "Signature JSON Reader" should "read GroupSignature" in {

    val jsonStr = """
       {
         "variableGroupID" : "GeneExpression",
         "asInput" : true,
         "asOutput" : true,
         "groupDistribution" : "discrete",
         "groupType" : "String",
         "groupValue" : [ "UP", "NC", "DN" ],
         "modelVariable" : [ 
           {
             "variableID" : "ENSG00000000003",
             "authorityURI" : "http://mygene.info/v3/gene/ENSG00000000003"
           }, {
             "variableID" : "ENSG00000000457",
             "authorityURI" : "http://mygene.info/v3/gene/ENSG00000000457"
           }
         ]
       }
       """

    val json = Json.parse(jsonStr)
    val obj = json.validate[GroupSignature]
    obj shouldBe a[JsSuccess[_]]
    info("OK")
  }

  it should "read signature json for all models" in {
    val folder = new java.io.File("models")
    for (modelId <- folder.list()) {
      info(modelId)
      val json = Json.parse(new java.io.FileInputStream("models/" + modelId + "/modelSignature.json"))
      json.validate[ModelSignatureResult] shouldBe a[JsSuccess[_]]
      info("OK")
    }
  }
}