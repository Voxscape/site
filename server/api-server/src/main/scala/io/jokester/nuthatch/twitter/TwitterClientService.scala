package io.jokester.nuthatch.twitter

import cats.effect.IO
import com.github.scribejava.core.model.OAuth1AccessToken
import com.typesafe.scalalogging.LazyLogging
import io.jokester.nuthatch.base.AppContext
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken

case class TwitterClientService(apiCtx: AppContext, accessToken: OAuth1AccessToken)
    extends LazyLogging {

  private lazy val client = new TwitterFactory().getInstance(
    new AccessToken(accessToken.getToken, accessToken.getTokenSecret),
  )

  def fetchFollowers(): IO[Unit] = {
    IO {
      val ownId     = client.getId
      val followers = client.getFollowersList(ownId, 0)

      logger.debug("got followers {}", followers)
    }
  }

}