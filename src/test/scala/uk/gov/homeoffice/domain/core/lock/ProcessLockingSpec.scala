package uk.gov.homeoffice.domain.core.lock

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ProcessLockingSpec extends Specification with Mockito {

  object ProcessLocking extends ProcessLocking {
    override val processLockRepository = mock[ProcessLockRepository]
  }
  sequential

  "ProcessLocking" should {
    import ProcessLocking._
    "run the function with a given lock" in {
      //Given
      val lock = Lock("SOME_LOCK", "SOME_OTHER_HOST")
      processLockRepository.obtainLock(any[String], any[String]) returns Some(lock)
      processLockRepository.releaseLock(any[Lock]) returns true

      //When
      val result = withLock("SOME_LOCK") {10}

      //Then
      result mustEqual Some(10)
    }

    "not run the function if it can't get the lock" in {
      //Given
      processLockRepository.obtainLock(any[String], any[String]) returns None

      //When
      val result = withLock("SOME_LOCK") {10}

      //Then
      result mustEqual None
    }
  }

}
