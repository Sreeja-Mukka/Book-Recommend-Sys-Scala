package controllers

import services.ProducerKafka
import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import models.{Book, BookRepository, User}
import play.api.inject.ApplicationLifecycle


import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BookController @Inject()(bookRepo: BookRepository, prod: ProducerKafka,
                               cc: ControllerComponents,lifecycle: ApplicationLifecycle) (implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val personFormat: Format[Book] = Json.format[Book]


  def getBooks = Action.async { implicit request =>
    bookRepo.list().map { books =>
      Ok(views.html.books(books))  // Ensure 'books' template has implicit Request and Flash parameters
    }.recover {
      case ex: Exception =>
        InternalServerError("An error occurred: " + ex.getMessage)
    }
  }

  def getBooksBtGenre(bgen:String) : Future[Option[Seq[Book]]]= {
    val books= bookRepo.findBooksByGen(bgen)
    books
  }

  def getBookById(bid: Int) = Action.async {
    bookRepo.findBook(bid).map {
      case Some(book) =>{
        getBooksBtGenre(book.bgenre).map {
          case Some(books) =>  prod.publishUserAction(book.bgenre,books)
        }
        Ok(views.html.bookbyid(book))
      }
      case None => NotFound
    }
  }

  def filter = Action.async { implicit request: Request[AnyContent] =>
    request.body.asFormUrlEncoded match {
      case Some(formData) =>
        formData.get("ugenre").flatMap(_.headOption) match {
          case Some(genre) =>
            bookRepo.findBooksByGen(genre).map {
              case Some(books) => Ok(views.html.books(books))
              case None => NotFound(s"Books of type $genre not found!")
            }
          case None =>
            Future.successful(BadRequest("Genre not specified"))
        }
      case None =>
        Future.successful(NotFound("Form data not found"))
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

