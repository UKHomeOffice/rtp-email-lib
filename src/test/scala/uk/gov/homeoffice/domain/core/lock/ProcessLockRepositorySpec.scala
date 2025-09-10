package uk.gov.homeoffice.domain.core.lock

import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.matcher.Scope
import uk.gov.homeoffice.mongo.TestMongo
import uk.gov.homeoffice.mongo.casbah._
import uk.gov.homeoffice.mongo.model.MongoException

class ProcessLockRepositorySpec extends Specification {

  class Context extends Scope {
    val repository = new ProcessLockRepository(TestMongo.testConnection())
    repository.initialise()
  }

  sequential

  "lock" should {
    "acquire lock if not already taken" in new Context {
      val lock: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST")

      lock mustNotEqual None

      val savedLock = repository.findOne(MongoDBObject("name" -> "SOME_LOCK")).get

      savedLock.host mustEqual "SOME_HOST"
      savedLock.name mustEqual "SOME_LOCK"
    }

    "not acquire lock if already exists" in new Context {
      val now = DateTime.now()
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now))

      val lock: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST")

      lock must beNone

      val savedLock = repository.findOne(MongoDBObject("name" -> "SOME_LOCK")).get

      savedLock.host mustEqual "SOME_OTHER_HOST"
      savedLock.createdAt mustEqual now
    }

    "acquire lock if already exists but has expired" in new Context {
      val now = DateTime.now()
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now.minusMinutes(ProcessLockRepository.EXPIRY_PERIOD_MINS + 1)))

      val lock: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST")

      lock mustNotEqual None

      val savedLock = repository.findOne(MongoDBObject("name" -> "SOME_LOCK")).get

      savedLock.host mustEqual "SOME_HOST"
    }

    "not be able to insert two locks" in new Context {
      val now = DateTime.now()
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now))
      repository.insert(Lock("SOME_LOCK", "SOME_OTHER_HOST", now)) must throwA[MongoException]
    }

    /*
     * In most implementations the lock mechanism works like this:
     *
     * def myFunction() = obtainLock(x, y).map { lock =>
     *    // do something
     *    releaseLock(lock)
     * }
     *
     * 1. aquire a lock
     * 2. do some work
     * 3. release the lock.
     *
     * A lock lasts 10 minutes. If a piece of code on a host accidently ran the code
     * twice... e.g:
     *
     * Future(myFunction()); myFunction()
     *
     * Then because obtainLock returns None if any lock is active, even you're own
     * it, you're protected against these race conditons. This is a well-written function
     * in this sense.
     *
     * RT caseworker used to set a flag in a shared memcache server as a locking mechanism
     * read by all Akka actors. But there hasn't been a memcache for RT caseworker for years!
     *
     * I've switched to obtainLock and ProccessLockRepository but as different Akka actors reuse
     * the same lock, it seems dangerous to cache a lock client side or compare its expiry time
     * for validity offline.
     *
     * The default behaviour of obtainLock returns None if anyone aleady holds the lock causing
     * a starvation issue for different actors trying to query the db. Only one actor every 10
     * minutes can run unless the lock names are split or the code is otherwise refactored.
     *
     * So I have provided an argument called "sharedLockOnHost". This will return a lock if you
     * already hold it, so all akka actors will be given grace to execute for a given host. It also
     * extends the time so things dont start right before the expiry threshold passes.
     *
     * This is perhaps not perfect (an akka actor may have a lock that has been replaced) and
     * akka actors on the same host could run concurrently if misconfigured, but should be
     * acceptable as a short term solution to prevent race conditions between rt-caseworker
     * hosts whilst I re-engineer the code.
     */

    "extend a lock if already owned by yourself and sharedLockOnHost enabled" in new Context {

      val lockOne: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST", sharedLockOnHost = true)
      lockOne.isDefined must beTrue

      // how old behaviour currently works. calling obtain lock on a lock you have returns None.
      // (this protects race conditions within the same host, ideal)
      val oldRelockBehaviour: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST", sharedLockOnHost = false)
      oldRelockBehaviour.isDefined must beFalse

      // new behaviour is that obtainLock from a host which already extends the timer and returns a lock
      val newRelockBehaviour: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST", sharedLockOnHost = true)
      newRelockBehaviour.isDefined must beTrue

      val thirdTime: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_HOST", sharedLockOnHost = true)
      thirdTime.isDefined must beTrue

      // lock expiry time is also extended.
      thirdTime.get.createdAt.isAfter(lockOne.get.createdAt)

      // if hosts do not match, do not provide a lock
      val alternateHostCheck: Option[Lock] = repository.obtainLock("SOME_LOCK", "SOME_OTHER_HOST", sharedLockOnHost = true)
      alternateHostCheck.isDefined must beFalse
    }
  }
}
