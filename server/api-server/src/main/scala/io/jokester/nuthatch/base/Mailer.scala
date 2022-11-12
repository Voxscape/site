package io.jokester.nuthatch.base

import cats.effect.IO
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

case class MailIntent(recipient: String, title: String, body: String) {}

trait Mailer {
  def send(intent: MailIntent): IO[Unit]

  def send(recipient: String, title: String, body: String): IO[Unit] = send(
    MailIntent(recipient, title, body),
  )
}

object Mailer {
  def get(_config: Config): Mailer = DummyMailer
}

object DummyMailer extends Mailer with LazyLogging {
  override def send(intent: MailIntent): IO[Unit] = {
    IO {
      logger.info("recipient: {}", intent.recipient)
      logger.info("title: {}", intent.title)
      logger.info("body: {}", intent.body)
    }
  }
}
