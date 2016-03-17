package uk.gov.homeoffice.domain.core.email

import domain.core.email.Email
import domain.core.email.EmailStatus.STATUS_WAITING
import org.bson.types.ObjectId
import org.joda.time.DateTime

object EmailBuilder {

  def apply(caseId: Option[ObjectId] = Some(new ObjectId()), emailType: String = "Some email type", html: String = "html") = {
    val email = Email(
      caseId = if (caseId.isEmpty) None else Some(caseId.get.toString),
      caseRef = Some("123"),
      date = new DateTime(),
      recipient = caseId + " recipient",
      subject = "subject",
      text = "text",
      html = html,
      status = STATUS_WAITING,
      emailType = emailType)
    email
  }


}
