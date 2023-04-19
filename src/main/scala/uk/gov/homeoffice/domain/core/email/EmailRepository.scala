package uk.gov.homeoffice.domain.core.email

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logging
import org.bson.types.ObjectId
import org.joda.time.DateTime
import uk.gov.homeoffice.mongo.casbah.{MongoSupport, Repository}

trait EmailRepository extends Repository with MongoSupport with Logging {
  val collectionName = "email"

  val MAX_LIMIT = 100

  def insert(email: Email) = collection.insert(email.toDBObject)

  def findByEmailId(emailId: String): Option[Email] =
    collection.findOne(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId))) match {
      case Some(dbo) => Some(Email(dbo))
      case None => None
    }

  def findByRecipientEmailIdAndType(recipientEmailId: String, emailType: String) = {
    val emailCursor = collection.find(byRecipientEmailIdAndEmailTypes(recipientEmailId, emailType)).sort(orderBy = MongoDBObject(Email.DATE -> -1)).limit(1).toList
    for {x <- emailCursor} yield Email(x)

  }

  def findUserInactiveWarningEmail(emailTypes: List[String], days: Int) = {
    val emailCursor = collection.find(byEmailTypesAndDate(emailTypes, days)).sort(orderBy = MongoDBObject(Email.DATE -> -1)).toList
    for {x <- emailCursor} yield Email(x)
  }

  def findByCaseId(caseId: String): List[Email] = {
    val emailCursor = collection.find(MongoDBObject(Email.CASE_ID -> new ObjectId(caseId))).sort(orderBy = MongoDBObject(Email.DATE -> -1)).toList

    for {x <- emailCursor} yield Email(x)
  }

  def findByCaseIdAndType(caseId: String, emailType: String): List[Email] = {
    val emailCursor = collection.find(MongoDBObject(Email.CASE_ID -> new ObjectId(caseId), Email.TYPE -> emailType)).toList

    for {x <- emailCursor} yield Email(x)
  }

  def findForCasesAndEmailTypes(caseIds: Iterable[ObjectId], emailTypes: Seq[String]): List[Email] = {
    val emailCursor = collection.find(byCaseIdsAndEmailTypes(caseIds, emailTypes)).toList

    for {x <- emailCursor} yield Email(x)
  }

  def findEmailTypesAndCaseIds(caseIds: Iterable[ObjectId], emailTypes: Seq[String]): List[(String, ObjectId)] =
    (for {
      item <- collection.find(byCaseIdsAndEmailTypes(caseIds, emailTypes), MongoDBObject(Email.CASE_ID -> 1, Email.TYPE -> 1))
      emailType <- item.getAs[String](Email.TYPE)
      caseId <- item.getAs[ObjectId](Email.CASE_ID)
    } yield {
      (emailType, caseId)
    }) toList

  def findByEmailType(emailType: String): List[Email] = {
    val emailCursor = collection.find(MongoDBObject(Email.TYPE -> emailType)).toList

    for {x <- emailCursor} yield Email(x)
  }


  def byEmailTypesAndDate(emailTypes: List[String], days: Int): Imports.DBObject =
    $and(Email.TYPE $in emailTypes, Email.DATE $gte DateTime.now.minusDays(days))


  def byRecipientEmailIdAndEmailTypes(recipientEmailId: String, emailTypes: String): Imports.DBObject =
    $and(Email.RECIPIENT $eq recipientEmailId, Email.TYPE $eq emailTypes)


  def byCaseIdsAndEmailTypes(caseIds: Iterable[ObjectId], emailTypes: Seq[String]): Imports.DBObject =
    $and(Email.CASE_ID $in caseIds, Email.TYPE $in emailTypes)

  def findByStatus(emailStatus: String): List[Email] = {
    val emailCursor = collection.find(byEmailStatus(emailStatus)).limit(MAX_LIMIT).toList

    for {x <- emailCursor} yield Email(x)
  }

  def findByDateRange(from: DateTime, to: Option[DateTime]): List[Email] = {
    val builder = MongoDBObject.newBuilder

    builder += Email.DATE -> dateRangeQuery(Some(from), to)

    val emailCursor = collection.find(builder.result()).toList

    for {x <- emailCursor} yield Email(x)
  }

  //Excluded html, text and cc from returned query as take up relatively large amount of space when running reports
  def findEmailSummaryByDateRange(from: DateTime, to: Option[DateTime]): Iterator[Email] = {
    val builder = MongoDBObject.empty

    val fields = MongoDBObject("user" -> 1, "emailId" -> 1, "caseId" -> 1, "caseRef" -> 1, "date" -> 1,
      "recipient" -> 1, "subject" -> 1, "status" -> 1, "type" -> 1)

    builder += Email.DATE -> dateRangeQuery(Some(from), to)

    collection.find(builder.result(), fields).map(Email(_))
  }

  def byEmailStatus(emailStatus: String): Imports.DBObject = MongoDBObject(Email.STATUS -> emailStatus)

  def updateStatus(emailId: String, newStatus: String, newText :Option[String] = None, newHtml :Option[String] = None) = {
    (newText, newHtml) match {
      case (Some(text), Some(html)) => //only updates if both are together (to avoid desync)
        collection.update(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)), $set(
          Email.STATUS -> newStatus,
          Email.TEXT -> text,
          Email.HTML -> html
        ))
      case _ =>
        collection.update(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)), $set(
          Email.STATUS -> newStatus,
        ))
    }
  }

  def updateDate(emailId: String, newDate: DateTime) =
    collection.update(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)), $set(Email.DATE -> newDate))

  def removeByCaseId(caseId: String): Unit =
    collection remove MongoDBObject(Email.CASE_ID -> new ObjectId(caseId))

  def drop = collection drop

}
