package dao

import cats.implicits.catsSyntaxEitherId
import domain.errors.{DataAlreadyExists, DataNotFound}
import domain.{Author, AuthorFirstName, AuthorLastName, AuthorWithoutId, Id}
import doobie.ConnectionIO
import doobie.implicits.{toDoobieApplicativeErrorOps, toSqlInterpolator}
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.util.query.Query0
import doobie.util.update.Update0

trait AuthorSql {
  def findPage(limit: Int, offset: Long): ConnectionIO[List[Author]]
  def findById(id: Id): ConnectionIO[Option[Author]]
  def findByFirstNameAndLastName(author: AuthorWithoutId): ConnectionIO[Option[Author]]
  def createAuthor(author: AuthorWithoutId): ConnectionIO[Either[DataAlreadyExists, Author]]
  def updateAuthor(author: Author): ConnectionIO[Either[DataNotFound, Author]]
  def removeById(id: Id): ConnectionIO[Either[DataNotFound, Unit]]

}
object AuthorSql {

  object scripts {
    def findPageSql(limit: Int, offset: Long): Query0[Author] =
      sql"select * from authors where id > $offset ORDER BY id LIMIT $limit".query[Author]

    def findByIdSql(id: Id): Query0[Author] =
      sql"select * from authors where id = ${id.value}".query[Author]

    def findByFirstNameAndLastNameSql(author: AuthorWithoutId): Query0[Author] =
      sql"select * from authors where first_name = ${author.firstName.value} AND last_name = ${author.lastName.value}".query[Author]

    def createAuthorSql(author: AuthorWithoutId): Update0 =
      sql"insert into authors(first_name, last_name) values(${author.firstName.value}, ${author.lastName.value})".update

    def updateAuthorSql(author: Author): Update0 =
      sql"update authors set first_name = ${author.firstName.value}, last_name = ${author.lastName.value} where id = ${author.id.value}".update

    def removeByIdSql(id: Id): Update0 =
      sql"delete from authors where id = ${id.value}".update
  }

  private final class Impl extends AuthorSql {
    import scripts._

    override def findPage(limit: Int, offset: Long): ConnectionIO[List[Author]] =
      findPageSql(limit, offset).to[List]

    override def findById(id: Id): ConnectionIO[Option[Author]] =
      findByIdSql(id).option

    override def findByFirstNameAndLastName(author: AuthorWithoutId): ConnectionIO[Option[Author]] =
      findByFirstNameAndLastNameSql(author).option

    override def createAuthor(author: AuthorWithoutId): ConnectionIO[Either[DataAlreadyExists, Author]] =
      createAuthorSql(author)
        .withUniqueGeneratedKeys[Id]("id")
        .attemptSomeSqlState {
          case UNIQUE_VIOLATION => DataAlreadyExists()
        }
        .map {
          case Left(error) => error.asLeft[Author]
          case Right(id) => Author(id, author.firstName, author.lastName).asRight[DataAlreadyExists]
        }

    override def updateAuthor(author: Author): ConnectionIO[Either[DataNotFound, Author]] =
      updateAuthorSql(author)
        .run
        .attemptSomeSqlState {
          case UNIQUE_VIOLATION => DataAlreadyExists()
        }
        .map {
          case Left(_) => DataNotFound(author.id).asLeft[Author]
          case Right(_) => author.asRight[DataNotFound]
        }

    override def removeById(id: Id): ConnectionIO[Either[DataNotFound, Unit]] =
      removeByIdSql(id).run
        .map {
          case 0 => DataNotFound(id).asLeft[Unit]
          case _ => ().asRight[DataNotFound]
        }
  }

  def make: AuthorSql = new Impl
}
