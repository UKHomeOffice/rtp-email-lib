package uk.gov.homeoffice.domain.core.lock

import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ProcessLockingSpec extends Specification with Mockito {
  trait Context extends Scope {
    val processLockRepository = mock[ProcessLockRepository]
  }
  
  "ProcessLocking" should {
    "run the function with a given lock" in new Context {
      val lock = Lock("SOME_LOCK", "SOME_OTHER_HOST")
      processLockRepository.obtainLock(any[String], any[String]) returns Some(lock)
      processLockRepository.releaseLock(any[Lock]) returns true

      val result = processLockRepository.obtainLock("xxx", "xxx").map { l => (l, 10) }
      result.map(_._2) mustEqual Some(10)
      processLockRepository.releaseLock(result.get._1) mustEqual true
    }

    "not run the function if it can't get the lock" in new Context {
      processLockRepository.obtainLock(any[String], any[String]) returns None

      val result = processLockRepository.obtainLock("xxx", "xxx").map { _ => 10 }
      result mustEqual None

    }
  }
}
