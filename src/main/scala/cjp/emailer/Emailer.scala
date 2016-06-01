package cjp.emailer

import org.apache.commons.mail.EmailException
import grizzled.slf4j.Logging
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.domain.core.email.{Email, EmailRepository}
import uk.gov.homeoffice.domain.core.lock.{ProcessLockRepository, ProcessLocking}

class Emailer(emailRepository: EmailRepository, emailSender: EmailSender, sender: EmailAddress, replyTo: Option[EmailAddress] = None, pollingFrequency: Int, override val processLockRepository: ProcessLockRepository) extends ProcessLocking with Logging {
  private val emailType = "WAITING_CUSTOMER_EMAILS"

  def sendEmails() = try {
    val emailsToSend = emailRepository.findByStatus(STATUS_WAITING)

    emailsToSend.foreach(sendEmail)
  } catch {
    case e: Exception => logger.error(e.getMessage)
  }

  def sendEmail(email: Email) = {
    try {
      logger.info(s"Sending email to ${email.recipient}")
      emailSender.sendMessage(sender = sender, recipient = email.recipient, ccList = email.cc, subject = email.subject, message = email.text, html = Some(email.html), replyTo = replyTo)
      logger.info("Marking email as sent")
      emailRepository.updateStatus(email.emailId, STATUS_SENT)
    } catch {
      case e: EmailException =>
        logger.error(e.getMessage, e)
        emailRepository.updateStatus(email.emailId, STATUS_EMAIL_ADDRESS_ERROR)

      case e: Exception =>
        logger.error(e.getMessage, e)
    }
  }

  def start() = while (true) {
    withLock(emailType) {
      sendEmails()
    }

    logger.info("Polling for new emails")
    Thread.sleep(pollingFrequency * 1000)
  }
}