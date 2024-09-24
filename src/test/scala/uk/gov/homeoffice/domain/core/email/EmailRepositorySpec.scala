package uk.gov.homeoffice.domain.core.email

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.matcher.Scope
import org.specs2.mutable.Specification
import uk.gov.homeoffice.domain.core.email.EmailStatus._
import uk.gov.homeoffice.mongo.TestMongo

class EmailRepositorySpec extends Specification {

  class Context extends Scope {
    implicit val repository = EmailRepository(TestMongo.testConnection)
  }

  sequential

  val PROVISIONAL_ACCEPTANCE = "provisional-acceptance"
  val FAILED_CREDIBILITY_CHECK = "failed-credibility-check"
  val MEMBERSHIP_EXPIRES_SOON = "expiring soon"
  val now = new DateTime()

  def insertEmail(
    caseId: Option[ObjectId] = Some(new ObjectId()),
    emailType: String = PROVISIONAL_ACCEPTANCE,
    html: String = "html",
    text: String = "text",
    date: DateTime = now
  )(implicit emailRepository :EmailRepository) = {
    val email = EmailBuilder(caseId = caseId, html = html, emailType = emailType, date = date, text = text)
    emailRepository.insert(email)
    email
  }


  "email repository" should {
    "find Email by caseId" in new Context {
      val email = insertEmail()
      val emailDocuments = repository.findByCaseId(email.caseId.get.toString)

      emailDocuments.size mustEqual 1
      emailDocuments.head.recipient mustEqual s"${email.caseId} recipient"
    }

    "find Emails by caseId and emailType" in new Context {
      insertEmail(emailType = PROVISIONAL_ACCEPTANCE)
      val failedCredibilityEmail = insertEmail(emailType = FAILED_CREDIBILITY_CHECK)
      val emailDocuments = repository.findByCaseIdAndType(failedCredibilityEmail.caseId.get.toString, FAILED_CREDIBILITY_CHECK)

      emailDocuments.size mustEqual 1
      emailDocuments.head.recipient mustEqual s"${failedCredibilityEmail.caseId} recipient"
    }

    "find Email by id" in new Context {
      val email = insertEmail()
      val persistedEmail = repository.findByEmailId(email.emailId)
      persistedEmail.get.recipient mustEqual s"${email.caseId} recipient"
    }

    "find emails by email Id without a caseId" in new Context {
      val emailObj = insertEmail(caseId = None)
      val email = repository.findByEmailId(emailObj.emailId)
      email.get.recipient mustEqual s"${emailObj.caseId} recipient"
    }

    "find emails by status" in new Context {
      val emailObj = insertEmail()
      val emailDocs = repository.findByStatus(STATUS_WAITING)
      emailDocs.size mustEqual 1
      emailDocs.head.emailId mustEqual emailObj.emailId
    }

    "find email by recipient email" in new Context {
      val emailObj = insertEmail()
      val emailDocs = repository.findByRecipientEmailIdAndType(emailObj.recipient, PROVISIONAL_ACCEPTANCE)
      emailDocs.size mustEqual 1
      emailDocs.head.emailId mustEqual emailObj.emailId
    }
  }

  "updateStatus" should {
    "update Email status" in new Context {
      val emailObj = insertEmail()
      repository.updateStatus(emailObj.emailId, STATUS_SENT)
      val Some(updatedEmail) = repository.findByEmailId(emailObj.emailId)
      updatedEmail.status mustEqual STATUS_SENT
    }
  }

  "insert record" should {
    "contain the correct html" in new Context {
      val templateHtml = "<html><title>Hello</title></html>"
      val emailObj = insertEmail(html = templateHtml)
      val foundEmail = repository.findByEmailId(emailObj.emailId)
      foundEmail.get.html mustEqual templateHtml
    }
  }

  "remove by Case Id" should {
    "remove an email associated with a case" in new Context {
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

  "updateDate" should {
    "update date older than 7 days" in new Context {
      val emailObj = insertEmail()
      repository.updateDate(emailObj.emailId, DateTime.now().withTimeAtStartOfDay().minusDays(7))
      val Some(updatedEmail) = repository.findByEmailId(emailObj.emailId)
      updatedEmail.date isBefore DateTime.now().minusDays(7)
    }

    "update date younger than 7 days" in new Context {
      val emailObj = insertEmail()
      repository.updateDate(emailObj.emailId, DateTime.now().withTimeAtStartOfDay().plusDays(7))
      val Some(updatedEmail) = repository.findByEmailId(emailObj.emailId)
      updatedEmail.date isBefore DateTime.now().plusDays(7)
    }
  }

  "Find warning emails for cases" should {
    "Return all warning emails for each case" in new Context {
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

  "findByEmailType" should {
    "Return all emails by email type" in new Context {
      val provisionalAcceptanceEmail1 = insertEmail(caseId = Some(new ObjectId()), emailType = PROVISIONAL_ACCEPTANCE)
      val provisionalAcceptanceEmail2 = insertEmail(caseId = Some(new ObjectId()), emailType = PROVISIONAL_ACCEPTANCE)
      insertEmail(caseId = Some(new ObjectId()), emailType = FAILED_CREDIBILITY_CHECK)

      repository.findByEmailType(PROVISIONAL_ACCEPTANCE) mustEqual Seq(provisionalAcceptanceEmail1, provisionalAcceptanceEmail2)
    }
  }

  "findCaseIdsForEmailAlreadySent" should {
    "Return all case ids for which email already sent" in new Context {
      val _caseId = new ObjectId()
      val notToBeFound = ObjectId.get()

      val emailType = "email type"

      insertEmail(caseId = Some(_caseId), emailType = emailType)
      insertEmail(caseId = Some(notToBeFound), emailType = "some other email type")

      val caseIds = repository.findEmailTypesAndCaseIds(Seq(_caseId, notToBeFound), Seq(emailType))

      caseIds must haveSize(1)
      caseIds must contain((emailType, _caseId))
    }

    "Return empty if no case ids provided" in new Context {
      val _caseId = new ObjectId()
      val notToBeFound = ObjectId.get()

      val emailType = "email type"

      insertEmail(caseId = Some(_caseId), emailType = emailType)
      insertEmail(caseId = Some(notToBeFound), emailType = "some other email type")

      val caseIds = repository.findEmailTypesAndCaseIds(Seq(), Seq(emailType))

      caseIds must haveSize(0)
    }
  }

  "findUserInactiveWarningEmail" should {
    "return Inactive email" in new Context {
      val _caseId = new ObjectId()
      val emailTypeObject1 = ObjectId.get()
      val emailTypeObject2 = ObjectId.get()

      val emailType1 = "email type 1"
      val emailType2 = "email type 2"

      insertEmail(caseId = Some(_caseId), emailType = "email type")
      insertEmail(caseId = Some(emailTypeObject1), emailType = emailType1)
      insertEmail(caseId = Some(emailTypeObject2), emailType = emailType2)
      val emails = repository.findUserInactiveWarningEmail(List(emailType1, emailType2), 1)

      emails.size mustEqual 2

      emails.count(_.emailType == emailType1) mustEqual 1

      emails.count(_.emailType == emailType2) mustEqual 1

    }


    "return Inactive email within 1 day" in new Context {
      val _caseId = new ObjectId()
      val emailTypeObject1 = ObjectId.get()
      val emailTypeObject2 = ObjectId.get()

      val emailType1 = "email type 1"

      val emailType2 = "email type 2"

      insertEmail(caseId = Some(_caseId), emailType = "email type")
      insertEmail(caseId = Some(emailTypeObject1), emailType = emailType1, date = DateTime.now.minusDays(2))
      insertEmail(caseId = Some(emailTypeObject2), emailType = emailType2)
      val emails = repository.findUserInactiveWarningEmail(List(emailType1, emailType2), 1)

      emails.size mustEqual 1

      emails.count(_.emailType == emailType1) mustEqual 0

      emails.count(_.emailType == emailType2) mustEqual 1

    }

    "returns no Inactive email within n day" in new Context {
      val _caseId = new ObjectId()
      val emailTypeObject1 = ObjectId.get()
      val emailTypeObject2 = ObjectId.get()

      val emailType1 = "email type 1"

      val emailType2 = "email type 2"

      insertEmail(caseId = Some(_caseId), emailType = "email type")
      insertEmail(caseId = Some(emailTypeObject1), emailType = emailType1, date = DateTime.now.minusDays(2))
      insertEmail(caseId = Some(emailTypeObject2), emailType = emailType2, date = DateTime.now.minusDays(2))
      val emails = repository.findUserInactiveWarningEmail(List(emailType1, emailType2), 1)

      emails.size mustEqual 0

      emails.count(_.emailType == emailType1) mustEqual 0

      emails.count(_.emailType == emailType2) mustEqual 0

    }

  }
}
