package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{Future, ExecutionContext}

case class Book (bno: Int, bname: String, bauthor: String, bsummary:String ,bpubyear: Int , brating: Int, bgenre: String)

@Singleton
class BookRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private  class BookTable(tag: Tag) extends Table[Book](tag, "book") {
    def bno = column[Int]("bno", O.PrimaryKey)
    def bname = column[String]("bname")
    def bauthor = column[String]("bauthor")
    def bsummary:Rep[String] = column[String]("bsummary")
    def bpubyear:Rep[Int] = column[Int]("bpubyear")
    def brating:Rep[Int] = column[Int]("brating")
    def bgenre:Rep[String] = column[String]("bgenre")
    def * = (bno, bname, bauthor , bsummary ,bpubyear , brating , bgenre) <> ((Book.apply _).tupled, Book.unapply)
  }

  private val books = TableQuery[BookTable]

  def list(): Future[Seq[Book]] = db.run(books.result)

  def findBook(bid: Int): Future[Option[Book]] = db.run(books.filter(_.bno === bid).result.headOption)

  def findBooksByGen(bgen: String): Future[Option[Seq[Book]]] = {
    val query = books.filter(_.bgenre.toLowerCase === bgen.toLowerCase())
    db.run(query.result).map { results =>
      if (results.isEmpty) None else Some(results)
    }
  }

  def add(book: Book): Future[Int] = db.run(books += book)

  def delete(bid: Int): Future[Int] = db.run(books.filter(_.bno === bid).delete)
}
