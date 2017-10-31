package broadtranslator.json

import play.api.libs.json._
import broadtranslator.engine.api.signature._
import broadtranslator.engine.api.evaluate.{ EvaluateModelRequest, EvaluateModelResult }
import broadtranslator.json.SignatureJsonReading._
import broadtranslator.json.EvaluateResultJsonReading.evaluateResultReads
import broadtranslator.json.EvaluateRequestJsonReading.evaluateRequestReads
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
    
    val jsonStrB = """

        {
            "variableGroupID": "CompoundTreatment",
            "asInput": true,
            "asOutput": true,
            "groupDistribution": "discrete",
            "groupType": "boolean",
            "groupValue": [
                true,
                false
            ],
            "modelVariable": [
                {
                    "variableID": "BRD_A81541225_001_02_7"
                }
            ]
         }
       """

    val json = Json.parse(jsonStr)
    val obj = json.validate[GroupSignature]
    obj shouldBe a[JsSuccess[_]]
    info("OK A")

    val jsonB = Json.parse(jsonStrB)
    val objB = jsonB.validate[GroupSignature]
    objB shouldBe a[JsSuccess[_]]
    info("OK B")
}

  it should "read signature json for all models" in {
    val folder = new java.io.File("models")
    for (modelId <- folder.list() if modelId.charAt(0) != '.') {
      info(modelId)
      val json = Json.parse(new java.io.FileInputStream("models/" + modelId + "/modelSignature.json"))
      json.validate[ModelSignatureResult] shouldBe a[JsSuccess[_]]
      info("OK")
    }
  }

  "Evaluate result JSON Reader" should
    "read mock result" in {

      val jsonStr = """
{
  "posteriorProbability": [
    {
      "variableGroupID": "GeneExpression",
      "modelVariable": [
        {
          "variableID": "ENSG00000070495",
          "posteriorDistribution": {
            "discreteDistribution": [
              {
                "variableValue": "1",
                "posteriorProbability": 0.037
              },
              {
                "variableValue": "2",
                "posteriorProbability": 0.95
              },
              {
                "variableValue": "3",
                "posteriorProbability": 0.013
              }
            ]
          }
        },
        {
          "variableID": "ENSG00000070495",
          "posteriorDistribution": {
            "GaussianDistribution": { "distributionMean": 0.25, "distributionStDev": 0.1}
          }
        },
        {
          "variableID": "ENSG00000070495",
          "posteriorDistribution": {
            "PoissonDistribution":
              { "lambdaParameter": 1.0}            
          }
        },
        {
          "variableID": "ENSG00000070495",
          "posteriorDistribution": {
            "empiricalDistribution":  { "distributionMean": 0.25, "distributionStDev": 0.1,
            "distributionPercentile": [1,2,3,4,5,6,7,8,9,10]}            
          }
        },
        {
          "variableID": "ENSG00000070495",
          "posteriorDistribution": {
            "rawDistribution": [1,2,3,4,5,6,7,8,9,10]
          }
        }
      ]
    },
    {
      "variableGroupID": "GeneKnockdown",
      "modelVariable": [
        {
          "variableID": "gene_knockdown",
          "posteriorDistribution": {
            "discreteDistribution": [
              {
                "variableValue": "10",
                "posteriorProbability": 0.001
              },
              {
                "variableValue": "14",
                "posteriorProbability": 0.001
              },
              {
                "variableValue": "15",
                "posteriorProbability": 0.001
              },
              {
                "variableValue": "19",
                "posteriorProbability": 0.002
              },
              {
                "variableValue": "22",
                "posteriorProbability": 0.001
              },
              {
                "variableValue": "27",
                "posteriorProbability": 0.001
              },
              {
                "variableValue": "3551",
                "posteriorProbability": 0.001
              }
            ]
          }
        }
      ]
    }
  ]
}
    """

      val json = Json.parse(jsonStr)
      val obj = json.validate[EvaluateModelResult]
      obj shouldBe a[JsSuccess[_]]
      println(obj)
      info("OK")
    }

  it should
    "read json file" in {

      val filename = "test/broadtranslator/json/api_test_query1_result.json"
      println(filename)
      val json = Json.parse(new java.io.FileInputStream(filename))
      val obj = json.validate[EvaluateModelResult] 
      obj shouldBe a[JsSuccess[_]]
      println(obj)
      info("OK file")

    }

  "Evaluate request JSON reader" should "read request" in {
    val jsonStr = """
      {
   "modelID": "GeneExpressionAndGeneKnockdown",
   "modelInput": [
       {
       "variableGroupID": "GeneExpression",
       "modelVariable": [
         {
           "variableID": "ENSG00000163913",
           "priorDistribution": {
            "scalarValue": { "variableValue": 0.1}
           }
         },
         {
           "variableID": "ENSG00000163914",
           "priorDistribution": {
            "scalarValue": { "variableValue": "M"}
           }
         },
         {
           "variableID": "ENSG00000163902",
           "priorDistribution": {
            "discreteDistribution": [
              { "variableValue": "UP", "priorProbability": 1.0},
              { "variableValue": "NC", "priorProbability": 0.0},
              { "variableValue": "DN", "priorProbability": 0.0}
            ]
           }
         },
         {
           "variableID": "ENSG00000163903",
           "priorDistribution": {
            "GaussianDistribution": { "distributionMean": 0.25, "distributionStDev": 0.1}
           }
         },
         {
           "variableID": "ENSG00000163902",
           "priorDistribution": {
            "PoissonDistribution":
              { "lambdaParameter": 1.0}
           }
         },
         {
           "variableID": "ENSG00000213190",
           "priorDistribution": {
            "empiricalDistribution":  { "distributionMean": 0.25, "distributionStDev": 0.1,
            "distributionPercentile": [1,2,3,4,5,6,7,8,9,10]}
           }
         }
       ]
     }
   ],
   "modelOutput": [
     {
       "variableGroupID": "GeneExpression",
       "variableID": ["ENSG00000070495"],
       "rawOutput": false
     },
     {
       "variableGroupID": "GeneKnockdown",
       "variableID": ["gene knockdown"],
       "rawOutput": false
     }
   ]
   }      
      """

    val json = Json.parse(jsonStr)
    val obj = json.validate[EvaluateModelRequest]
    obj shouldBe a[JsSuccess[_]]
    println(obj)
    info("OK")

  }

}