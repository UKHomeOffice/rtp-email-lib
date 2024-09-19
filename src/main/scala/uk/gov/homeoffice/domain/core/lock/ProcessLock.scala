package uk.gov.homeoffice.domain.core.lock

import java.net.InetAddress

import grizzled.slf4j.Logging
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.Minutes.minutesBetween
import uk.gov.homeoffice.domain.core.lock.ProcessLockRepository.EXPIRY_PERIOD_MINS
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.homeoffice.mongo._
import uk.gov.homeoffice.mongo.model._
import uk.gov.homeoffice.mongo.model.syntax._
import uk.gov.homeoffice.mongo.repository._
import uk.gov.homeoffice.mongo.casbah._
import uk.gov.homeoffice.mongo.casbah.syntax._

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import uk.gov.homeoffice.mongo.MongoJsonEncoders.DatabaseEncoding._

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
  def apply(dbObject: DBObject): Lock =
    Lock(
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

class ProcessLockRepository(mongoConnection :MongoConnection) extends Logging with CasbahPassthrough[Lock] {
  val collectionName = "locks"

  val collection =
    new MongoCasbahSalatRepository[Lock](
      new MongoCasbahRepository(
        new MongoJsonRepository(
          new MongoStreamRepository(mongoConnection, collectionName="locks", primaryKeys=List("name"))
        )
      )
    ) {

    def toMongoDBObject(a :Lock) :MongoResult[MongoDBObject] = Right(a.toDbObject.mongoDBObject)
    def fromMongoDBObject(mongoDBObject :MongoDBObject) :MongoResult[Lock] = Right(Lock(mongoDBObject))
  }

  def initialise() :Unit = {
    collection.mongoCasbahRepository.ensureUniqueIndex(
      collection.mongoCasbahRepository.mongoJsonRepository.mongoStreamRepository.collection,
      "lockNameIdx",
      List("name")
    )
  }

  initialise()

  private def newLock(name: String, host: String): Option[Lock] = try {
    val lock = Lock(name = name, host = host, createdAt = DateTime.now)
    collection.insert(lock)
    //println(s"Lock : $name acquired by host : $host") // TODO: reinstate debug logging
    Some(lock)
  } catch {
    case _: MongoException =>
      debug(s"duplicate key error creating new lock: $name from host: $host. continuing ..")
      None
    case t: Throwable =>
      warn(s"error creating new lock: $name from host: $host. continuing .. ", t)
      None
  }

  def obtainLock(name: String, host: String): Option[Lock] = try {
    collection.findOne(MongoDBObject("name" -> name)) match {
      case Some(l) => if (minutesBetween(l.createdAt, DateTime.now).getMinutes >= EXPIRY_PERIOD_MINS) {
        if (releaseLock(l)) newLock(name, host) else None
      } else None

      case None => newLock(name, host)
    }
  } catch {
    case e: Throwable =>
      warn(s"error obtaining lock: $name from host: $host. continuing .. ", e)
      None
  }

  def releaseLock(lock: Lock): Boolean = try {
    val result = collection.remove(lock.toDbObject.mongoDBObject)
    debug(s"Lock : ${lock.name} released by host : ${lock.host}")
    result.getN == 1
  } catch {
    case e: Throwable =>
      warn(s"error releasing lock: ${lock.name} from host: ${lock.host}. continuing .. ", e)
      false
  }
}

trait ProcessLock extends ProcessLockRepository {
  lazy val host = InetAddress.getLocalHost.getHostName

  def withLock[T](lockName: String)(f: => T) :Option[T] = obtainLock(lockName, host).map { lock =>
    try {
      f
    } finally {
      releaseLock(lock)
    }
  }

  def withLockF[T](lockName :String)(f: => Future[T])(implicit ec :ExecutionContext) :Future[Option[T]] =
    obtainLock(lockName, host) match {
      case Some(lock) =>
        f.map { result =>
          releaseLock(lock)
          Some(result)
        }.recoverWith {
          case t :Throwable =>
            releaseLock(lock)
            Future.failed(t)
        }
      case None => Future.successful(None)
  }
}
