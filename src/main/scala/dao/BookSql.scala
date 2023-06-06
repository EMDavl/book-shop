package dao

import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}
import domain.errors.{CommonError, DataAlreadyExists, DataNotFound}
import domain._
import doobie._
import doobie.implicits._

trait BookSql {
  def findPage(limit: Int, offset: Long): ConnectionIO[List[Book]]

  def findById(id: Id): ConnectionIO[Option[Book]]

  def findAllByAuthorId(authorId: Id): ConnectionIO[List[Book]]

  def update(book: Book): ConnectionIO[Either[CommonError, Book]]

  def removeById(id: Id): ConnectionIO[Either[DataNotFound, Unit]]

  def create(book: BookWithoutId): ConnectionIO[Either[CommonError, Book]]
}

object BookSql {

  object scripts {
    def findPageSql(limit: Int, offset: Long): Query0[Book] =
      sql"SELECT * FROM books b WHERE b.id > $offset ORDER BY b.id limit $limit".query[Book]

    def findByIdSql(id: Id): Query0[Book] =
      sql"SELECT * FROM books b WHERE b.id = ${id.value}".query[Book]

    def findAllByAuthorIdSql(id: Id): Query0[Book] =
      sql"SELECT * FROM books b b.author_id = ${id.value}".query[Book]

    def existsByAuthorIdAndNameAndPublishDate(book: BookWithoutId): Query0[Int] =
      sql"SELECT 1 FROM books b WHERE b.author_id = ${book.authorId.value} AND lower(b.name) = lower(${book.name.value}) AND b.publish_date = ${book.publishDate.value.toEpochMilli}".query[Int]

    def updateSql(book: Book): Update0 =
      sql"""
      UPDATE books SET
                      name = ${book.name.value},
                      author_id = ${book.authorId.value},
                      publish_date = ${book.publishDate.value.toEpochMilli}
                  WHERE id = ${book.id.value}
      """.update

    def removeByIdSql(id: Id): Update0 =
      sql"DELETE FROM books WHERE id = ${id.value}".update

    def createSql(book: BookWithoutId): Update0 =
      sql"INSERT INTO books(name, author_id, publish_date) VALUES(${book.name.value}, ${book.authorId.value}, ${book.publishDate.value.toEpochMilli})".update
  }

  private final class Impl(authorSql: AuthorSql) extends BookSql {

    import scripts._

    override def findPage(limit: Int, offset: Long): ConnectionIO[List[Book]] = {
      findPageSql(limit, offset).to[List]
    }

    override def findById(id: Id): ConnectionIO[Option[Book]] = {
      findByIdSql(id).option
    }

    override def findAllByAuthorId(authorId: Id): ConnectionIO[List[Book]] = {
      findAllByAuthorIdSql(authorId).to[List]
    }

    override def update(book: Book): ConnectionIO[Either[CommonError, Book]] = {
      authorSql.findById(book.authorId).flatMap {
        case None => (DataNotFound(book.authorId): CommonError).asLeft[Book].pure[ConnectionIO]
        case Some(_) => existsByAuthorIdAndNameAndPublishDate(BookWithoutId(book.name, book.authorId, book.publishDate))
          .option
          .flatMap {
            case None =>
              findByIdSql(book.id)
                .option
                .flatMap {
                  case None => (DataNotFound(book.id): CommonError).asLeft[Book].pure[ConnectionIO]
                  case Some(fetchedBook) => updateSql(book).run.map {
                    case 1 => fetchedBook.asRight[CommonError]
                  }: ConnectionIO[Either[CommonError, Book]]
                }
            case Some(_) => (DataAlreadyExists(): CommonError).asLeft[Book].pure[ConnectionIO]
          }
      }
    }

    override def removeById(id: Id): ConnectionIO[Either[DataNotFound, Unit]] = {
      removeByIdSql(id).run.map {
        case 1 => ().asRight[DataNotFound]
        case _ => DataNotFound(id).asLeft[Unit]
      }
    }

    override def create(book: BookWithoutId): ConnectionIO[Either[CommonError, Book]] = {
      authorSql.findById(book.authorId).flatMap {
        case None => (DataNotFound(book.authorId): CommonError).asLeft[Book].pure[ConnectionIO]
        case Some(_) => existsByAuthorIdAndNameAndPublishDate(book)
          .option
          .flatMap {
            case None =>
              createSql(book)
                .withUniqueGeneratedKeys[Id]("id")
                .map(id => Book(id, book.name, book.authorId, book.publishDate).asRight[DataAlreadyExists])
            case Some(_) => (DataAlreadyExists(): CommonError).asLeft[Book].pure[ConnectionIO]
          }
      }
    }
  }
  def make(authorSql: AuthorSql): BookSql = new Impl(authorSql)
}
