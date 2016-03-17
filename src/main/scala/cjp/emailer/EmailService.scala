package cjp.emailer

import java.io.FileOutputStream
import java.nio.file.Files
import java.util.Properties
import javax.mail.{Flags, Folder, Message, Multipart, Part, Session, URLName}

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.mail.{DefaultAuthenticator, EmailAttachment, HtmlEmail}
import org.joda.time.DateTime

import scala.Predef._
import scala.collection.mutable.ListBuffer

object SSLProps {

  java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider())

  val props = new java.util.Properties()

  // set this session up to use SSL for IMAP connections
  props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory")

  // don't fallback to normal IMAP connections on failure.
  props.setProperty("mail.imap.socketFactory.fallback", "false")

  // use the simap port for imap/ssl connections.
  props.setProperty("mail.imap.socketFactory.port", "993")
}

object EmailService {

  private def addAttachments(email: HtmlEmail, attachments: Seq[Attachment]) {
    attachments.foreach { attachment =>
      val emailAttachment = new EmailAttachment()
      emailAttachment.setPath(attachment.path)
      emailAttachment.setDisposition(EmailAttachment.ATTACHMENT)
      emailAttachment.setDescription(attachment.description)
      emailAttachment.setName(attachment.name)
      email.attach(emailAttachment)
    }
  }

  /**
   * Private helper invoked by the actors that sends the email
    *
    * @param emailMessage the email message
   */
  def send(emailMessage: EmailMessage) {

    val email = new HtmlEmail()


    if (emailMessage.hasAttachments) {
      addAttachments(email, emailMessage.attachments)
    }

    email.setStartTLSEnabled(emailMessage.smtpConfig.tls)
    email.setSSLOnConnect(emailMessage.smtpConfig.ssl)
    email.setSmtpPort(emailMessage.smtpConfig.port)
    email.setHostName(emailMessage.smtpConfig.host)
    email.setCharset("UTF-8")
    email.setAuthenticator(new DefaultAuthenticator(
      emailMessage.smtpConfig.user,
      emailMessage.smtpConfig.password
    ))

    emailMessage.text match {
      case Some(text) => email.setTextMsg(text)
      case None =>
    }

    emailMessage.html match {
      case Some(html) => email.setHtmlMsg(html)
      case None =>
    }

    emailMessage.replyTo match  {
      case Some(em) => email.addReplyTo(em.email, em.name)
      case None =>
    }

    val emailToSend = email.addTo(emailMessage.recipient)
      .setFrom(emailMessage.from.email, emailMessage.from.name)
      .setSubject(emailMessage.subject)

    if (emailMessage.ccList.nonEmpty) emailToSend.addCc(emailMessage.ccList: _*).send() else emailToSend.send()
  }

  /*
  Receive emails from a given imapconfig and from a specific email address.
  Delete the email if required
   */
  def receiveEmails(imapConfig: ImapConfig, from: String, delete: Boolean, sessionProperties: Properties = SSLProps.props, moveToFolder: Option[String] = None): List[ReceivedEmail] = {

    var receivedEmails = List[ReceivedEmail]()
    val session = Session.getInstance(sessionProperties)
    val urlName = new URLName(imapConfig.protocol, imapConfig.server,
      imapConfig.port.toInt, null, imapConfig.user, imapConfig.password)
    val store = session.getStore(urlName)
    store.connect(imapConfig.server, imapConfig.user, imapConfig.password)

    val inbox: Folder = store.getFolder("Inbox")

    // Set the mode to the read-write so we can delete
    inbox.open(Folder.READ_WRITE)
    val messages: Array[Message] = inbox.getMessages

    // process all messages with a matching from address
    messages.foreach(message => {
      message.getFrom.foreach(address => {

        if (address.toString.toUpperCase.contains(from.toUpperCase)) {
          receivedEmails = List.concat(processMessage(message), receivedEmails)

          if (moveToFolder.isDefined) {
            val folderToMoveMessagesTo = store.getFolder(moveToFolder.get)
            if (!folderToMoveMessagesTo.exists()) folderToMoveMessagesTo.create(Folder.HOLDS_MESSAGES)
            inbox.copyMessages(Array(message), folderToMoveMessagesTo)
          }
          // Delete message if required
          if (delete) message.setFlag(Flags.Flag.DELETED, true)
        }
      })
    })

    inbox.close(delete)
    store.close()
    receivedEmails
  }

  /*
  Read a mail message and save each attachment to a temporary file.
  Return all temp file paths
   */
  private def processMessage(message: Message): List[ReceivedEmail] = {
    val receivedEmails = new ListBuffer[ReceivedEmail]

    try {
      val multipart = message.getContent.asInstanceOf[Multipart]

      // Get all multiparts
      (0 to (multipart.getCount - 1)).foreach(partNumber => {
        val bodyPart = multipart.getBodyPart(partNumber)

        // Only process attachments
        if (bodyPart.getDisposition != null &&
          bodyPart.getDisposition.toUpperCase == Part.ATTACHMENT.toUpperCase &&
          !bodyPart.getFileName.isEmpty) {

          // Copy attachment to a temporary file
          val inputStream = bodyPart.getInputStream
          val filename = bodyPart.getFileName
          val prefix = FilenameUtils.getBaseName(filename)
          val suffix = FilenameUtils.getExtension(filename)
          val extension = if (StringUtils.isBlank(suffix)) null else s".$suffix"
          //debug(s"Creating temp file called $filename")
          val tempFile = Files.createTempFile(prefix, extension).toFile
          val outputStream = new FileOutputStream(tempFile)

          Iterator
            .continually(inputStream.read)
            .takeWhile(-1 !=)
            .foreach(outputStream.write)

          // Add file path to list
          receivedEmails += ReceivedEmail(tempFile.toString, new DateTime(message.getReceivedDate))
        }
      })
    } catch {
      case e: Exception => //debug(e.getMessage)
        throw e
    }

    receivedEmails.toList
  }
}

case class ReceivedEmail(filePath: String, receivedTime: DateTime)