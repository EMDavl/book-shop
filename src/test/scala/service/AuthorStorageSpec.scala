package service

import dao.AuthorSql
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import cats.data.Reader
import domain.errors.CommonError
import domain.{Author, AuthorFirstName, AuthorLastName, IOWithRequestContext, Id}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId}

class AuthorStorageSpec extends AnyWordSpec with Matchers with MockitoSugar {
  trait Fixture {
    val mockAuthorSql: AuthorSql = mock[AuthorSql]
    val mockTransactor: Transactor[IOWithRequestContext] = null // Mock Transactor[IO] if necessary
    val authorStorage: AuthorStorage = AuthorStorage.make(mockAuthorSql, mockTransactor)
  }

  "findPage" should {
    "return a list of authors for valid page parameters" in new Fixture {
      val limit = 10
      val offset = 0
      val expectedAuthors = List(Author(Id(1), AuthorFirstName("John"), AuthorLastName("Doe")))

      when(mockAuthorSql.findPage(limit, offset)).thenReturn(expectedAuthors.asLeft[CommonError].pure(ConnectionIO))
      val result = authorStorage.findPage(limit, offset)

      result.unsafeToFuture().map { response =>
        response shouldBe Right(expectedAuthors)
      }
    }

    "return an error for invalid page limit" in new Fixture {
      val limit = 0
      val offset = 0

      val result = authorStorage.findPage(limit, offset)

      result.unsafeToFuture().map { response =>
        response shouldBe Left(BadRequest("Limit must be greater than 0"))
      }
    }
  }

  "findAuthorById" should {
    "return an author for a valid ID" in new Fixture {
      val authorId = 1
      val expectedAuthor = Some(Author(id = authorId, firstName = "John", lastName = "Doe"))

      when(mockAuthorSql.findById(authorId)).thenReturn(Reader(_ => IO.pure(expectedAuthor)))

      val result = authorStorage.findAuthorById(authorId)

      result.unsafeToFuture().map { response =>
        response shouldBe Right(expectedAuthor)
      }
    }

    "return None for an invalid ID" in new Fixture {
      val authorId = 0

      when(mockAuthorSql.findById(authorId)).thenReturn(Reader(_ => IO.pure(None)))

      val result = authorStorage.findAuthorById(authorId)

      result.unsafeToFuture().map { response =>
        response shouldBe Right(None)
      }
    }
  }
