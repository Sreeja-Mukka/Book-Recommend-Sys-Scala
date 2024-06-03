package models

import models.BookRepository
import models.User

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future, blocking}

case class UserViewed (user_id: Int , book_id: Int)

@Singleton
class UserViewedRepository @Inject()(dbConfigProvider: DatabaseConfigProvider,bookRepo: BookRepository)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private  class UserViewedTable(tag: Tag) extends Table[UserViewed](tag, "UserViewedBooks") {


    def user_id = column[Int]("user_id")
    def book_id = column[Int]("book_id")

    override def * : ProvenShape[UserViewed] = (user_id,book_id) <> ((UserViewed.apply _).tupled, UserViewed.unapply)
  }
  private val user_viewed = TableQuery[UserViewedTable]


  def saveBookView(userId: Int, bookId: Int): Unit = {
    // SQL to insert or update the view record
    db.run(user_viewed += UserViewed(userId,bookId))
  }

//  def getBooksViewedByUser(userId: Int): Future[Seq[Book]] = {
//    val books = bookRepo.booksTableQuery
//    val query = for {
//      view <- user_viewed if view.user_id === userId
//      book <- bookRepo.findBook(view.book_id)
//    } yield book
//    db.run(query.result)
//  }
def getBooksViewedByUser(userId: Int): Future[Seq[Book]] = {
  val viewedBooksQuery = user_viewed.filter(_.user_id === userId).result

  val booksFuture = db.run(viewedBooksQuery).flatMap { userViews =>
    val bookFutures = userViews.map { userView =>
      bookRepo.findBook(userView.book_id)
    }

    Future.sequence(bookFutures).map { booksOption =>
      booksOption.collect { case Some(book) => book }
    }
  }

  booksFuture
}

}
