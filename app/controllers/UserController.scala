package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import models.{User, UserRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(userrepo: UserRepository,
                               cc: ControllerComponents ) (implicit ec: ExecutionContext) extends AbstractController(cc)  {

  implicit val personFormat: Format[User] = Json.format[User]

  def createUser = Action.async(parse.json) { request =>
    request.body.validate[User].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      book => {
        userrepo.addUser(book).map(_ => Created(Json.toJson(book)))
      }
    )
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

        userCheck.flatMap {
          case true =>
            // If userCheck is true, redirect to the main page with a success message
            Future.successful(Redirect(routes.UserController.login).flashing("success" -> "User Logged In"))
          case false =>
            // If userCheck is false, redirect back to login with an error message
            // re-direct to login
            Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Invalid username or password"))
        }

        if(username == "admin" && password == "password") {
          //direct to admin listofbooks page
          Future.successful(Redirect(routes.UserController.login))
        }
        else{
          //re-direct to login
          Future.successful(Redirect(routes.UserController.login))
        }
      }
      case None =>
        // Handle the case where form data is not sent correctly
        Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Form data missing"))
    }
  }
}