import cats.data.ReaderT
import cats.effect.IO
import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.util.Read
import io.estatico.newtype.macros.newtype
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, Schema}
import tofu.logging.derivation._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime}
package object domain {


  @derive(loggable, encoder, decoder)
  @newtype
  case class Id(value: Long)
  object Id {
    implicit val doobieRead: Read[Id] = Read[Long].map(Id(_))

    implicit val schema: Schema[Id] =
      Schema.schemaForLong.map(l => Some(Id(l)))(_.value)

    implicit val codec: Codec[String, Id, TextPlain] =
      Codec.long.map(Id(_))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class BookName(value: String)
  object BookName {
    implicit val doobieRead: Read[BookName] = Read[String].map(BookName(_))
    implicit val schema: Schema[BookName] =
      Schema.schemaForString.map(n => Some(BookName(n)))(_.value)
    implicit val codec: Codec[String, BookName, TextPlain] =
      Codec.string.map(BookName(_))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class AuthorFirstName(value: String)

  object AuthorFirstName {
    implicit val doobieRead: Read[AuthorFirstName] = Read[String].map(AuthorFirstName(_))
    implicit val schema: Schema[AuthorFirstName] =
      Schema.schemaForString.map(n => Some(AuthorFirstName(n)))(_.value)
    implicit val codec: Codec[String, AuthorFirstName, TextPlain] =
      Codec.string.map(AuthorFirstName(_))(_.value)
  }

  @derive(loggable, encoder, decoder)
  @newtype
  case class AuthorLastName(value: String)

  object AuthorLastName {
    implicit val doobieRead: Read[AuthorLastName] = Read[String].map(AuthorLastName(_))
    implicit val schema: Schema[AuthorLastName] =
      Schema.schemaForString.map(n => Some(AuthorLastName(n)))(_.value)
    implicit val codec: Codec[String, AuthorLastName, TextPlain] =
      Codec.string.map(AuthorLastName(_))(_.value)
  }


  @derive(loggable, encoder, decoder)
  @newtype
  case class PublishDate(value: LocalDate)
  object PublishDate {
    implicit val doobieRead: Read[PublishDate] =
      Read[String].map(ts => PublishDate(LocalDate.parse(ts)))
    implicit val schema: Schema[PublishDate] = Schema.schemaForString.map(n =>
      Some(PublishDate(LocalDate.parse(n)))
    )(_.value.format(DateTimeFormatter.ISO_LOCAL_DATE))

  }

  type IOWithRequestContext[A] = ReaderT[IO, RequestContext, A]
}