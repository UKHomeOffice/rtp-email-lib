package uk.gov.homeoffice.domain.core.email

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.mongo.casbah.EmbeddedMongoSpecification

class EmailRepositorySpec extends Specification with EmbeddedMongoSpecification {
  val repository = new EmailRepository with TestMongo
  val PROVISIONAL_ACCEPTANCE = "provisional-acceptance"
  val FAILED_CREDIBILITY_CHECK = "failed-credibility-check"
  val MEMBERSHIP_EXPIRES_SOON = "expiring soon"
  val now = new DateTime()

  def insertEmail(caseId: Option[ObjectId] = Some(new ObjectId()), emailType: String = PROVISIONAL_ACCEPTANCE, html: String = "html") = {
    val email = EmailBuilder(caseId = caseId, html = html, emailType = emailType, date = now)
    repository.insert(email)
    email
  }

  "email repository" should {
    "find Email by caseId" in {
      val email = insertEmail()
      val emailDocuments = repository.findByCaseId(email.caseId.get.toString)

      emailDocuments.size mustEqual 1
      emailDocuments.head.recipient mustEqual s"${email.caseId} recipient"
    }

    "find Emails by caseId and emailType" in {
      insertEmail(emailType = PROVISIONAL_ACCEPTANCE)
      val failedCredibilityEmail = insertEmail(emailType = FAILED_CREDIBILITY_CHECK)
      val emailDocuments = repository.findByCaseIdAndType(failedCredibilityEmail.caseId.get.toString, FAILED_CREDIBILITY_CHECK)

      emailDocuments.size mustEqual 1
      emailDocuments.head.recipient mustEqual s"${failedCredibilityEmail.caseId} recipient"
    }

    "find Email by id" in {
      val email = insertEmail()
      val persistedEmail = repository.findByEmailId(email.emailId)
      persistedEmail.get.recipient mustEqual s"${email.caseId} recipient"
    }

    "find emails by email Id without a caseId" in {
      val emailObj = insertEmail(caseId = None)
      val email = repository.findByEmailId(emailObj.emailId)
      email.get.recipient mustEqual s"${emailObj.caseId} recipient"
    }

    "find emails by status" in {
      val emailObj = insertEmail()
      val emailDocs = repository.findByStatus(STATUS_WAITING)
      emailDocs.size mustEqual 1
      emailDocs.head.emailId mustEqual emailObj.emailId
    }

    "find email summary by date range" in {
      val emailObj = insertEmail()

      repository.findEmailSummaryByDateRange(now.minusHours(1), Some(now)).toStream must beLike {
        case email #:: Stream.Empty =>
          email.emailId mustEqual emailObj.emailId
          email.html must beNull
          email.text must beNull
      }
    }
  }

  "updateStatus" should {
    "update Email status" in {
      val emailObj = insertEmail()
      repository.updateStatus(emailObj.emailId, STATUS_SENT)
      val Some(updatedEmail) = repository.findByEmailId(emailObj.emailId)
      updatedEmail.status mustEqual STATUS_SENT
    }
  }

  "resend" should {
    "create a new Email" in {
      val emailObj = insertEmail()
      val newEmail = repository.resend(emailObj.emailId)
      val Some(foundEmail) = repository.findByEmailId(newEmail.emailId)
      foundEmail.status mustEqual STATUS_WAITING
    }

    "create a new Email with new recipient" in {
      val emailObj = insertEmail()
      val newEmail = repository.resend(emailObj.emailId, "peppa pig")
      val Some(foundEmail) = repository.findByEmailId(newEmail.emailId)
      foundEmail.status mustEqual STATUS_WAITING
      foundEmail.recipient mustEqual "peppa pig"
    }
  }

  "insert record" should {
    "contain the correct html" in {
      val templateHtml = "<html><title>Hello</title></html>"
      val emailObj = insertEmail(html = templateHtml)
      val foundEmail = repository.findByEmailId(emailObj.emailId)
      foundEmail.get.html mustEqual templateHtml
    }
  }

  "remove by Case Id" should {
    "remove an email associated with a case" in {
      val allCaseIds = (1 to 3).map { _ =>
        val email = insertEmail()
        email.caseId.get.toString
      }

      repository.removeByCaseId(allCaseIds.head)

      repository.findByCaseId(allCaseIds.head).size mustEqual 0
      repository.findByCaseId(allCaseIds(1)).size mustEqual 1
      repository.findByCaseId(allCaseIds(2)).size mustEqual 1
    }
  }

  "Find warning emails for cases" should {
    "Return all warning emails for each case" in {
      val _caseId = new ObjectId()

      val warningEmailTypes = Seq(PROVISIONAL_ACCEPTANCE, FAILED_CREDIBILITY_CHECK, MEMBERSHIP_EXPIRES_SOON)

      insertEmail(caseId = Some(_caseId), emailType = "Ignored Email Type") // should be ignored in the query below
      val provisionalAcceptanceEmail = insertEmail(caseId = Some(_caseId), emailType = PROVISIONAL_ACCEPTANCE)
      val failedCredibilityEmail = insertEmail(caseId = Some(_caseId), emailType = FAILED_CREDIBILITY_CHECK)
      val membershipExpiresSoonEmail = insertEmail(caseId = Some(_caseId), emailType = MEMBERSHIP_EXPIRES_SOON)

      val emails = repository.findForCasesAndEmailTypes(Seq(_caseId), warningEmailTypes)

      emails mustEqual Seq(provisionalAcceptanceEmail, failedCredibilityEmail, membershipExpiresSoonEmail)
    }
  }

  "findCaseIdsForEmailAlreadySent" should {
    "Return all case ids for which email already sent" in {
      val _caseId = new ObjectId()
      val notToBeFound = ObjectId.get()

      val emailType = "email type"

      insertEmail(caseId = Some(_caseId), emailType = emailType)
      insertEmail(caseId = Some(notToBeFound), emailType = "some other email type")

      val caseIds = repository.findEmailTypesAndCaseIds(Seq(_caseId, notToBeFound), Seq(emailType))

      caseIds must haveSize(1)
      caseIds must contain((emailType, _caseId))
    }

    "Return empty if no case ids provided" in {
      val _caseId = new ObjectId()
      val notToBeFound = ObjectId.get()

      val emailType = "email type"

      insertEmail(caseId = Some(_caseId), emailType = emailType)
      insertEmail(caseId = Some(notToBeFound), emailType = "some other email type")

      val caseIds = repository.findEmailTypesAndCaseIds(Seq(), Seq(emailType))

      caseIds must haveSize(0)
    }
  }
}