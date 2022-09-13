package cjp.emailer

import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import javax.mail.Message.RecipientType
import javax.mail.internet.{MimeMessage, MimeMultipart}

import com.icegreen.greenmail.util.{GreenMail, ServerSetup}
import com.icegreen.greenmail.configuration.GreenMailConfiguration
import org.specs2.specification.BeforeAfterEach

import scala.concurrent.duration.FiniteDuration

trait GreenMailHelper extends BeforeAfterEach {
  override def before() :Unit = {
    GreenMailHelper.start()
  }

  override def after() :Unit = {
    GreenMailHelper.stop()
  }
}

object GreenMailHelper{
  val retryOn = FiniteDuration(10, TimeUnit.SECONDS)
  val port = findAvailablePort
  val port2 = findAvailablePort
  val smtpConfig = SmtpConfig(port = port)
  val smtpConfig2 = SmtpConfig(port = port2)
  var greenMail: GreenMail = null

  def initGreenMail() :Unit = {
    greenMail = new GreenMail(new ServerSetup(port, "localhost", "smtp"))
      .withConfiguration(new GreenMailConfiguration().withDisabledAuthentication())
  }

  def getReceivedMessages = greenMail.getReceivedMessages
  def getLastMessage: MimeMessage = getReceivedMessages.apply(getReceivedMessages.size - 1)
  def getLastReceivedMessageContent =  getLastMessage.getContent.asInstanceOf[MimeMultipart]
  def getLastMessageCCList = getLastMessage.getRecipients(RecipientType.CC).map(_.toString).toList

  def getLastMessageContent = getLastReceivedMessageContent.getBodyPart(0).getContent.asInstanceOf[String]

  def findAvailablePort = {
    val availablePort = 12345 until 12700 find {
      port =>   try {
        new ServerSocket(port).close()
        true
      } catch {
        case ioe: IOException => false
      }
    }
    availablePort.getOrElse(throw new RuntimeException("Could find an available port"))
  }

  def start() {
    initGreenMail()
    greenMail.start()
  }

  def stop() {
    greenMail.stop()
  }
}

