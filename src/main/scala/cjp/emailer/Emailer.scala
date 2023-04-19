package cjp.emailer

import cats._
import cats.implicits._
import cats.effect.IO
import grizzled.slf4j.Logging
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.domain.core.email.{Email, EmailRepository}
import scala.util.{Try, Success, Failure}

class Emailer(emailRepository: EmailRepository, emailSender :Email => IO[EmailSentResult]) extends Logging {

  def sendEmails() :IO[Either[String, List[(Email, EmailSentResult)]]] = try {
    val emailsToSend = emailRepository.findByStatus(STATUS_WAITING)

    val results = emailsToSend.map { email =>
      sendEmail(email).map { newStatus => (email, newStatus) }
    }.sequence

    results.map(Right(_))

  } catch {
    case e: Exception =>
      logger.error(s"Exception caught in sendEmails loop: ${e.getMessage}")
      IO.delay(Left(e.getMessage))
  }

  def sendEmail(email: Email) :IO[EmailSentResult] = {
    logger.info(s"Sending email to ${email.recipient}")

    emailSender(email).map {
      case Sent(newText, newHtml) =>
        logger.info("Marking email as sent")
        emailRepository.updateStatus(email.emailId, STATUS_SENT, newText, newHtml)
        Sent(newText, newHtml)

      case Waiting =>
        logger.info("Marking not sent")
        emailRepository.updateStatus(email.emailId, STATUS_WAITING)
        Waiting

      case TransientError(err) =>
        logger.error(s"Error sending email: $err")
        emailRepository.updateStatus(email.emailId, STATUS_WAITING)
        TransientError(err)

      case EmailAddressError(err) =>
        logger.error(s"Error with email address: $err")
        emailRepository.updateStatus(email.emailId, STATUS_EMAIL_ADDRESS_ERROR)
        EmailAddressError(err)
    }
  }

}
