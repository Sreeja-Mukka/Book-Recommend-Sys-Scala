package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import models.{BookRepository,Book}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BookController @Inject()(bookRepo: BookRepository,
                               cc: ControllerComponents) (implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val personFormat: Format[Book] = Json.format[Book]

  def getBooks = Action.async {
    bookRepo.list().map { books =>
      {
        Ok(views.html.books(books)) } // Ensure this matches the expected type in your view
    }
  }

  def getBookById(bid: Int) = Action.async {
    bookRepo.findBook(bid).map {
      case Some(book) => Ok(views.html.bookbyid(book))  //
      case None => NotFound
    }
  }

  def getFilteredBooks(genre: String) = Action.async {
    bookRepo.findBooksByGen(genre).map {
      case Some(books) => Ok(Json.toJson(books))
      case None => NotFound("No books found for the given author")
    } recover {
      case ex: Exception => InternalServerError("An error occurred: " + ex.getMessage)
    }
  }

 def deleteBook(bid: Int) = Action.async {
    bookRepo.delete(bid).map(_ => NoContent)
  }

  def index = Action.async { implicit request =>
    bookRepo.list().map { books =>
      Ok(views.html.AdminBooks(books))
    }
  }

  def createBook = Action.async { implicit request =>
    request.body.asFormUrlEncoded match {
      case Some(formData) =>
        try {
          val bno = formData("bno").head.toInt
          val bname = formData("bname").head
          val bauthor = formData("bauthor").head
          val bpub = formData("bpubyear").head.toInt
          val brating = formData("brating").head.toInt
          val bgenre = formData("bgenre").head
          val bsummary = formData("bsummary").head
          val newBook = Book(bno, bname, bauthor, bsummary, bpub, brating, bgenre)

          bookRepo.add(newBook).map { _ =>
            Redirect(routes.BookController.index) // Ensure correct function call
          }
        } catch {
          case e: NoSuchElementException => Future.successful(BadRequest("Missing form data"))
          case e: NumberFormatException => Future.successful(BadRequest("Invalid number format"))
          case e: Exception => Future.successful(BadRequest("An error occurred: " + e.getMessage))
        }
      case None => Future.successful(BadRequest("Expecting form URL encoded body"))
    }
  }
}
