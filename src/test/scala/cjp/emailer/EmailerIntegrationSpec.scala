package cjp.emailer

import java.io.{FileWriter, InputStream}
import java.nio.file.Files
import java.util.Scanner
import javax.mail.util.SharedByteArrayInputStream

import domain.core.email.EmailStatus._
import domain.core.email._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import uk.gov.homeoffice.domain.core.email.EmailRepository
import uk.gov.homeoffice.domain.core.lock.ProcessLockRepository
import uk.gov.homeoffice.mongo.casbah.MongoSpecification

class EmailerIntegrationSpec extends Specification with MongoSpecification with GreenMailHelper {

  sequential

  val sender = EmailAddress("jonny.cavell@gmail.com", "Jonny Cavell")
  val replyTo = EmailAddress("replyto@test.com", "Reply To")
  val emailRepository = new EmailRepository with TestMongo
  val processLockRepository = new ProcessLockRepository with TestMongo

  val PROVISIONAL_ACCEPTANCE = "PROVISIONAL_ACCEPTANCE"

  "Sending an email message via the Emailer" should {

    "result in that message with invalid email not ending up in the GreenMail message queue" in {
      val emailSender = new EmailSender(GreenMailHelper.smtpConfig)
      val emailer = new Emailer(emailRepository, emailSender, sender, Some(replyTo), 5, processLockRepository)

      val emailObj1 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob@bob.com",
        subject = "subject",
        text = "text",
        html = "data",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE)

      emailRepository.insert(emailObj1)

      emailer.sendEmails()

      // Longer time needed for virtual environments with less resources
      Thread.sleep(1000)

      GreenMailHelper.getReceivedMessages.size mustEqual 1
    }

    "result in that message with email ending up in the GreenMail message queue" in {

      val emailSender = new EmailSender(GreenMailHelper.smtpConfig)
      val emailer = new Emailer(emailRepository, emailSender, sender, Some(replyTo), 5, processLockRepository)

      val emailObj1 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob@bob.com",
        subject = "subject",
        text = "text",
        html = "data",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE)

      val emailObj2 = Email(
        caseId = Some(new ObjectId().toString),
        caseRef = Some(""),
        date = new DateTime(),
        recipient = "bob@",
        subject = "subject",
        text = "text",
        html = "data",
        status = STATUS_WAITING,
        emailType = PROVISIONAL_ACCEPTANCE)

      emailRepository.insert(emailObj1)
      emailRepository.insert(emailObj2)

      emailer.sendEmails()

      // Longer time needed for virtual environments with less resources
      Thread.sleep(1000)

      GreenMailHelper.getReceivedMessages.size mustEqual 1
    }
  }

  "Sending an email message via the EmailService " should {

    "result in that message ending up in the GreenMail message queue" in {

      val emailSender = new EmailSender(GreenMailHelper.smtpConfig)
      emailSender.sendMessage(
        sender = sender,
        recipient = "jonny.cavell@gmail.com",
        ccList = List("a@a.com", "b@b.com"),
        subject = "Your Registered Traveller application has been received",
        message = "This is some text",
        replyTo = Some(replyTo))

      // Longer time needed for virtual environments with less resources
      Thread.sleep(1000)

      GreenMailHelper.getLastMessageContent mustEqual "This is some text"
      GreenMailHelper.getLastMessageCCList mustEqual List("a@a.com", "b@b.com")
    }
  }
  "Sending an email with an attachment via the EmailService " should {

    "result in that message with the attachment ending up in the GreenMail message queue" in {

      val attachmentPath = Files.createTempFile("test_attachment.txt", null)
      val writer = new FileWriter(attachmentPath.toString)
      writer.write("This is an attachment")
      writer.flush()
      writer.close()

      val attachment = Attachment(attachmentPath.toString, "This is an attachment for testing", "Test attachment")

      val emailSender = new EmailSender(GreenMailHelper.smtpConfig)
      emailSender.sendMessage(
        sender = sender,
        recipient = "jonny.cavell@gmail.com",
        subject = "This is a test email with an attachment",
        message = "There should be an attachment",
        attachments = Vector(attachment),
        replyTo = Some(replyTo)
      )

      // Longer time needed for virtual environments with less resources
      Thread.sleep(1000)

      GreenMailHelper.getLastMessageContent mustEqual "There should be an attachment"

      val attachmentName = GreenMailHelper.getLastReceivedMessageContent.getBodyPart(1).getFileName
      attachmentName mustEqual "Test attachment"

      val attachmentContent = GreenMailHelper.getLastReceivedMessageContent.getBodyPart(1).
        getContent.asInstanceOf[SharedByteArrayInputStream]
      convertStreamToString(attachmentContent) mustEqual ("This is an attachment")
      val fromAddress = GreenMailHelper.getReceivedMessages.last.getFrom.head.toString
      fromAddress must contain(sender.name)
      fromAddress must contain(sender.email)
      val replyToAddress = GreenMailHelper.getReceivedMessages.last.getReplyTo.head.toString
      replyToAddress must contain(replyTo.name)
      replyToAddress must contain(replyTo.email)
    }
  }


  def convertStreamToString(inputStream: InputStream) = {
    val scanner = new Scanner(inputStream).useDelimiter("\\A")
    if (scanner.hasNext) scanner.next else ""
  }
}
