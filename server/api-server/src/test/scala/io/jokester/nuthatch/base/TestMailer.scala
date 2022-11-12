package io.jokester.nuthatch.base

import cats.effect.IO

import java.util.concurrent.atomic.AtomicReference

class TestMailer() extends Mailer {
  private val _sent = new AtomicReference(List.empty[MailIntent])

  def sentMails: List[MailIntent] = _sent.get()

  def cleanMails(): List[MailIntent] = _sent.getAndSet(List.empty)

  override def send(intent: MailIntent): IO[Unit] = IO {
    _sent.getAndUpdate(_ :+ intent)
  }
}
