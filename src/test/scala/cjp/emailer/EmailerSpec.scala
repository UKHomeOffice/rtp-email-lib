package cjp.emailer

import domain.core.email.EmailStatus._
import domain.core.email._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.specs2.matcher.MustMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.domain.core.email.EmailRepository
import uk.gov.homeoffice.domain.core.lock.ProcessLockRepository

class EmailerSpec extends Specification with MustMatchers with Mockito {

  sequential

  val emailRepository = mock[EmailRepository]
  val processLockRepository = mock[ProcessLockRepository]
  val emailSender = mock[EmailSender]
  val sender = EmailAddress("", "")
  val replyTo = EmailAddress("", "")
  val emailer = new Emailer(emailRepository, emailSender, sender, Some(replyTo), 5, processLockRepository)
  val PROVISIONAL_ACCEPTANCE = "PROVISIONAL_ACCEPTANCE"

  "Emailer" should {

    "sendEmails zero emails if no emails in queue" in {

      when(emailRepository.findByStatus(STATUS_WAITING)).thenReturn(List())

      emailer.sendEmails()

      there was no(emailSender).sendMessage(any, anyString, any[List[String]], anyString, anyString, any, any, any)
      there was no(emailRepository).updateStatus(anyString, any)
    }

    "sendEmails should send an email and set the status to sent for all emails in the queue" in {

      val emailObj1 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob",
        subject = "subject",
        text = "text",
        html = "<html>data<html>",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE)
      val emailObj2 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob",
        subject = "subject",
        text = "text",
        html = "<html>data<html>",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE)
      val emailList = List(emailObj1, emailObj2)
      when(emailRepository.findByStatus(STATUS_WAITING)).thenReturn(emailList)

      emailer.sendEmails()

      there were two(emailSender).sendMessage(any, anyString, any[List[String]], anyString, any, any, any, any)
      there were two(emailRepository).updateStatus(anyString, any)
    }
  }
}
