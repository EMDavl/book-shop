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

trait BookStorage {
  def findPage(limit: Int, offset: Long): IOWithRequestContext[Either[CommonError, List[Book]]]

  def findBookById(id: Id): IOWithRequestContext[Either[InternalError, Option[Book]]]

  def findAllByAuthorId(id: Id): IOWithRequestContext[Either[InternalError, List[Book]]]

  def updateBook(book: Book): IOWithRequestContext[Either[CommonError, Book]]

  def removeBook(id: Id): IOWithRequestContext[Either[CommonError, Unit]]

  def createBook(book: BookWithoutId): IOWithRequestContext[Either[CommonError, Book]]
}

object BookStorage {
  private final class Impl(sql: BookSql,
                           transactor: Transactor[IOWithRequestContext])
    extends BookStorage {
    override def findPage(limit: Int, offset: Long): IOWithRequestContext[Either[CommonError, List[Book]]] = limit match {
      case value if value < 1 => (BadRequest("Limit must be greater than 0"): CommonError).asLeft[List[Book]].pure[IOWithRequestContext]
      case _ => sql.findPage(limit, offset).transact(transactor).attempt.map(_.leftMap(InternalError))
    }

    override def findBookById(id: Id): IOWithRequestContext[Either[InternalError, Option[Book]]] =
      sql
        .findById(id)
        .transact(transactor)
        .attempt
        .map(_.leftMap(InternalError))

    override def findAllByAuthorId(id: Id): IOWithRequestContext[Either[InternalError, List[Book]]] =
      sql
        .findAllByAuthorId(id)
        .transact(transactor)
        .attempt
        .map(_.leftMap(InternalError))

    override def updateBook(book: Book): IOWithRequestContext[Either[CommonError, Book]] =
      sql
        .update(book)
        .transact(transactor)
        .attempt
        .map {
          case Left(th) => InternalError(th).asLeft[Book]
          case Right(Left(error)) => error.asLeft[Book]
          case Right(Right(book)) => book.asRight[CommonError]
        }

    override def removeBook(id: Id): IOWithRequestContext[Either[CommonError, Unit]] =
      sql.removeById(id).transact(transactor).attempt.map {
        case Left(th) => InternalError(th).asLeft[Unit]
        case Right(Left(error)) => error.asLeft[Unit]
        case _ => ().asRight[CommonError]
      }

    override def createBook(book: BookWithoutId): IOWithRequestContext[Either[CommonError, Book]] =
      sql.create(book).transact(transactor).attempt.map {
        case Left(th) => InternalError(th).asLeft[Book]
        case Right(Left(error)) => error.asLeft[Book]
        case Right(Right(book)) => book.asRight[CommonError]
      }
  }

  private final class LoggingImpl(storage: BookStorage)
                                 (implicit logging: Logging[IOWithRequestContext]
                                 ) extends BookStorage {

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

    override def findPage(limit: Int, offset: Long): IOWithRequestContext[Either[CommonError, List[Book]]] = {
      surroundWithLogs[CommonError, List[Book]]("Getting books page") {
        error =>
          (s"Error while fetching books page: ${error.message} ${error.cause}", error.cause)
      } {
        _ =>
          s"Books page successfully fetched"
      }(storage.findPage(limit, offset))
    }

    override def findBookById(id: Id): IOWithRequestContext[Either[InternalError, Option[Book]]] =
      surroundWithLogs[InternalError, Option[Book]]("Getting book by id") {
        errors =>
          (s"Error while fetching book by id. ${errors.message}", errors.cause)
      } {
        result =>
          (s"Successfully fetched book: ${result.mkString}")
      }(storage.findBookById(id))

    override def findAllByAuthorId(id: Id): IOWithRequestContext[Either[InternalError, List[Book]]] = {
      surroundWithLogs[InternalError, List[Book]](s"Getting books by author id ${id.value}") {
        errors =>
          (s"Error while fetching books by author id. ${errors.message} ${errors.cause}", errors.cause)
      } {
        _ =>
          (s"Successfully fetched books by author id ${id.value}")
      }(storage.findAllByAuthorId(id))
    }

    override def updateBook(book: Book): IOWithRequestContext[Either[CommonError, Book]] = {
      surroundWithLogs[CommonError, Book](s"Updating book: ${book}") {
        errors =>
          (s"Error while updating book. ${errors.message} ${errors.cause}", errors.cause)
      } {
        _ =>
          (s"Successfully updated book with id ${book.id.value}")
      }(storage.updateBook(book))
    }

    override def removeBook(id: Id): IOWithRequestContext[Either[CommonError, Unit]] = {
      surroundWithLogs[CommonError, Unit](s"Removing book with id: ${id.value}") {
        errors =>
          (s"Error while removing book. ${errors.message}", errors.cause)
      } {
        result =>
          (s"Successfully removed book with id ${id.value}")
      }(storage.removeBook(id))
    }

    override def createBook(book: BookWithoutId): IOWithRequestContext[Either[CommonError, Book]] = {
      surroundWithLogs[CommonError, Book](s"Creating book: ${book}") {
        errors =>
          (s"Error while creating book. ${errors.message} ${errors.cause}", errors.cause)
      } {
        result =>
          (s"Successfully created book with id ${result.id.value}")
      }(storage.createBook(book))
    }
  }

  def make(
            sql: BookSql,
            transactor: Transactor[IOWithRequestContext]
          ): BookStorage = {
    implicit val logs =
      Logging.Make
        .contextual[IOWithRequestContext, RequestContext]
        .forService[BookStorage]
    val storage = new Impl(sql, transactor)
    new LoggingImpl(storage)
  }
}
