package uk.gov.homeoffice.domain.core.email

import grizzled.slf4j.Logging
import org.bson.types.ObjectId
import org.joda.time.DateTime

import uk.gov.homeoffice.mongo._
import uk.gov.homeoffice.mongo.model._
import uk.gov.homeoffice.mongo.model.syntax._
import uk.gov.homeoffice.mongo.repository._
import uk.gov.homeoffice.mongo.casbah._
import uk.gov.homeoffice.mongo.casbah.syntax._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import uk.gov.homeoffice.mongo.MongoJsonEncoders.DatabaseEncoding._

import scala.language.postfixOps

class EmailRepository(mongoCasbahRepository :MongoCasbahRepository) extends MongoCasbahSalatRepository[Email](mongoCasbahRepository) {

  def toMongoDBObject(a :Email) :MongoResult[MongoDBObject] = Right(a.toDBObject.mongoDBObject)
  def fromMongoDBObject(mongoDBObject :MongoDBObject) :MongoResult[Email] = Right(Email(mongoDBObject.asDBObject()))

  val collectionName = "email"

  val MAX_LIMIT = 100

  def findByEmailId(emailId: String): Option[Email] =
    findOne(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)))

  def findByRecipientEmailIdAndType(recipientEmailId: String, emailType: String) :List[Email] =
    find(byRecipientEmailIdAndEmailTypes(recipientEmailId, emailType)).sort(orderBy = MongoDBObject(Email.DATE -> -1)).limit(1).toList()

  def findUserInactiveWarningEmail(emailTypes: List[String], days: Int) :List[Email] =
    find(byEmailTypesAndDate(emailTypes, days)).sort(orderBy = MongoDBObject(Email.DATE -> -1)).toList()

  def findByCaseId(caseId: String): List[Email] =
    find(MongoDBObject(Email.CASE_ID -> new ObjectId(caseId))).sort(orderBy = MongoDBObject(Email.DATE -> -1)).toList()

  def findByCaseIdAndType(caseId: String, emailType: String): List[Email] =
    find(MongoDBObject(Email.CASE_ID -> new ObjectId(caseId), Email.TYPE -> emailType)).toList()

  def findForCasesAndEmailTypes(caseIds: Iterable[ObjectId], emailTypes: Seq[String]): List[Email] =
    find(byCaseIdsAndEmailTypes(caseIds, emailTypes)).toList()

  def findEmailTypesAndCaseIds(caseIds: Iterable[ObjectId], emailTypes: Seq[String]): List[(String, ObjectId)] =
    mongoCasbahRepository.find(byCaseIdsAndEmailTypes(caseIds, emailTypes), MongoDBObject(Email.CASE_ID -> 1, Email.TYPE -> 1)).toList()
      .map { mongoDBObject => (mongoDBObject.as[String](Email.TYPE), mongoDBObject.as[ObjectId](Email.CASE_ID)) }

  def findByEmailType(emailType: String): List[Email] =
    find(MongoDBObject(Email.TYPE -> emailType)).toList()

  def byEmailTypesAndDate(emailTypes: List[String], days: Int) :MongoDBObject =
    $and(Email.TYPE $in emailTypes, Email.DATE $gte DateTime.now.minusDays(days))

  def byRecipientEmailIdAndEmailTypes(recipientEmailId: String, emailTypes: String) :MongoDBObject =
    $and(Email.RECIPIENT $eq recipientEmailId, Email.TYPE $eq emailTypes)

  def byCaseIdsAndEmailTypes(caseIds: Iterable[ObjectId], emailTypes: Seq[String]) :MongoDBObject =
    $and(Email.CASE_ID $in caseIds, Email.TYPE $in emailTypes)

  def findByStatus(emailStatus: String): List[Email] =
    find(byEmailStatus(emailStatus)).limit(MAX_LIMIT).toList()

  def findByDateRange(from: DateTime, to: Option[DateTime]): List[Email] = {
    val builder = MongoDBObject.newBuilder()
    builder += Email.DATE -> dateRangeQuery(Some(from), to)

    find(builder.result()).toList()
  }

  //Excluded html, text and cc from returned query as take up relatively large amount of space when running reports
  def findEmailSummaryByDateRange(from: DateTime, to: Option[DateTime]): Iterator[Email] = {
    val builder = MongoDBObject.empty()

    val fields = MongoDBObject("user" -> 1, "emailId" -> 1, "caseId" -> 1, "caseRef" -> 1, "date" -> 1,
      "recipient" -> 1, "subject" -> 1, "status" -> 1, "type" -> 1)

    builder += Email.DATE -> dateRangeQuery(Some(from), to)

    find(builder.result(), fields).toList().toIterator
  }

  def byEmailStatus(emailStatus: String) = MongoDBObject(Email.STATUS -> emailStatus)

  def updateStatus(emailId: String, newStatus: String, newText :Option[String] = None, newHtml :Option[String] = None) = {
    (newText, newHtml) match {
      case (Some(text), Some(html)) => //only updates if both are together (to avoid desync)
        update(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)), MongoDBObject("$set" -> MongoDBObject(
          Email.STATUS -> newStatus,
          Email.TEXT -> text,
          Email.HTML -> html
        )))
      case _ =>
        update(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)), MongoDBObject("$set" -> MongoDBObject(Email.STATUS -> newStatus)))
    }
  }

  def updateDate(emailId: String, newDate: DateTime) =
    update(MongoDBObject(Email.EMAIL_ID -> new ObjectId(emailId)), $set(Email.DATE -> newDate))

  def removeByCaseId(caseId: String): Unit =
    remove(MongoDBObject(Email.CASE_ID -> new ObjectId(caseId)))

}

object EmailRepository {
  def apply(mongoConnection :MongoConnection) =
    new EmailRepository(
      new MongoCasbahRepository(
        new MongoJsonRepository(
          new MongoStreamRepository(mongoConnection, collectionName="email", primaryKeys=List("emailId"))
        )
      )
    )
}
