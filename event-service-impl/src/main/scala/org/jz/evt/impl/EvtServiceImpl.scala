package org.jz.evt.impl

import java.util.UUID

import akka.Done
import akka.NotUsed
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.util.Timeout
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import CommandResponses._
import Commands._
import org.jz.evt.api._
import org.jz.user.api.Quantity
import org.jz.user.api.UserService
import org.jz.user.api.UserView

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

class EvtServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    userService: UserService,
    evtViewRepository: EvtViewRepository
)(implicit ec: ExecutionContext)
    extends EvtService {

  private def aggregateRef(id: UUID): EntityRef[EvtCommand] =
    clusterSharding.entityRefFor(EvtAggregate.typeKey, id.toString)

  implicit val timeout: Timeout = Timeout(5.seconds)

  def createEvt(): ServiceCall[EvtData, EvtView] = ServiceCall { evtData =>
    for {
      evtCreatedResponse <- cmdCreateEvt(evtData)
    } yield {
      evtCreatedResponse match {
        case EvtCreated(id, name, availableTickets) => EvtView(id, name, availableTickets)
        case Rejected(reason)                       => throw BadRequest(reason)
      }
    }
  }

  def getEvt(evtId: UUID): ServiceCall[NotUsed, EvtView] = ServiceCall { _ =>
    evtViewRepository.findById(evtId).map {
      case Some(evtView) => evtView
      case None          => throw NotFound(s"Event with id $evtId does not exist")
    }
  }

  def createReservation(evtId: UUID): ServiceCall[ReservationData, ReservationView] = ServiceCall { reservationData =>
    for {
      _                          <- userService.addTickets(reservationData.userId).invoke(Quantity(reservationData.ticketsCnt))
      reservationCreatedResponse <- cmdCreateReservation(evtId, reservationData.userId, reservationData)
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
