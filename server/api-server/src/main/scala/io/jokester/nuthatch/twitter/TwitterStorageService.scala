package io.jokester.nuthatch.twitter

import cats.effect.IO
import io.circe.Json
import io.jokester.nuthatch.base.AppContextBase
import io.jokester.nuthatch.generated.quill.{public => T}
import twitter4j.v1.{User => TwitterUser}
import io.getquill._

class TwitterStorageService(ctx: AppContextBase) {
  private case class NewTwitterUser(twitterId: Long, twitterProfile: Json)

  def upsertUsers(users: Seq[TwitterUser]): IO[Int] = {
    if (users.isEmpty) {
      return IO.pure(0)
    }

    IO {
      import ctx.quill._

      val insert = quote {
        liftQuery(users.map(u => NewTwitterUser(u.getId, ctx.quill.toJson(u))))
          .foreach(u =>
            query[T.TwitterUser]
              .insert(
                _.twitterUserId      -> u.twitterId,
                _.twitterUserProfile -> u.twitterProfile,
              )
              .onConflictUpdate(_.twitterUserId)((t, e) =>
                t.twitterUserProfile -> e.twitterUserProfile,
              ),
          )

      }

      val inserted = run(insert)

      inserted.length
    }
  }

  def upsertFollowers(followee: Long, followers: Seq[Long]): IO[Unit] = {
    if (followers.isEmpty) {
      return IO.pure(())
    }

    IO {
      import ctx.quill._

      val clean = quote {
        query[T.TwitterFriendship]
          .filter(_.twitterUserId2 == lift(followee))
          .update(_.following -> false)
      }

      val upsert = quote {
        liftQuery(followers).foreach(follower => {
          query[T.TwitterFriendship]
            .insert(
              _.following      -> true,
              _.twitterUserId1 -> follower,
              _.twitterUserId2 -> lift(followee),
            )
            .onConflictUpdate(_.twitterUserId1, _.twitterUserId2)((t, e) =>
              t.following -> e.following,
            )
        })
      }

      transaction {
        run(clean)
        run(upsert)
      }
    }
  }

  def upsertFollowees(follower: Long, followees: Seq[Long]): IO[Unit] = {
    if (followees.isEmpty) {
      return IO.pure(())
    }

    IO {
      import ctx.quill._

      val clean = quote {
        query[T.TwitterFriendship]
          .filter(_.twitterUserId1 == lift(follower))
          .update(_.following -> false)
      }

      val upsert = quote {
        liftQuery(followees).foreach(followee => {
          query[T.TwitterFriendship]
            .insert(
              _.following      -> true,
              _.twitterUserId1 -> lift(follower),
              _.twitterUserId2 -> followee,
            )
            .onConflictUpdate(_.twitterUserId1, _.twitterUserId2)((t, e) =>
              t.following -> e.following,
            )
        })
      }

      transaction {
        run(clean)
        run(upsert)
      }
    }

  }
}
