package broadtranslator.json

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import play.api.libs.json._
import broadtranslator.engine.api.evaluate.{ EvaluateModelRequest, EvaluateModelResult }
import broadtranslator.json.EvaluateRequestJsonReading.evaluateRequestReads
import broadtranslator.json.EvaluateRequestJsonWriting.evaluateRequestWrites
import broadtranslator.json.EvaluateResultJsonReading.evaluateResultReads
import broadtranslator.json.EvaluateResultJsonWriting.evaluateResultWrites

class JsonWritingTest extends FlatSpec with Matchers {

  "Evaluate request JSON writer" should
    "write mock request" in {

      val jsonStr = """
      {
  "modelID": "ModelThree",
  "modelOutput": [ 
    { "variableGroupID": "oranges",
     "variableID": ["Navel","Clementine", "Tangerine"],
     "rawOutput": false
    }
  ],
  "modelInput": [
    {"variableGroupID": "apples",
     "modelVariable": [
       { "variableID": "Gala",
         "priorDistribution": 
         {"discreteDistribution": [
           { "variableValue": "red","priorProbability": 0.15},
           { "variableValue": "green","priorProbability": 0.45},
           { "variableValue": "yellow","priorProbability": 0.40}
         ]}
       },
       { "variableID": "Granny Smith",
         "priorDistribution":{
            "empiricalDistribution":  { "distributionMean": 5.5, "distributionStDev": 2.5,
            "distributionPercentile": [1,2,3,4,5,6,7,8,9,10]}
           }
       },
       { "variableID": "Fuji",
         "priorDistribution":  
         {"GaussianDistribution":{ "distributionMean": 0.25, "distributionStDev": 0.1}
         }
       }
     ]
    }
  ]
}
      """
      val json = Json.parse(jsonStr)
      val obj = json.validate[EvaluateModelRequest]
      obj shouldBe a[JsSuccess[_]]
      info("OK")
      val request = obj.asInstanceOf[JsSuccess[EvaluateModelRequest]].value
      val jsonStr1 = Json.prettyPrint(Json.toJson(request))
      println(jsonStr1)
      val json1 = Json.parse(jsonStr1)
      val obj1 = json1.validate[EvaluateModelResult]
      obj1 shouldBe a[JsSuccess[_]]
      info("OK")
    }

  "Evaluate response JSON writer" should
    "write JSON" in {
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
      info("OK")
      val request = obj.asInstanceOf[JsSuccess[EvaluateModelResult]].value
      val jsonStr1 = Json.prettyPrint(Json.toJson(request))
      println(jsonStr1)
      val json1 = Json.parse(jsonStr1)
      val obj1 = json1.validate[EvaluateModelResult]
      obj1 shouldBe a[JsSuccess[_]]
      info("OK")
    }
}