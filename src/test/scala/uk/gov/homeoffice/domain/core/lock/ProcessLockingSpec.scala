package uk.gov.homeoffice.domain.core.lock

import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ProcessLockingSpec extends Specification with Mockito {
  trait Context extends Scope with ProcessLocking {
    override val processLockRepository = mock[ProcessLockRepository]
  }
  
  "ProcessLocking" should {
    "run the function with a given lock" in new Context {
      val lock = Lock("SOME_LOCK", "SOME_OTHER_HOST")
      processLockRepository.obtainLock(any[String], any[String]) returns Some(lock)
      processLockRepository.releaseLock(any[Lock]) returns true

      val result = withLock("SOME_LOCK") {10}

      result mustEqual Some(10)
    }

    "not run the function if it can't get the lock" in new Context {
      processLockRepository.obtainLock(any[String], any[String]) returns None

      val result = withLock("SOME_LOCK") {10}

      result mustEqual None
    }
  }
}