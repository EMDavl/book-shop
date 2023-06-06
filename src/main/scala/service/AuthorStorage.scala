package service

import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.applicativeError._
import cats.syntax.either._
import doobie._
import doobie.implicits._
import tofu.logging.Logging
import domain.errors._
import domain._
import dao._

trait AuthorStorage{
  def findPage(limit: Int, offset: Long): IOWithRequestContext[Either[CommonError, List[Author]]]

  def findAuthorById(id: Id): IOWithRequestContext[Either[InternalError, Option[Author]]]

  def findAuthorByFirstNameAndLastName(author: AuthorWithoutId): IOWithRequestContext[Either[InternalError, Option[Author]]]

  def updateAuthor(author: Author): IOWithRequestContext[Either[CommonError, Author]]

  def removeAuthor(id: Id): IOWithRequestContext[Either[CommonError, Unit]]

  def createAuthor(author: AuthorWithoutId): IOWithRequestContext[Either[CommonError, Author]]
}
object AuthorStorage {
  private final class Impl(sql: AuthorSql,
                           transactor: Transactor[IOWithRequestContext])
    extends AuthorStorage {
    override def findPage(limit: Int, offset: Long): IOWithRequestContext[Either[CommonError, List[Author]]] = limit match {
      case value if value < 1 || value > 100 => (BadRequest("Limit must be greater than 0 and less than 100"): CommonError).asLeft[List[Author]].pure[IOWithRequestContext]
      case _ => sql.findPage(limit, offset).transact(transactor).attempt.map(_.leftMap(InternalError))
    }

    override def findAuthorById(id: Id): IOWithRequestContext[Either[InternalError, Option[Author]]] =
      sql
        .findById(id)
        .transact(transactor)
        .attempt
        .map(_.leftMap(InternalError))

    override def findAuthorByFirstNameAndLastName(author: AuthorWithoutId): IOWithRequestContext[Either[InternalError, Option[Author]]] =
      sql
        .findByFirstNameAndLastName(author)
        .transact(transactor)
        .attempt
        .map(_.leftMap(InternalError))

    override def updateAuthor(author: Author): IOWithRequestContext[Either[CommonError, Author]] =
      sql
        .updateAuthor(author)
        .transact(transactor)
        .attempt
        .map {
          case Left(th) => InternalError(th).asLeft[Author]
          case Right(Left(error)) => error.asLeft[Author]
          case Right(Right(book)) => book.asRight[CommonError]
        }

    override def removeAuthor(id: Id): IOWithRequestContext[Either[CommonError, Unit]] =
      sql.removeById(id).transact(transactor).attempt.map {
        case Left(th) => InternalError(th).asLeft[Unit]
        case Right(Left(error)) => error.asLeft[Unit]
        case _ => ().asRight[CommonError]
      }

    override def createAuthor(author: AuthorWithoutId): IOWithRequestContext[Either[CommonError, Author]] =
      sql.createAuthor(author).transact(transactor).attempt.map {
        case Left(th) => InternalError(th).asLeft[Author]
        case Right(Left(error)) => error.asLeft[Author]
        case Right(Right(book)) => book.asRight[CommonError]
      }
  }

  private final class LoggingImpl(storage: AuthorStorage)
                                 (implicit logging: Logging[IOWithRequestContext]
                                 ) extends AuthorStorage {

    private def surroundWithLogs[Error, Res](
                                              inputLog: String
                                            )(errorOutputLog: Error => (String, Option[Throwable]))(
                                              successOutputLog: Res => String
                                            )(
                                              io: IOWithRequestContext[Either[Error, Res]]
                                            ): IOWithRequestContext[Either[Error, Res]] =
      for {
        _ <- logging.info(inputLog)
        res <- io
        _ <- res match {
          case Left(error) => {
            val (msg, cause) = errorOutputLog(error)
            cause.fold(logging.error(msg))(cause => logging.error(msg, cause))
          }
          case Right(result) => logging.info(successOutputLog(result))
        }
      } yield res


    override def findPage(limit: Int, offset: Long): IOWithRequestContext[Either[CommonError, List[Author]]] =
      surroundWithLogs[CommonError, List[Author]]("Getting authors page") {
        error =>
          (s"Error while fetching authors page: ${error.message}", error.cause)
      } {
        _ =>
          s"Books page successfully fetched"
      }(storage.findPage(limit, offset))

    override def findAuthorById(id: Id): IOWithRequestContext[Either[InternalError, Option[Author]]] =
      surroundWithLogs[InternalError, Option[Author]]("Getting author by id") {
        errors =>
          (s"Error while fetching author by id. ${errors.message}", errors.cause)
      } {
        result =>
          (s"Successfully fetched author: ${result.mkString}")
      }(storage.findAuthorById(id))

    override def findAuthorByFirstNameAndLastName(author: AuthorWithoutId): IOWithRequestContext[Either[InternalError, Option[Author]]] =
      surroundWithLogs[InternalError, Option[Author]](s"Getting books by author first/last name ${author}") {
        errors =>
          (s"Error while fetching books by author first/last name. ${errors.message}", errors.cause)
      } {
        author =>
          (s"Successfully fetched author with id ${author.mkString}")
      }(storage.findAuthorByFirstNameAndLastName(author))


    override def updateAuthor(author: Author): IOWithRequestContext[Either[CommonError, Author]] =
      surroundWithLogs[CommonError, Author](s"Updating author: ${author}") {
        errors =>
          (s"Error while updating author. ${errors.message}", errors.cause)
      } {
        _ =>
          (s"Successfully updated author with id ${author.id.value}")
      }(storage.updateAuthor(author))


    override def removeAuthor(id: Id): IOWithRequestContext[Either[CommonError, Unit]] =
      surroundWithLogs[CommonError, Unit](s"Removing author with id: ${id.value}") {
        errors =>
          (s"Error while removing author. ${errors.message}", errors.cause)
      } {
        result =>
          (s"Successfully removed author with id ${id.value}")
      }(storage.removeAuthor(id))

    override def createAuthor(author: AuthorWithoutId): IOWithRequestContext[Either[CommonError, Author]] =
      surroundWithLogs[CommonError, Author](s"Creating author: ${author}") {
        errors =>
          (s"Error while creating author. ${errors.message}", errors.cause)
      } {
        result =>
          (s"Successfully created author with id ${result.id.value}")
      }(storage.createAuthor(author))

  }

  def make(
            sql: AuthorSql,
            transactor: Transactor[IOWithRequestContext]
          ): AuthorStorage = {
    implicit val logs =
      Logging.Make
        .contextual[IOWithRequestContext, RequestContext]
        .forService[BookStorage]
    val storage = new Impl(sql, transactor)
    new LoggingImpl(storage)
  }
}
