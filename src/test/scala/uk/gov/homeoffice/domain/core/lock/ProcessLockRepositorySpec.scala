package uk.gov.homeoffice.domain.core.lock

import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import salat.dao.SalatInsertError
import uk.gov.homeoffice.mongo.casbah.MongoSpecification

class ProcessLockRepositorySpec extends Specification with MongoSpecification {
  val repository = new ProcessLockRepository with TestMongo

  "lock" should {
    "acquire lock if not already taken" in {
      val lock: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST")

      lock mustNotEqual None

      val savedLock = repository.findOne(MongoDBObject("name" -> "SOME_LOCK")).get

      savedLock.host mustEqual "SOME_HOST"
      savedLock.name mustEqual "SOME_LOCK"
    }

    "not acquire lock if already exists" in {
      val now = DateTime.now()
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now))

      val lock: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST")

      lock must beNone

      val savedLock = repository.findOne(MongoDBObject("name" -> "SOME_LOCK")).get

      savedLock.host mustEqual "SOME_OTHER_HOST"
      savedLock.createdAt mustEqual now
    }

    "acquire lock if already exists but has expired" in {
      val now = DateTime.now()
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now.minusMinutes(ProcessLockRepository.EXPIRY_PERIOD_MINS + 1)))

      val lock: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST")

      lock mustNotEqual None

      val savedLock = repository.findOne(MongoDBObject("name" -> "SOME_LOCK")).get

      savedLock.host mustEqual "SOME_HOST"
    }

    "not be able to insert two locks" in {
      val now = DateTime.now()
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now))
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now)) must throwA[SalatInsertError]
    }
  }
}
