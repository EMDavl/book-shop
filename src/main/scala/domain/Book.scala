package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, encoder, decoder)
final case class BookWithoutId(name: BookName, authorId: Id, publishDate: PublishDate)
object BookWithoutId {
  implicit val schema: Schema[BookWithoutId] = Schema.derived
}
@derive(loggable, encoder, decoder)
final case class Book (id: Id, name: BookName, authorId: Id, publishDate: PublishDate)
object Book {
  implicit val schema: Schema[Book] = Schema.derived
}