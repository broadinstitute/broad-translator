package controllers

import javax.inject._

import play.api._
import play.api.mvc._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(configuration: Configuration) extends Controller {

  val logger = Logger(getClass)
  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = Action { implicit request =>
    logger.info("Received request for home page.")
    Ok(views.html.index(configuration.getString("example.greeting").getOrElse("Howdy!")))
  }
}
