package cjp.emailer

import com.twitter.finagle.http.Request
import com.twitter.util._
import com.twitter.finagle.Service

case class User(id: Long, name: String)

case class Ticket(id: Long)

case class GetUser(userId: Long) extends Service[Request, User] {
  def apply(req: Request) = Future.value(User(userId, "John"))
}

case class GetTicket(ticketId: Long) extends Service[Request, Ticket] {
  def apply(req: Request) = Future.value(Ticket(ticketId))
}

case class GetUserTickets(userId: Long) extends Service[Request, Seq[Ticket]] {
  def apply(req: Request) = Future.value(Seq(Ticket(1), Ticket(2), Ticket(3)))
}