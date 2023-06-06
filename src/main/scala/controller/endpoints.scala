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
      .in(path[Id]("id").description("id автора"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Option[Author]])
      .description("Метод для получения автора по id")

  val findByFirstNameAndLastNameAuthorEndpoint: Endpoint[Unit, (RequestContext, AuthorFirstName, AuthorLastName), CommonError, Option[Author], Any] =
    authorsEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in("find")
      .in(query[AuthorFirstName]("firstName").description("Имя автора"))
      .in(query[AuthorLastName]("lastName").description("Фамилия автора"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Option[Author]])
      .description("Метод для получения автора по имени и фамилии")

  val createAuthorEndpoint: Endpoint[Unit, (RequestContext, AuthorWithoutId), CommonError, Author, Any] =
    authorsEndpoint.post
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[AuthorWithoutId])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Author])
      .description("Метод для создания автора")

  val updateAuthorEndpoint: Endpoint[Unit, (RequestContext, Author), CommonError, Author, Any] =
    authorsEndpoint.put
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[Author])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Author])
      .description("Метод для обновления автора")

  val removeAuthorByIdEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Unit, Any] =
    authorsEndpoint.delete
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[CommonError])
      .in(path[Id]("id").description("id автора для удаления"))
      .description("Метод для удаления автора по id")


  val findPageBookEndpoint: Endpoint[Unit, (RequestContext, Int, Long), CommonError, List[Book], Any] =
    booksEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in(query[Int]("limit").description("Максимальное количество записей в запросе"))
      .in(query[Long]("offset").description("Id автора от которого будем отталкиваться при поиске новой страницы"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[List[Book]])
      .description("Метод для пагинированного получения книг")

  val findByIdBookEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Option[Book], Any] =
    booksEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in(path[Id]("id").description("id книги"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Option[Book]])
      .description("Метод для получения книги по id")

  val findAllByAuthorIdEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, List[Book], Any] =
    booksEndpoint.get
      .in(header[RequestContext]("X-Request-Id"))
      .in("author" / path[Id](name = "id").description("id автора"))
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[List[Book]])
      .description("Метод для получения всех книг автора по его id")

  val updateBookEndpoint: Endpoint[Unit, (RequestContext, Book), CommonError, Book, Any] =
    booksEndpoint.put
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[Book])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Book])
      .description("Метод для обновления книги")

  val removeBookByIdEndpoint: Endpoint[Unit, (RequestContext, Id), CommonError, Unit, Any] =
    booksEndpoint.delete
      .in(header[RequestContext]("X-Request-Id"))
      .errorOut(jsonBody[CommonError])
      .in(path[Id]("id").description("id книги для удаления"))
      .description("Метод для удаления книги по id")

  val createBookEndpoint: Endpoint[Unit, (RequestContext, BookWithoutId), CommonError, Book, Any] =
    booksEndpoint.post
      .in(header[RequestContext]("X-Request-Id"))
      .in(jsonBody[BookWithoutId])
      .errorOut(jsonBody[CommonError])
      .out(jsonBody[Book])
      .description("Метод для создания книги")

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
