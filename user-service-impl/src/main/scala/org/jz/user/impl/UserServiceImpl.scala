package org.jz.user.impl

import java.util.UUID

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import org.jz.user.api.{Quantity, UserData, UserService, UserView}
import org.jz.user.impl.CommandResponses.{Accepted, Confirmation, Rejected, UserCreated}
import org.jz.user.impl.Commands._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class UserServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    userViewRepository: UserViewRepository
)(implicit ec: ExecutionContext)
    extends UserService {

  private def aggregateRef(id: UUID): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserAggregate.typeKey, id.toString)

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def createUser(): ServiceCall[UserData, UserView] = ServiceCall { userData =>
    val newUserId = UUID.randomUUID()
    aggregateRef(newUserId)
      .ask[UserCreated](replyTo => Create(id = newUserId, name = userData.name, replyTo = replyTo))
      .map(
        userCreated =>
          UserView(newUserId, userCreated.name, userCreated.reservedTicketsCnt, userCreated.maxReservedTicketsCnt)
      )
  }

  // TODO In case of invalid request and Exception is thrown. This is exception is converted to a
  // HTTP 404 response. Is there more functional way of doing this in Lagom?
  override def addTickets(id: UUID): ServiceCall[Quantity, Done] = ServiceCall { quantity =>
    aggregateRef(id)
      .ask[Confirmation](
        replyTo => AdjustReservedTicketsCnt(id, quantity.cnt, replyTo)
      )
      .map {
        case Accepted         => Done
        case Rejected(reason) => throw BadRequest(reason)
      }
  }

  override def getUser(userId: UUID): ServiceCall[NotUsed, UserView] = ServiceCall { _ =>
    userViewRepository.findById(userId).map {
      case Some(userView) => userView
      case None           => throw NotFound(s"User with id $userId does not exist")
    }

  }

}
