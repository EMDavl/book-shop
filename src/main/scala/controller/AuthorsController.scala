package controller

import cats.effect.IO
import cats.syntax.either._
import controller.endpoints._
import domain.AuthorWithoutId
import sttp.tapir.server.ServerEndpoint
import domain.errors._
import service._

trait AuthorsController {
  def findPage: ServerEndpoint[Any, IO]
  def findById: ServerEndpoint[Any, IO]
  def findByFirstAndLastName: ServerEndpoint[Any, IO]
  def createAuthor: ServerEndpoint[Any, IO]
  def updateAuthor: ServerEndpoint[Any, IO]
  def removeAuthor: ServerEndpoint[Any, IO]
  def all: List[ServerEndpoint[Any, IO]]
}

object AuthorsController {

  final private class Impl(storage: AuthorStorage) extends AuthorsController {
    override def findPage: ServerEndpoint[Any, IO] =
      findPageAuthorEndpoint.serverLogic {
        case (ctx, limit, offset) => storage.findPage(limit, offset).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def findById: ServerEndpoint[Any, IO] =
      findByIdAuthorEndpoint.serverLogic {
        case (ctx, id) => storage.findAuthorById(id).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def findByFirstAndLastName: ServerEndpoint[Any, IO] =
      findByFirstNameAndLastNameAuthorEndpoint.serverLogic {
        case (ctx, firstName, lastName) => storage.findAuthorByFirstNameAndLastName(AuthorWithoutId(firstName, lastName)).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def createAuthor: ServerEndpoint[Any, IO] =
      createAuthorEndpoint.serverLogic {
        case (ctx, author) => storage.createAuthor(author).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def updateAuthor: ServerEndpoint[Any, IO] =
      updateAuthorEndpoint.serverLogic {
        case (ctx, author) => storage.updateAuthor(author).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def removeAuthor: ServerEndpoint[Any, IO] =
      removeAuthorByIdEndpoint.serverLogic {
        case (ctx, id) => storage.removeAuthor(id).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def all: List[ServerEndpoint[Any, IO]] = List(
      findPage,
      findById,
      findByFirstAndLastName,
      createAuthor,
      updateAuthor,
      removeAuthor
    )
  }

  def make(storage: AuthorStorage): AuthorsController = new Impl(storage)
}
