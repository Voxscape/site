package io.jokester.nuthatch.authn

import cats.effect.IO
import io.jokester.api.OpenAPIConvention
import io.jokester.api.OpenAPIConvention.BadRequest
import io.jokester.nuthatch.authn.AuthenticationApi.Password
import io.jokester.nuthatch.generated.quill.{public => T}
import org.http4s.Query
import org.springframework.security.crypto.bcrypt.BCrypt
import redis.clients.jedis.params.SetParams
import io.getquill._

import java.util.UUID

private[authn] trait EmailSignup {
  self: BaseAuth =>

  def emailPasswordSignup(
      cred: AuthenticationApi.Password.EmailSignUpRequest,
  ): IO[UserAuthBundle] = {
    for (
      emailMatched <- findUserByEmail(cred.email);
      created      <- upsertUser(cred, emailMatched)
    ) yield created
  }

  // TODO
  def confirmEmail(
      req: AuthenticationApi.Password.EmailConfirmRequest,
  ): IO[UserAuthBundle] = ???

  private def upsertUser(
      cred: Password.EmailSignUpRequest,
      emailMatched: Option[UserAuthBundle],
  ): IO[UserAuthBundle] = {
    emailMatched match {
      case Some(existed) if !existed.hasPassword =>
        IO.raiseError(
          OpenAPIConvention.BadRequest(
            """
              |A user existed but password auth is not set up.
              |Try reset password if you are the owner.
              |""".stripMargin,
          ),
        )
      case Some(existed) =>
        IO.raiseError(OpenAPIConvention.BadRequest("A user already existed.\nTry login."))
      case Some(existed) =>
        // TODO: when we have isEmailVerified in DB: allow unverified user to signup again
        IO.raiseError(
          OpenAPIConvention
            .BadRequest("A user already existed.\nTry login or reset password."),
        )
      case None =>
        insertEmailPasswordUser(cred)
          .flatMap(userId => findUserById(userId).map(_.get))
          .flatTap(inserted => sendActivationMail(inserted))
    }
  }

  def requestActivationMail(u: UserAuthBundle): IO[Boolean] = {
    if (!u.hasEmail) {
      return IO.raiseError(BadRequest("email not set"))
    }
    // TODO: prevent isEmailVerified user from requesting more mail

    sendActivationMail(u)
  }

  private def sendActivationMail(u: UserAuthBundle): IO[Boolean] = {
    val issueUrl = appCtx.redis.use(jedis =>
      IO {
        val nonce         = UUID.randomUUID().toString
        val activationUrl = appCtx.web.userActivationPage(Query("t" -> Some(nonce)))
        jedis.set(
          appCtx.redisKeys.authn.userActivationLink(nonce),
          u.user.id.toString,
          SetParams.setParams().ex(3600 * 24),
        )
        jedis.set(
          appCtx.redisKeys.authn.userActivationLinkIssued(u.user.id),
          "",
          SetParams.setParams().ex(1800),
        )
        activationUrl
      },
    )
    val sendMail = (url: String) => {
      appCtx.providers.mailer.send(
        u.user.email,
        "Voxscape: account activation",
        s"""
           |Thanks for using Voxscape. Please activate using the following link:
           |
           |  $url
           |
           |By creating your account you agree to our user agreement and private policy:
           |User agreement: TODO
           |Private policy: TODO
           |""".stripMargin,
      )
    }

    for (
      canSend <- checkActivationMailInterval(u);
      sent <- {
        if (canSend) {
          issueUrl.flatMap(sendMail).map(_ => true)
        } else {
          IO.raiseError(OpenAPIConvention.BadRequest("""
              |Activation request too frequent.
              |Please try again in 1 hour.
              |""".stripMargin))
        }
      }
    ) yield sent
  }

  private def checkActivationMailInterval(u: UserAuthBundle): IO[Boolean] = {
    appCtx.redis.use(jedis =>
      IO {
        val key = appCtx.redisKeys.authn.userActivationLinkIssued(u.user.id)
        val f   = jedis.get(key)
        Some(f).isEmpty
      },
    )
  }

  private def consumeActivationUrl(token: String): IO[Boolean] = {
    None.asInstanceOf[Nothing]
  }

  private def insertEmailPasswordUser(
      cred: AuthenticationApi.Password.EmailSignUpRequest,
  ): IO[Int] = IO {
    import quill._

    val passwordHash = BCrypt.hashpw(cred.initialPassword, BCrypt.gensalt())
    val profileJson  = cred.initialProfile.map(_.toJson)
    val insertUser = quote {
      query[T.User]
        .insert(_.email -> lift(cred.email), _.profile -> lift(profileJson))
        .returning(u => u.id)
    }
    val insertPasswordAuth = (userId: Int) =>
      quote {
        query[T.UserPassword].insert(_.userId -> lift(userId), _.passwordHash -> lift(passwordHash))
      }
    val newUserId = transaction {
      val userId = run(insertUser)
      run(insertPasswordAuth(userId))
      userId
    }

    newUserId
  }
}
