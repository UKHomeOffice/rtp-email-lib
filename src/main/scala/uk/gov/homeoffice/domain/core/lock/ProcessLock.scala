package uk.gov.homeoffice.domain.core.lock

import java.net.InetAddress
import com.mongodb.DBObject
import com.mongodb.casbah.ReadPreference
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.Minutes.minutesBetween
import grizzled.slf4j.Logging
import uk.gov.homeoffice.domain.core.lock.ProcessLockRepository.EXPIRY_PERIOD_MINS
import uk.gov.homeoffice.mongo.salat.Repository

case class Lock(_id: ObjectId, name: String, host: String, createdAt: DateTime) {
  def toDbObject: DBObject = {
    val builder = MongoDBObject.newBuilder
    builder += "_id" -> _id
    builder += "name" -> name
    builder += "host" -> host
    builder += "createdAt" -> createdAt
    builder.result()
  }
}

object Lock {
  def apply(dbObject: DBObject): Lock = Lock(
    dbObject.get("_id").asInstanceOf[ObjectId],
    dbObject.get("name").asInstanceOf[String],
    dbObject.get("host").asInstanceOf[String],
    dbObject.get("createdAt").asInstanceOf[DateTime]
  )

  def apply(name: String, host: String, createdAt: DateTime = DateTime.now()): Lock =
    Lock(new ObjectId(), name, host, createdAt)
}

object ProcessLockRepository {
  val EXPIRY_PERIOD_MINS = 10
}

trait ProcessLockRepository extends Repository[Lock] with Logging {
  val collectionName = "locks"

  dao.collection.createIndex(MongoDBObject("name" -> 1), MongoDBObject("name" -> "lockNameIdx", "unique" -> true))

  private def newLock(name: String, host: String): Option[Lock] = try {
    val lock = Lock(name = name, host = host, createdAt = DateTime.now)
    insert(lock)
    info(s"Lock : $name acquired by host : $host")
    Some(lock)
  } catch {
    case e: Throwable =>
      warn(s"error creating new lock: $name from host: $host. continuing .. ", e)
      None
  }

  def obtainLock(name: String, host: String): Option[Lock] = try {
    findOne(MongoDBObject("name" -> name), ReadPreference.primaryPreferred).fold(newLock(name, host)) { l =>
      if (minutesBetween(l.createdAt, DateTime.now).getMinutes >= EXPIRY_PERIOD_MINS) {
        releaseLock(l)
        newLock(name, host)
      } else None
    }
  } catch {
    case e: Throwable =>
      warn(s"error obtaining lock: $name from host: $host. continuing .. ", e)
      None
  }

  def releaseLock(lock: Lock): Boolean = try {
    val result = remove(lock)
    info(s"Lock : ${lock.name} released by host : ${lock.host}")
    result.getN == 1
  } catch {
    case e: Throwable =>
      warn(s"error releasing lock: ${lock.name} from host: ${lock.host}. continuing .. ", e)
      false
  }
}

trait ProcessLocking {
  val processLockRepository: ProcessLockRepository

  lazy val host = InetAddress.getLocalHost.getHostName

  def withLock[T](lockName: String)(f: => T) = processLockRepository.obtainLock(lockName, host).map { lock =>
    try {
      f
    } finally {
      processLockRepository.releaseLock(lock)
    }
  }
}