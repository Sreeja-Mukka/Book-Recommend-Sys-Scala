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
        case true => Redirect(routes.UserController.login)
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

        if (username == "admin" && password == "password") {
          // Directly redirect if it's an admin login
          Future.successful(Redirect(routes.BookController.index))
        } else {
          // Validate user and get user ID asynchronously
          userrepo.validateUser(username, password).flatMap {
            case Some(_) =>
              // User is valid, get the ID
              userrepo.getIdByName(username).flatMap {
                case Some(userId) =>
                  // Store userId in session and redirect to the books listing
                  Future.successful(Redirect(routes.BookController.getBooks).withSession("userId" -> userId.toString))
                case None =>
                  // Handle unexpected error, e.g., username valid but no ID found
                  Future.successful(Redirect(routes.UserController.login).flashing("error" -> "User not found"))
              }
            case None =>
              // User validation failed, redirect to login with an error
              Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Invalid username or password"))
          }
        }
      }
      case None =>
        // Form data not submitted, redirect to login with an error
        Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Form data missing"))
    }
  }


}