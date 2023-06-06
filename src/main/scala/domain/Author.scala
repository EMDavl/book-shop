package domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, encoder, decoder)
final case class AuthorWithoutId(firstName: AuthorFirstName, lastName:AuthorLastName)
object AuthorWithoutId {
  implicit val schema: Schema[AuthorWithoutId] = Schema.derived
}

@derive(loggable, encoder, decoder)
final case class Author (id: Id, firstName: AuthorFirstName, lastName: AuthorLastName)
object Author {
  implicit val schema: Schema[Author] = Schema.derived
}
