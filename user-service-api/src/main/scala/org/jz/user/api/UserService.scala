package org.jz.user.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object UserService {
  val TOPIC_NAME = "users"
}

trait UserService extends Service {

  /**
   *  Create a new user.
   *
   *  Example: curl -v -H "Content-Type: application/json" -d '{"name": "Jon Kilroy"}' -X POST http://localhost:9000/user | jq .
   *
   */
  def createUser(): ServiceCall[UserData, UserView]

  /**
   * Get a user.
   *
   * Example: curl -v http://localhost:9000/user/4db6ed98-a68f-4a54-9d6e-8aecf57a7684 | jq .
   *
   */
  def getUser(userId: UUID): ServiceCall[NotUsed, UserView]

  /**
   * Add number of reserved tickets (can be less than 0).
   *
   * Example: curl -v -H "Content-Type: application/json" -d '{"cnt": 4}' -X PATCH http://localhost:9000/user/4db6ed98-a68f-4a54-9d6e-8aecf57a7684 | jq .
   *
   */
  def addTickets(id: UUID): ServiceCall[Quantity, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("user-service")
      .withCalls(
        restCall(Method.POST, "/user", createUser _),
        restCall(Method.GET, "/user/:id", getUser _),
        restCall(Method.PATCH, "/user/:id", addTickets _)
      )
      .withAutoAcl(true)
  }
}

object UserView {
  implicit val format: Format[UserView] = Json.format
}

final case class UserView(id: UUID, name: String, reservedTicketsCnt: Int, maxReservedTicketsCnt: Int) {
  def adjustNumberOfReservedTickets(cnt: Int): UserView = {
    copy(reservedTicketsCnt = reservedTicketsCnt + cnt)
  }
}

object Quantity {
  implicit val format: Format[Quantity] = Json.format
}

final case class Quantity(cnt: Int)

object UserData {
  implicit val format: Format[UserData] = Json.format
}

final case class UserData(name: String)
