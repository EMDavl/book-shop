package domain

import cats.syntax.option._
import io.circe.{Decoder, Encoder, HCursor, Json}
import sttp.tapir.Schema

object errors {
  sealed abstract class CommonError(
                                  val message: String,
                                  val cause: Option[Throwable] = None
                                )

  object CommonError {
    implicit val encoder: Encoder[CommonError] = (a: CommonError) => Json.obj(
      ("message", Json.fromString(a.message))
    )

    implicit val decoder: Decoder[CommonError] = (c: HCursor) => c.downField("message").as[String].map(MockError)

    implicit val schema: Schema[CommonError] = Schema.string[CommonError]
  }

  case class BadRequest(override val message: String)
    extends CommonError(message)

  case class DataNotFound(id: Id)
    extends CommonError(s"Data not found by provided id: ${id.value}")

  case class DataAlreadyExists()
    extends CommonError("Data already exists!")
  case class InternalError(cause0: Throwable)
    extends CommonError("Internal error", cause0.some)

  case class MockError(override val message: String) extends CommonError(message)
}
