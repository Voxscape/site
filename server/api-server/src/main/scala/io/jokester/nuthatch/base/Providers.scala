package io.jokester.nuthatch.base

import cats.effect.IO
import com.typesafe.config.Config
import twitter4j.{OAuth2Authorization, OAuth2Token, Twitter}

/** external service
  */
private[base] trait Providers {
  protected def rootConfig: Config

  def mailer: Mailer

  protected trait TwitterProvider {
    def config: Config = rootConfig.getConfig("twitter")

    def fetchAppOAuth2Token: IO[OAuth2Token] = {
      val auth = OAuth2Authorization.getInstance(
        config.getConfig("oauth1").getString("consumer_key"),
        config.getConfig("oauth1").getString("consumer_secret"),
      )
      IO(auth.getOAuth2Token)
    }

    def buildAppAuthedClient(): Twitter = {
      Twitter
        .newBuilder()
        .applicationOnlyAuthEnabled(true)
        .oAuthConsumer(
          config.getConfig("oauth1").getString("consumer_key"),
          config.getConfig("oauth1").getString("consumer_secret"),
        )
        .oAuth2Token("bearer", config.getConfig("oauth2").getString("bearer_token"))
        .build()
    }

    /** user-authed twitter client
      */
    def buildOAuth1AuthClient(oauthAccessToken: String, oAuthTokenSecret: String): Twitter = {
      Twitter
        .newBuilder()
        .oAuthConsumer(
          config.getConfig("oauth1").getString("consumer_key"),
          config.getConfig("oauth1").getString("consumer_secret"),
        )
        .oAuthAccessToken(oauthAccessToken, oAuthTokenSecret)
        .build()
    }

  }

  def twitter: TwitterProvider
}
