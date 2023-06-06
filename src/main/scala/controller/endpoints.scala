package controller

import cats.effect.IO
import domain.errors.CommonError
import domain._
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter

object endpoints {

  private val authorsEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] =
    endpoint.in("authors")

  private val booksEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] =
    endpoint.in("books")

  val findPageAuthorEndpoint: Endpoint[Unit, (RequestContext, Int, Long), CommonError, List[Author], Any] =
    authorsEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in(query[Int]("limit").description("Максимальное количество записей в запросе"))
      .in(query[Long]("offset").description("Id автора от которого будем отталкиваться при поиске новой страницы"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[List[Author]])
      .description("Метод для пагинированного получения авторов")

  val findByIdAuthorEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Option[Author], Any] =
    authorsEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in(path[Id]("id"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Option[Author]])

  val findByFirstNameAndLastNameAuthorEndpoint: Endpoint[Unit, (RequestContext, AuthorWithoutId), CommonError, Option[Author], Any] =
    authorsEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in("find")
      .in(jsonBody[AuthorWithoutId])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Option[Author]])

  val createAuthorEndpoint: Endpoint[Unit, (RequestContext, AuthorWithoutId), CommonError, Author, Any] =
    authorsEndpoint.post
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[AuthorWithoutId])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Author])

  val updateAuthorEndpoint: Endpoint[Unit, (RequestContext, Author), CommonError, Author, Any] =
    authorsEndpoint.put
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[Author])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Author])

  val removeAuthorByIdEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Unit, Any] =
    authorsEndpoint.delete
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[CommonError])
      .in(path[Id]("id"))


  val findPageBookEndpoint: Endpoint[Unit, (RequestContext, Int, Long), CommonError, List[Book], Any] =
    booksEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in(query[Int]("limit"))
      .in(query[Long]("offset"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[List[Book]])

  val findByIdBookEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Option[Book], Any] =
    booksEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in(path[Id]("id"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Option[Book]])

  val findAllByAuthorIdEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, List[Book], Any] =
    booksEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in("author" / path[Id])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[List[Book]])

  val updateBookEndpoint: Endpoint[Unit, (RequestContext, Book), CommonError, Book, Any] =
    booksEndpoint.put
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[Book])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Book])

  val removeBookByIdEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Unit, Any] =
    booksEndpoint.delete
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[CommonError])
      .in(path[Id]("id"))

  val createBookEndpoint: Endpoint[Unit, (RequestContext, BookWithoutId), CommonError, Book, Any] =
    booksEndpoint.post
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[BookWithoutId])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Book])

  def swaggerRoute(): HttpRoutes[IO] = {
    val endpoints =
      List(
        findPageAuthorEndpoint,
        findByIdAuthorEndpoint,
        findByFirstNameAndLastNameAuthorEndpoint,
        createAuthorEndpoint,
        updateAuthorEndpoint,
        removeAuthorByIdEndpoint,
        findPageBookEndpoint,
        findByIdBookEndpoint,
        findAllByAuthorIdEndpoint,
        updateBookEndpoint,
        removeBookByIdEndpoint,
        createBookEndpoint
      )

    val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[IO](endpoints, "Library", "1.0")

    Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)
  }

}
