package uk.gov.homeoffice.domain.core.email

import uk.gov.homeoffice.mongo.model._
import uk.gov.homeoffice.mongo.repository._
import uk.gov.homeoffice.mongo.casbah._
import uk.gov.homeoffice.mongo.casbah.syntax._

import org.bson.types.ObjectId
import org.joda.time.DateTime

case class Email(
  emailId: String = new ObjectId().toString,
  caseId: Option[String],
  caseRef: Option[String] = None,
  date: DateTime,
  recipient: String,
  subject: String,
  text: String,
  html: String,
  status: String,
  emailType: String,
  cc: List[String] = List.empty,
  personalisations :Option[MongoDBObject] = None
) {

  def toDBObject: DBObject = {
    val builder = MongoDBObject.newBuilder()
    builder += Email.EMAIL_ID -> new ObjectId(emailId)
    caseId.foreach { id => builder += (Email.CASE_ID -> new ObjectId(id)) }
    caseRef.foreach { cr => builder += (Email.CASE_REF -> caseRef) }
    builder += Email.DATE -> date
    builder += Email.RECIPIENT -> recipient
    builder += Email.SUBJECT -> subject
    builder += Email.TEXT -> text
    builder += Email.HTML -> html
    builder += Email.STATUS -> status
    builder += Email.TYPE -> emailType
    builder += Email.CC -> MongoDBList(cc)
    personalisations.foreach { p => builder += (Email.PERSONALISATIONS -> p) }
    builder.result()
  }

  override def equals(other :Any) = other match {
    case e :Email =>
      emailId == e.emailId &&
      caseId == e.caseId &&
      caseRef == e.caseRef &&
      date.isEqual(e.date) &&
      recipient == e.recipient &&
      subject == e.subject &&
      text == e.text &&
      html == e.html &&
      status == e.status &&
      emailType == e.emailType &&
      cc == e.cc &&
      personalisations == e.personalisations
    case _ => false
  }
}

object Email {
  val EMAIL_ID: String = "_id"
  val CASE_ID: String = "caseId"
  val CASE_REF: String = "caseRef"
  val DATE: String = "date"
  val RECIPIENT: String = "recipient"
  val SUBJECT: String = "subject"
  val TEXT: String = "text"
  val HTML: String = "html"
  val STATUS: String = "status"
  val TYPE: String = "type"
  val CC: String = "cc"
  val PERSONALISATIONS :String = "personalisations"

  def apply(dbObject: DBObject): Email = {
    new Email(dbObject.get(EMAIL_ID).asInstanceOf[ObjectId].toString,
      if (dbObject.containsField(CASE_ID)) Some(dbObject.get(CASE_ID).asInstanceOf[ObjectId].toString) else None,
      if (dbObject.containsField(CASE_REF)) Some(dbObject.get(CASE_REF).asInstanceOf[String]) else None,
      dbObject.get(DATE).asInstanceOf[DateTime],
      dbObject.get(RECIPIENT).asInstanceOf[String],
      dbObject.get(SUBJECT).asInstanceOf[String],
      dbObject.get(TEXT).asInstanceOf[String],
      dbObject.get(HTML).asInstanceOf[String],
      dbObject.get(STATUS).asInstanceOf[String],
      dbObject.get(TYPE).asInstanceOf[String],
      if (dbObject.containsField(CC)) dbObject.get(CC).asInstanceOf[MongoDBList[String]].toList() else List.empty,
      if (dbObject.containsField(PERSONALISATIONS)) Some(dbObject.get(PERSONALISATIONS).asInstanceOf[MongoDBObject]) else None
    )
  }
}

object EmailStatus {
  val STATUS_WAITING = "WAITING"
  val STATUS_SENT = "SENT"
  val STATUS_ERROR = "ERROR"
  val STATUS_EMAIL_ADDRESS_ERROR = "ERROR - Email Address"
  val STATUS_EXHAUSTED = "Exhausted Retries"
  val STATUS_PARTIAL = "Partial Error"

  sealed trait EmailSentResult
  case object Waiting extends EmailSentResult
  case class Sent(newText :Option[String] = None, newHtml :Option[String] = None) extends EmailSentResult
  case class TransientError(errorMessage :String) extends EmailSentResult
  case class EmailAddressError(errorMessage :String) extends EmailSentResult
  case object ExhaustedRetries extends EmailSentResult
  case class PartialError(errorMessage :String) extends EmailSentResult

}

