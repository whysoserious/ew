package org.jz.user.impl

import akka.Done
import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import java.util.UUID

import org.jz.user.api.Commands._
import org.jz.user.api.Quantity
import org.jz.user.api.UserData
import org.jz.user.api.UserService
import org.jz.user.api.UserView
import org.jz.user.api.UserViewResult

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import com.lightbend.lagom.scaladsl.api.transport.NotFound

class UserServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext)
    extends UserService {

  private def aggregateRef(id: UUID): EntityRef[UserCommand] =
    clusterSharding.entityRefFor(UserState.typeKey, id.toString)

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
    Future {
      UserViewRepository.getCatalog(userId) match {
        case Some(userView) => userView
        case None           => throw NotFound(s"User with id $userId does not exist")
      }
    }
  }

}
