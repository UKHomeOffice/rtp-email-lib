package cjp.emailer

class EmailSender(val smtpConfig: SmtpConfig) {

  /**
   * Create message and send on to the async EmailService
   * @param sender
   * @param recipient
   * @param subject
   * @param message
   * @param attachments
   */
  def sendMessage(sender: EmailAddress, recipient: String, ccList: List[String] = List.empty, subject: String, message: String,
                  attachments: Seq[Attachment] = Vector[Attachment](), html: Option[String] = None, replyTo: Option[EmailAddress]) = {

    val emailMessage = EmailMessage(
      subject = subject,
      recipient = recipient,
      ccList = ccList,
      from = sender,
      replyTo = replyTo,
      text = Some(message),
      html = html,
      attachments = attachments,
      smtpConfig = smtpConfig)

    EmailService.send(emailMessage)
  }
}

case class EmailMessage(subject: String,
                        recipient: String,
                        ccList: List[String] = List.empty,
                        from: EmailAddress,
                        replyTo: Option[EmailAddress] = None,
                        text: Option[String] = None,
                        html: Option[String] = None,
                        attachments: Seq[Attachment],
                        smtpConfig: SmtpConfig) {

  def hasAttachments = attachments.nonEmpty
}