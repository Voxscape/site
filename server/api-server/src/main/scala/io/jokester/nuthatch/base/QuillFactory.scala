package io.jokester.nuthatch.base

import cats.effect.IO
import cats.effect.kernel.Resource
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariDataSource
import io.circe.{Json, JsonObject}
import io.getquill.{PostgresDialect, PostgresJdbcContext, Query}
import io.jokester.nuthatch.generated.quill.public.PublicExtensions
import io.jokester.nuthatch.generated.quill.{public => T}
import io.jokester.quill.{FixedPostgresNaming, QuillCirceJsonEncoding, QuillDataSource}
import io.getquill._

import java.io.Closeable
import java.time.OffsetDateTime
import javax.sql.DataSource

object QuillFactory {

  class RdbContext(dataSource: DataSource with Closeable)
      extends PostgresJdbcContext(FixedPostgresNaming, dataSource)
      with PublicExtensions[PostgresDialect, FixedPostgresNaming.type]
      with QuillCirceJsonEncoding
      with QuillJsonHelper
      with PublicCtxDummyFactory {

    import cats.effect.{IO => CatsIO}

    def testConnection(): CatsIO[Boolean] = {
      CatsIO {
        val q = quote {
          sql"SELECT 1".as[Query[Int]]
        }
        run(q).headOption.contains(1)
      }
    }
  }

  def createNonPooledCtx(c: Config): Resource[IO, RdbContext] = {
    val simpleDataSource = QuillDataSource.simplePgDataSource(c.getString("url"))
    Resource.make(
      IO.blocking(new RdbContext(simpleDataSource)),
    )(ctx =>
      IO.blocking({
        ctx.close()
      }),
    )
  }

  def unsafeCreateNonPolledCtx(c: Config): (AutoCloseable, RdbContext) = {
    val simpleDataSource = QuillDataSource.simplePgDataSource(c.getString("url"))
    val ctx              = new RdbContext(simpleDataSource)
    (simpleDataSource, ctx)
  }

  def unsafeCreatePooledQuill(c: Config): (HikariDataSource, RdbContext) = {
    val simpleDataSource = QuillDataSource.simplePgDataSource(c.getString("url"))
    val hikariDataSource = QuillDataSource.pooled(simpleDataSource)

    val ctx = new RdbContext(hikariDataSource)
    (hikariDataSource, ctx)
  }

  trait PublicCtxDummyFactory {
    val dummyUser: T.User = T.User(
      id = -1,
      email = "CHANGE_ME",
      profile = None,
      createdAt = OffsetDateTime.MIN,
      updatedAt = OffsetDateTime.MIN,
    )
    val dummyOAuth1: T.UserOauth1 = T.UserOauth1(
      id = -2,
      userId = -1,
      provider = "DUMMY",
      accessToken = "DUMMY",
      accessTokenSecret = "DUMMY",
      providerId = "DUMMY",
      providerProfile = Json.fromJsonObject(JsonObject.empty),
      createdAt = OffsetDateTime.MIN,
      updatedAt = OffsetDateTime.MIN,
    )

  }
}
