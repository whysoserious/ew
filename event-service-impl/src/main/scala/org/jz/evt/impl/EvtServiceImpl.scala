package org.jz.evt.impl

import java.util.UUID

import akka.Done
import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import org.jz.evt.api.EvtData
import org.jz.evt.api.EvtViewResult
import org.jz.evt.api.ReservationData
import org.jz.evt.api.ReservationViewResult
import org.jz.evt.api.ReservationTime
import org.jz.evt.api.Commands._
import org.jz.evt.api.CommandResponses._
import org.jz.evt.api.EvtService
import org.jz.evt.api.EvtView
import akka.cluster.ddata.protobuf.msg.ReplicatorMessages.NotFoundOrBuilder
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import org.jz.user.api.UserService
import org.jz.evt.api.ReservationView
import org.jz.evt.api.Events.ReservationCreatedEvent
import org.jz.user.api.UserView

class EvtServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    userService: UserService
)(implicit ec: ExecutionContext)
    extends EvtService {

  private def aggregateRef(id: UUID): EntityRef[EvtCommand] =
    clusterSharding.entityRefFor(EvtState.typeKey, id.toString)

  implicit val timeout: Timeout = Timeout(5.seconds)

  def createEvt(): ServiceCall[EvtData, EvtView] = ServiceCall { evtData =>
    for {
      evtCreatedResponse <- cmdCreateEvt(evtData)
    } yield {
      evtCreatedResponse match {
        case EvtCreated(id, name, availableTickets) => EvtView(id, name, availableTickets, List.empty)
        case Rejected(reason)                       => throw BadRequest(reason)
      }
    }
  }

  def getEvt(evtId: UUID): ServiceCall[NotUsed, EvtView] = ServiceCall { _ =>
    Future {
      EvtViewRepository.getCatalog(evtId) match {
        case Some(evtView) => evtView
        case None          => throw NotFound(s"Event with id $evtId does not exist")
      }
    }
  }

  // TODO Reservations should sit in a separate service
  def createReservation(evtId: UUID): ServiceCall[ReservationData, ReservationView] = ServiceCall { reservationData =>
    for {
      userView                   <- getUser(reservationData.userId)
      reservationCreatedResponse <- cmdCreateReservation(evtId, userView.id, reservationData)
    } yield {
      reservationCreatedResponse match {
        case ReservationCreated(_, _, userId, ticketsCount, reservationTime, canceled) =>
          ReservationView(userId, ticketsCount, reservationTime, canceled)
        case Rejected(reason) => throw BadRequest(reason)
      }
    }
  }

  def extendReservationTime(evtId: UUID, reservationId: UUID): ServiceCall[ReservationTime, Done] = ???

  def cancelReservation(evtId: UUID, reservationId: UUID): ServiceCall[NotUsed, Done] = ???

  private def cmdCreateEvt(evtData: EvtData): Future[EvtCreatedResponse] = {
    val newEvtId = UUID.randomUUID()
    aggregateRef(newEvtId)
      .ask[EvtCreatedResponse](replyTo => Create(newEvtId, evtData.name, evtData.availableTickets, replyTo))
  }

  private def getUser(userId: UUID): Future[UserView] = {
    userService.getUser(userId).invoke()
  }

  private def cmdCreateReservation(
      evtId: UUID,
      userId: UUID,
      reservationData: ReservationData
  ): Future[ReservationCreatedResponse] = {
    aggregateRef(evtId)
      .ask[ReservationCreatedResponse](
        replyTo =>
          CreateReservation(
            UUID.randomUUID(),
            evtId,
            userId,
            reservationData.ticketsCnt,
            reservationData.reservationTime,
            replyTo
          )
      )
  }

}
