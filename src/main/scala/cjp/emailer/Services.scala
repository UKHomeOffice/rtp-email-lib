package cjp.emailer

import io.finch._
import com.twitter.finagle.Service

case class User(id: Long, name: String)

case class Ticket(id: Long)

case class GetUser(userId: Long) extends Service[HttpRequest, User] {
  def apply(req: HttpRequest) = User(userId, "John").toFuture
}

case class GetTicket(ticketId: Long) extends Service[HttpRequest, Ticket] {
  def apply(req: HttpRequest) = Ticket(ticketId).toFuture
}

case class GetUserTickets(userId: Long) extends Service[HttpRequest, Seq[Ticket]] {
  def apply(req: HttpRequest) = Seq(Ticket(1), Ticket(2), Ticket(3)).toFuture
}