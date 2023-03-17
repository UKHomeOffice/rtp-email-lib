package cjp.emailer

import grizzled.slf4j.Logging
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.domain.core.email.{Email, EmailRepository}
import scala.util.{Try, Success, Failure}

class Emailer(emailRepository: EmailRepository, emailSender :Email => EmailSentResult) extends Logging {
  private val emailType = "WAITING_CUSTOMER_EMAILS"

  def sendEmails() :Either[String, List[(Email, String)]] = try {
    val emailsToSend = emailRepository.findByStatus(STATUS_WAITING)

    val results = emailsToSend.map { email =>
      val newStatus = sendEmail(email)
      (email, newStatus)
    }

    Right(results)

  } catch {
    case e: Exception =>
      logger.error(s"Exception caught in sendEmails loop: ${e.getMessage}")
      Left(e.getMessage)
  }

  def sendEmail(email: Email) :String = {
    logger.info(s"Sending email to ${email.recipient}")

    Try(emailSender(email)) match {
      case Success(Sent) =>
        logger.info("Marking email as sent")
        emailRepository.updateStatus(email.emailId, STATUS_SENT)
        STATUS_SENT

      case Success(Waiting) =>
        logger.info("Marking not sent")
        emailRepository.updateStatus(email.emailId, STATUS_WAITING)
        STATUS_WAITING

      case Success(TransientError(err)) =>
        logger.error(s"Error sending email: $err")
        emailRepository.updateStatus(email.emailId, STATUS_WAITING)
        STATUS_WAITING

      case Success(EmailAddressError(err)) =>
        logger.error(s"Error with email address: $err")
        emailRepository.updateStatus(email.emailId, STATUS_EMAIL_ADDRESS_ERROR)
        STATUS_EMAIL_ADDRESS_ERROR

      case Failure(exception) =>
        /* we assume errors can be recovered from but this runs the risk of emails being spammed in a loop */
        logger.error(exception.getMessage, exception)
        STATUS_WAITING
    }
  }

}
