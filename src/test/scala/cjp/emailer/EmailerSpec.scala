package cjp.emailer

import cats.effect.IO
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.domain.core.email.{Email, EmailRepository}

import cats.effect.unsafe.implicits.global

class EmailerSpec extends Specification with Mockito {
  val PROVISIONAL_ACCEPTANCE = "PROVISIONAL_ACCEPTANCE"

  trait Context extends Scope {
    val emailRepository = mock[EmailRepository]

    var emailsSent :List[Email] = List()
    def senderFunc(email :Email) :IO[EmailSentResult] = {
      emailsSent = emailsSent ++ List[Email](email)
      IO.delay(Sent)
    }

    val emailer = new Emailer(emailRepository, senderFunc)
  }

  "Emailer" should {
    "sendEmails zero emails if no emails in queue" in new Context {
      emailRepository.findByStatus(STATUS_WAITING) returns List()

      val result = emailer.sendEmails().unsafeRunSync()

      emailsSent.length mustEqual 0
      there was no(emailRepository).updateStatus(anyString, any)
      result mustEqual Right(List())
    }

    "sendEmails should send an email and set the status to sent for all emails in the queue" in new Context {
      val emailObj1 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob",
        subject = "subject",
        text = "text",
        html = "<html>data<html>",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE
      )

      val emailObj2 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob",
        subject = "subject",
        text = "text",
        html = "<html>data<html>",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE
      )

      val emailList = List(emailObj1, emailObj2)
      emailRepository.findByStatus(STATUS_WAITING) returns emailList

      val result = emailer.sendEmails().unsafeRunSync()

      emailsSent.length mustEqual 2
      there were two(emailRepository).updateStatus(anyString, any)
      result mustEqual Right(List((emailObj1, Sent), (emailObj2, Sent)))
    }
  }
}
