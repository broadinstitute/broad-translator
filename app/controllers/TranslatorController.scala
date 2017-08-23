package controllers

import javax.inject.{ Inject, Singleton }

import broadtranslator.AppWiring
import broadtranslator.engine.api.id.ModelId
import broadtranslator.json.TranslatorJsonApi
import broadtranslator.json.smart.TranslatorSmartApi
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import util.HttpContentTypes
import util.rdf.Rdf4jUtils

/**
 * broadtranslator
 * Created by oliverr on 3/30/2017.
 */
@Singleton
class TranslatorController @Inject() extends Controller {

  val logger = Logger(getClass)

  val jsonApi: TranslatorJsonApi = AppWiring.jsonApi
  val smartApi: TranslatorSmartApi = AppWiring.smartApi

  def logRequest(implicit request: Request[_]): Unit = {
    logger.info(request.toString())
  }

  /**
   * Get the list of available models in JSON
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def getModelList: Action[AnyContent] = Action { implicit request =>
    logRequest
    Ok(jsonApi.getModelList)
  }

  def getModelSignature(modelId: String): Action[AnyContent] = Action { implicit request =>
    logRequest
    Ok(jsonApi.getModelSignature(ModelId(modelId)))
  }

  def evaluate: Action[JsValue] = Action(parse.json) { implicit request =>
    logRequest
    Ok(jsonApi.evaluate(request.body))
  }

  def smart(modelId: String): Action[AnyContent] = Action { implicit request =>
    logRequest
    val repo = smartApi.getSmartApi(ModelId(modelId))
    val jsonLdString = Rdf4jUtils.getContentAsString(repo)
    Ok(jsonLdString).as(HttpContentTypes.json) // Switch to JSON-LD
  }

}
