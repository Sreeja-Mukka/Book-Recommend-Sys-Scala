package models

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

case class User (uname:String , upassword:String)

@Singleton
class UserRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private  class UserTable(tag: Tag) extends Table[User](tag, "user") {

    def uname = column[String]("uname")
    def upassword = column[String]("upassword")

    override def * : ProvenShape[User] = (uname,upassword) <> ((User.apply _).tupled, User.unapply)
  }

  private var users = TableQuery[UserTable]


  def addUser(user:User) :Future[Int] = {
    db.run(users += user)
  }

  def validateUser(uname: String, upassword: String): Future[Option[Boolean]] = {
    val query = users.filter(_.uname === uname).result.headOption
    db.run(query).map {
      case Some(user) if user.upassword == upassword =>
        Some(true)  // Password matches
      case _ =>
        None  // User not found or password doesn't match
    }
  }

  def register(username: String, password: String): Future[Boolean] = {

    val newuser = User(username,password)
    if (username.length >= 2 && password.length >= 2) {
      val userExists = db.run(users.filter(_.uname === username).exists.result)
      userExists.flatMap {
        case true =>
          Future.successful(false) // Username already exists, return false
        case false =>
          db.run(users += newuser).map(_ => true) // Add user and return true
      }
    } else {
      Future.successful(false) // Validation failed, return false
    }
  }
}
