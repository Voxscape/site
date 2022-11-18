package io.jokester.nuthatch.authn

import io.getquill._
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import io.jokester.nuthatch.base.{AppContextBase, QuillFactory, QuillJsonHelper}
import io.jokester.nuthatch.generated.quill.{public => T}

private[authn] trait BaseAuth extends LazyLogging with QuillJsonHelper {
  protected def appCtx: AppContextBase

  protected val quill: QuillFactory.RdbContext = appCtx.quill

  def findUserByEmail(email: String): IO[Option[UserAuthBundle]] = {
    val findIdByEmail: IO[Option[Int]] = IO.blocking {
      import quill._
      val users: Seq[T.User] = run {
        query[T.User]
          .filter(_.email == lift(email.toLowerCase))
      }
      users.headOption.map(_.id)
    }
    findIdByEmail.flatMap({
      case Some(userId) => findUserById(userId)
      case _            => IO.pure(None)
    })
  }

  def findUserById(userId: Int): IO[Option[UserAuthBundle]] = IO.blocking {
    import quill._
    val load = quote {
      query[T.User]
        .filter(_.id == lift(userId))
        .leftJoin(query[T.UserOauth1])
        .on((user, userOauth) => user.id == userOauth.userId)
        .leftJoin(query[T.UserPassword])
        .on((userAndUserOAuth, userPassword) => userAndUserOAuth._1.id == userPassword.userId)
    }
    val loaded: Seq[(T.User, Option[T.UserOauth1], Option[T.UserPassword])] =
      run(load).map(row => (row._1._1, row._1._2, row._2))

    loaded.headOption match {
      case Some(row) =>
        Some(UserAuthBundle(row._1, userOAuth1 = loaded.flatMap(_._2), userPassword = row._3))
      case _ => None
    }
  }

  def findUserByOAuth(provider: String, externalId: String): IO[Option[UserAuthBundle]] = {

    val search: IO[Option[T.User]] = IO.blocking {
      import quill._

      val matched: Seq[(T.UserOauth1, T.User)] = run(quote {
        query[T.UserOauth1]
          .filter(r => r.provider == lift(provider) && r.providerId == lift(externalId))
          .join(query[T.User])
          .on((oauth, u) => oauth.userId == u.id)
      })
      matched.headOption.map(_._2)
    }
    search.flatMap({
      case Some(found) => findUserById(found.id)
      case _           => IO.pure(None)
    })

  }

  def upsertOAuthUser(
      oauthMatch: Option[UserAuthBundle],
      emailMatch: Option[UserAuthBundle],
      initialUser: T.User,
      initialOauth: T.UserOauth1,
  ): IO[Int] = {
    IO.blocking({
      import quill._

      val insertUser = quote {
        query[T.User]
          .insert(_.email -> lift(initialUser.email), _.profile -> lift(initialUser.profile))
          .returning(u => u.id)
      }
      val insertOAuth = (userId: Int) =>
        quote {
          query[T.UserOauth1]
            .insert(
              _.userId            -> lift(userId),
              _.provider          -> lift(initialOauth.provider),
              _.providerId        -> lift(initialOauth.providerId),
              _.providerProfile   -> lift(initialOauth.providerProfile),
              _.accessToken       -> lift(initialOauth.accessToken),
              _.accessTokenSecret -> lift(initialOauth.accessTokenSecret),
            )
        }

      val updateOAuth = (existed: T.UserOauth1, patch: T.UserOauth1) =>
        quote {
          query[T.UserOauth1]
            .filter(_.id == lift(existed.id))
            .update(
              _.providerProfile   -> lift(patch.providerProfile),
              _.accessToken       -> lift(patch.accessToken),
              _.accessTokenSecret -> lift(patch.accessTokenSecret),
            )
        }

      val userId: Int = (oauthMatch, emailMatch) match {
        // existing user with email + OAuth profile: update the OAuth part
        case (Some(m1), Some(m2)) if m1.user.id == m2.user.id =>
          run(updateOAuth(m1.findByProvider(initialOauth.provider).get, initialOauth))
          m1.user.id
        // existed user with only OAuth profile
        case (Some(m1), None) =>
          run(updateOAuth(m1.findByProvider(initialOauth.provider).get, initialOauth))
          m1.user.id
        // existed user without oauth profile: create the OAuth part
        case (None, Some(m2)) =>
          run(insertOAuth(m2.user.id))
          m2.user.id
        // new user
        case (None, None) =>
          transaction {
            val newUserId = run(insertUser)
            run(insertOAuth(newUserId))
            newUserId
          }
        //
        case _ => throw new AssertionError("user profile mismatch")
      }
      userId
    })
  }
}
