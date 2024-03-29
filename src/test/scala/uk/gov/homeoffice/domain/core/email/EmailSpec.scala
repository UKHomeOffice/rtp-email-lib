package uk.gov.homeoffice.domain.core.email

import com.mongodb._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class EmailSpec extends Specification {

  "Email" should {
    "creates the correct DbObject with all the fields and Email from dbobject" in {
      val email = EmailBuilder().copy(cc = List("cc1","cc2"))

      val dbObject = email.toDBObject

      dbObject.get(Email.EMAIL_ID).toString must_== email.emailId
      dbObject.get(Email.CASE_ID).toString must_== email.caseId.get
      dbObject.get(Email.CASE_REF).toString must_== email.caseRef.get
      dbObject.get(Email.CC).asInstanceOf[BasicDBList].toArray(Array.empty[String]).toList must_== email.cc
      dbObject.get(Email.DATE).asInstanceOf[DateTime] must_== email.date
      dbObject.get(Email.HTML) must_== email.html
      dbObject.get(Email.TEXT) must_== email.text
      dbObject.get(Email.RECIPIENT) must_== email.recipient
      dbObject.get(Email.STATUS) must_== email.status
      dbObject.get(Email.SUBJECT) must_== email.subject
      dbObject.get(Email.TYPE) must_== email.emailType

      Email(dbObject) must_== email
    }

    "create correct Email if cc field is missing from the dbObject" in {
      val email = EmailBuilder()

      val dbObject = email.toDBObject
      dbObject.removeField("cc")

      Email(dbObject).cc must_== List.empty

    }

    "can store and retrieve adhoc personalisations from an email" in {
      val myStringList :List[String] = List("reason1", "reason3")

      val email = EmailBuilder().copy(personalisations = Some(MongoDBObject("reasons" -> MongoDBList(myStringList :_*))))

      email.toDBObject.get(Email.PERSONALISATIONS).asInstanceOf[DBObject].getAs[MongoDBList]("reasons") must beSome(MongoDBList("reason1", "reason3"))

    }
  }

}
