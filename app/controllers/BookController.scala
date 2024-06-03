package controllers

import services.ProducerKafka

import javax.inject._
import play.api.mvc.{request, _}
import play.api.libs.json._
import models.{Book, BookRepository, User, UserViewedRepository}
import play.api.inject.ApplicationLifecycle

import scala.Option.option2Iterable
import scala.collection.IterableOnce.iterableOnceExtensionMethods
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BookController @Inject()(bookRepo: BookRepository, prod: ProducerKafka, viewrepo : UserViewedRepository,
                               cc: ControllerComponents,lifecycle: ApplicationLifecycle) (implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val bookformat: Format[Book] = Json.format[Book]


  def getBooks = Action.async { implicit request =>
    val user_id: Option[Int] = request.session.get("userId").flatMap { s =>
      scala.util.Try(s.toInt).toOption
    }

    bookRepo.list().map { books =>
      user_id match {
        case Some(uid) =>
          Ok(views.html.books(books, uid)) // Assuming your view takes books and uid as parameters
        case None =>
          Ok(views.html.books(books, -1)) // Assuming -1 indicates no user id
      }
    }.recover {
      case ex: Exception =>
        InternalServerError("An error occurred: " + ex.getMessage)
    }
  }


  def getBooksBtGenre(bgen:String) : Future[Option[Seq[Book]]]= {
    val books= bookRepo.findBooksByGen(bgen)
    books
  }

//  def showUserBooks() = Action.async { implicit request =>
//    request.session.get("userId").map(_.toInt) match {
//      case Some(userId) =>
//        viewrepo.getBooksViewedByUser(userId).map { books =>
//          Ok(views.html.viewedBooks(books))
//        }
//      case None =>
//        Future.successful(Redirect(routes.UserController.login).flashing("error" -> "Please log in first"))
//    }
//  }
def booksViewedByUser(userId: Int) = Action.async { implicit request: Request[AnyContent] =>
  viewrepo.getBooksViewedByUser(userId).map { books =>
    Ok(views.html.viewedBooks(books)) // Assuming you have a view to display books
  }
}

  def getBookById(bid: Int) = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption: Option[Int] = request.session.get("userId").flatMap { s =>
      scala.util.Try(s.toInt).toOption
    }

    userIdOption.foreach { uid =>
      // Assuming you have a method to save or update the view
      viewrepo.saveBookView(uid, bid)
    }
    bookRepo.findBook(bid).map {
      case Some(book) =>{
        getBooksBtGenre(book.bgenre).map {
          case Some(books) => prod.publishUserAction(book.bgenre, books)
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
              case Some(books) => Ok(views.html.books(books,0))
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
            Redirect(routes.BookController.index)
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

