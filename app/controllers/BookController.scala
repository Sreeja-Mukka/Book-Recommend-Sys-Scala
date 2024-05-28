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
    bookRepo.list().map { book =>
      Ok(Json.toJson(book))
    }
  }

  def getBookById(bid: Int) = Action.async {
    bookRepo.findBook(bid).map {
      case Some(book) => Ok(Json.toJson(book))
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

  def createBook = Action.async(parse.json) { request =>
    request.body.validate[Book].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      book => {
        bookRepo.add(book).map(_ => Created(Json.toJson(book)))
      }
    )
  }

}
