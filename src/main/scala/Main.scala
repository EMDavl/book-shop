import cats.data.ReaderT
import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s._
import config.Config
import controller.{AuthorsController, BookController, endpoints}
import dao.{AuthorSql, BookSql}
import domain.{IOWithRequestContext, RequestContext}
import doobie.util.transactor.Transactor
import org.http4s.ember.server._
import org.http4s.server.Router
import service.{AuthorStorage, BookStorage}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import tofu.logging.Logging

object Main extends IOApp {

  private val mainLogs =
    Logging.Make.plain[IO].byName("Main")

  override def run(args: List[String]): IO[ExitCode] =
    (for {
      _ <- Resource.eval(mainLogs.info("Starting library service..."))
      config <- Resource.eval(Config.load)
      transactor = Transactor
        .fromDriverManager[IO](
          config.db.driver,
          config.db.url,
          config.db.user,
          config.db.password
        )
        .mapK[IOWithRequestContext](ReaderT.liftK[IO, RequestContext])
      authorSql = AuthorSql.make
      bookSql = BookSql.make(authorSql)
      bookStorage = BookStorage.make(bookSql, transactor)
      authorStorage = AuthorStorage.make(authorSql, transactor)
      bookController = BookController.make(bookStorage)
      authorController = AuthorsController.make(authorStorage)
      routes = Http4sServerInterpreter[IO]().toRoutes(bookController.all ++ authorController.all) <+> endpoints.swaggerRoute();
      httpApp = Router("/" -> routes).orNotFound

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(
          Ipv4Address.fromString(config.app.host).getOrElse(ipv4"0.0.0.0")
        )
        .withPort(Port.fromString(config.app.port).getOrElse(port"8080"))
        .withHttpApp(httpApp)
        .build
    } yield ()).useForever.as(ExitCode.Success)
}