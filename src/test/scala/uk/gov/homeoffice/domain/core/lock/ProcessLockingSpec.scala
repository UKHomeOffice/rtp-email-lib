package uk.gov.homeoffice.domain.core.lock

import org.specs2.matcher.Scope
import org.specs2.mutable.Specification
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

class ProcessLockingSpec extends Specification {
  trait Context extends Scope {
    val processLockRepository = mock(classOf[ProcessLockRepository])
  }
  
  "ProcessLocking" should {
    "run the function with a given lock" in new Context {
      val lock = Lock("SOME_LOCK", "SOME_OTHER_HOST")
      when(processLockRepository.obtainLock(any, any)).thenReturn(Some(lock))
      when(processLockRepository.releaseLock(any(classOf[Lock]))).thenReturn(true)

      val result = processLockRepository.obtainLock("xxx", "xxx").map { l => (l, 10) }
      result.map(_._2) mustEqual Some(10)
      processLockRepository.releaseLock(result.get._1) mustEqual true
    }

    "not run the function if it can't get the lock" in new Context {
      when(processLockRepository.obtainLock(any, any)).thenReturn(None)

      val result = processLockRepository.obtainLock("xxx", "xxx").map { _ => 10 }
      result mustEqual None

    }
  }
}
