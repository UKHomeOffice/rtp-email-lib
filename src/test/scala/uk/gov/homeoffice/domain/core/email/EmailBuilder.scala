package uk.gov.homeoffice.domain.core.email

import org.bson.types.ObjectId
import org.joda.time.DateTime
import uk.gov.homeoffice.domain.core.email.EmailStatus._

object EmailBuilder {
  def apply(caseId: Option[ObjectId] = Some(new ObjectId()), emailType: String = "Some email type", html: String = "html",text:String="text",date: DateTime = new DateTime()) = {
    val email = Email(
      caseId = if (caseId.isEmpty) None else Some(caseId.get.toString),
      caseRef = Some("123"),
      date = date,
      recipient = caseId.map(_.toHexString).getOrElse("") + " recipient",
      subject = "subject",
      text = text,
      html = html,
      status = STATUS_WAITING,
      emailType = emailType)
    email
  }
}
