package cjp.emailer

import org.apache.commons.mail.EmailException
import grizzled.slf4j.Logging
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.domain.core.email.{Email, EmailRepository}

class Emailer(emailRepository: EmailRepository, emailSender: EmailSender, sender: EmailAddress, replyTo: Option[EmailAddress] = None) extends Logging {
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
      logger.error(e.getMessage)
      Left(e.getMessage)
  }

  def sendEmail(email: Email) = try {
    logger.info(s"Sending email to ${email.recipient}")
    emailSender.sendMessage(sender = sender, recipient = email.recipient, ccList = email.cc, subject = email.subject, message = email.text, html = Some(email.html), replyTo = replyTo)
    logger.info("Marking email as sent")
    emailRepository.updateStatus(email.emailId, STATUS_SENT)
    STATUS_SENT
  } catch {
    case e: EmailException =>
      logger.error(e.getMessage, e)
      emailRepository.updateStatus(email.emailId, STATUS_EMAIL_ADDRESS_ERROR)
      STATUS_EMAIL_ADDRESS_ERROR

    case e: Exception =>
      logger.error(e.getMessage, e)
      STATUS_WAITING
  }

}
