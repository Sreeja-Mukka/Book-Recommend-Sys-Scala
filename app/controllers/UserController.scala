package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import models.{User, UserRepository}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class UserController @Inject()(userrepo: UserRepository,
                               cc: ControllerComponents ) (implicit ec: ExecutionContext) extends AbstractController(cc) {


  implicit val personFormat: Format[User] = Json.format[User]

  def register = Action { implicit request =>
    Ok(views.html.register())
  }

  def registerSubmit = Action.async { implicit request =>
    val postVals = request.body.asFormUrlEncoded
    postVals.map { args =>
      val username = args("username").head
      val password = args("password").head
      userrepo.register(username, password).map {
        case true => Redirect(routes.UserController.login) // Redirect to login on success
        case false => BadRequest("Username already exists.")
      }
    }.getOrElse(Future.successful(BadRequest("Form data missing")))
  }

  def login = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login())
  }

  def loginSubmit = Action.async { implicit request: Request[AnyContent] =>
    request.body.asFormUrlEncoded match {
      case Some(formData) => {
        val username = formData("uname").head
        val password = formData("upassword").head

        val userCheck: Future[Boolean] = userrepo.validateUser(username, password).map {
          case Some(_) => true  // User exists and password is correct
          case None => false    // User not found or password is incorrect
        }

        if(username == "admin" && password == "password") {
          Future.successful(Redirect(routes.BookController.index))
        }
        else{
          userCheck.flatMap {
            case true =>
              Future.successful(Redirect(routes.BookController.getBooks))
            case false =>
              Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Invalid username or password"))
          }
        }
      }
      case None =>
        Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Form data missing"))
    }
  }

}