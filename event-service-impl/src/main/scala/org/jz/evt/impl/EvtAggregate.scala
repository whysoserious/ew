package org.jz.evt.impl

import java.time.ZonedDateTime
import java.util.UUID

import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import CommandResponses._
import Commands._
import Events._
import org.jz.evt.impl.EvtAggregate.Reservation
import play.api.libs.json._


object EvtAggregate {
  object Reservation {
    implicit val format: Format[Reservation] = Json.format
  }

  final case class Reservation(id: UUID, userId: UUID, ticketsCnt: Int, reservationTime: ZonedDateTime)
  def initial: EvtAggregate =
    EvtAggregate(
      name = "",
      availableTickets = 0,
      reservations = Map.empty,
      canceled = false
    )

  val typeKey: EntityTypeKey[EvtCommand] = EntityTypeKey[EvtCommand]("EvtAggregate")

  // TODO Can I avoid writing all these mappings in whole project?
  implicit val mapReads: Reads[Map[UUID, Reservation]] = (jv: JsValue) =>
    JsSuccess(jv.as[Map[UUID, Reservation]].map {
      case (k, v) =>
        k -> v
    })

  implicit val mapWrites: Writes[Map[UUID, Reservation]] = (map: Map[UUID, Reservation]) =>
    Json.toJson(map.map {
      case (s, o) =>
        s.toString -> o
    })

  implicit val jsonMapFormat: Format[Map[UUID, Reservation]] = Format(mapReads, mapWrites)

  implicit val format: Format[EvtAggregate] = Json.format
}

case class EvtAggregate(
    name: String,
    availableTickets: Int,
    reservations: Map[UUID, Reservation],
    canceled: Boolean
) {
  def applyCommand(cmd: EvtCommand): ReplyEffect[EvtEvent, EvtAggregate] =
    cmd match {
      case x: Create                => onCreate(x)
      case x: CreateReservation     => onCreateReservation(x)
      case x: ExtendReservationTime => onExtendReservationTime(x)
      case x: CancelReservation     => onCancelReservation(x)
    }

  private def onCreate(cmd: Create): ReplyEffect[EvtEvent, EvtAggregate] = {
    if (cmd.availableTickets > 0) {
      Effect
        .persist(EvtCreatedEvent(cmd.id, cmd.name, cmd.availableTickets))
        .thenReply(cmd.replyTo) { _ =>
          EvtCreated(cmd.id, cmd.name, cmd.availableTickets)
        }
    } else {
      Effect.reply(cmd.replyTo)(Rejected("Number of reserved tickets cannot be less than 0"))
    }
  }

  private def onCreateReservation(cmd: CreateReservation): ReplyEffect[EvtEvent, EvtAggregate] = {
    // TODO this date should be read from the CreateReservation object!
    // This function has to be pure (think of the situation when all events are replayed)
    val now: ZonedDateTime = ZonedDateTime.now()
    if (cmd.reservationTime.isBefore(now)) {
      Effect.reply(cmd.replyTo)(Rejected("Reservation time cannot be set in past."))
    } else if (cmd.ticketsCount <= 0) {
      Effect.reply(cmd.replyTo)(Rejected("Number of reserved tickets cannot be less or equal to 0"))
    } else if (availableTickets - cmd.ticketsCount < 0) {
      Effect.reply(cmd.replyTo)(Rejected("Number of reserved tickets is too big"))
    } else {
      Effect
        .persist(ReservationCreatedEvent(cmd.id, cmd.evtId, cmd.userId, cmd.ticketsCount, cmd.reservationTime))
        .thenReply(cmd.replyTo) { _ =>
          ReservationCreated(cmd.id, cmd.evtId, cmd.userId, cmd.ticketsCount, cmd.reservationTime, canceled = false)
        }
    }
  }

  private def onExtendReservationTime(cmd: ExtendReservationTime): ReplyEffect[EvtEvent, EvtAggregate] = {
    ???
  }

  private def onCancelReservation(cmd: CancelReservation): ReplyEffect[EvtEvent, EvtAggregate] = {
    ???
  }

  def applyEvent(event: EvtEvent): EvtAggregate = {
    event match {
      case x: EvtCreatedEvent         => createEvent(x)
      case x: ReservationCreatedEvent => createReservation(x)
      case _ => ???
    }
  }

  private def createEvent(event: EvtCreatedEvent): EvtAggregate = {
    copy(
      name = event.name,
      availableTickets = event.availableTickets,
      reservations = Map.empty,
      canceled = false
    )
  }

  private def createReservation(event: ReservationCreatedEvent): EvtAggregate = {
    val reservation = Reservation(event.id, event.userId, event.ticketsCount, event.reservationTime)
    copy(
      availableTickets = availableTickets - event.ticketsCount,
      reservations = reservations + (event.userId -> reservation)
    )
  }

}






