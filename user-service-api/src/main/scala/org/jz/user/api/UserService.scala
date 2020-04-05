package org.jz.user.api

import akka.Done
import akka.NotUsed

import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.Method

import java.util.UUID
import play.api.libs.json.Format
import play.api.libs.json.Json

object UserService {
  val TOPIC_NAME = "users"
}

trait UserService extends Service {

  /**
   *  Create a new user.
   *
   *  Example: curl -H "Content-Type: application/json" -d '{"name": "Jon Kilroy"}' -X POST http://localhost:9000/user
   *
   */
  def createUser(): ServiceCall[UserData, UserView]

  /**
   * Get a user.
   *
   * Example: curl http://localhost:9000/user/2ebc279a-1ad1-45fe-9553-c0a3687d10c4
   *
   */
//TODO userId
  def getUser(id: UUID): ServiceCall[NotUsed, UserView]

  /**
   * Add number of reserved tickets (can be less than 0).
   *
   * Example: curl -H "Content-Type: application/json" -d '{"cnt": 4}' -X PATCH http://localhost:9000/user/2ebc279a-1ad1-45fe-9553-c0a3687d10c4
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

// TODO remove
object UserViewResult {
  implicit val format: Format[UserViewResult] = Json.format
}

final case class UserViewResult(content: Option[UserView])

object UserView {
  implicit val format: Format[UserView] = Json.format
}

// TODO maxReservedTicketsCnt should be defined somewhere else
// TODO get rid of default values
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
