package controller

import cats.effect.IO
import cats.syntax.either._
import controller.endpoints._
import sttp.tapir.server.ServerEndpoint
import domain.errors._
import service._

trait BookController {
  def findPage: ServerEndpoint[Any, IO]

  def findById: ServerEndpoint[Any, IO]

  def findAllByAuthorId: ServerEndpoint[Any, IO]

  def updateBook: ServerEndpoint[Any, IO]

  def removeBook: ServerEndpoint[Any, IO]

  def createBook: ServerEndpoint[Any, IO]
  def all: List[ServerEndpoint[Any, IO]]
}

object BookController {

  final private class Impl(storage: BookStorage) extends BookController {
    override def findPage: ServerEndpoint[Any, IO] =
      findPageBookEndpoint.serverLogic {
        case (ctx, limit, offset) => storage.findPage(limit, offset).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def findById: ServerEndpoint[Any, IO] =
      findByIdBookEndpoint.serverLogic {
        case (ctx, id) => storage.findBookById(id).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def findAllByAuthorId: ServerEndpoint[Any, IO] =
      findAllByAuthorIdEndpoint.serverLogic {
        case (ctx, authorId) => storage.findAllByAuthorId(authorId).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def updateBook: ServerEndpoint[Any, IO] =
      updateBookEndpoint.serverLogic {
        case (ctx, book) => storage.updateBook(book).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def removeBook: ServerEndpoint[Any, IO] =
      removeBookByIdEndpoint.serverLogic {
        case (ctx, id) => storage.removeBook(id).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def createBook: ServerEndpoint[Any, IO] =
      createBookEndpoint.serverLogic {
        case (ctx, book) => storage.createBook(book).map(_.leftMap[CommonError](identity)).run(ctx)
      }

    override def all: List[ServerEndpoint[Any, IO]] = List(
      findPage,
      findById,
      findAllByAuthorId,
      updateBook,
      removeBook,
      createBook
    )
  }

  def make(storage: BookStorage): BookController = new Impl(storage)
}
