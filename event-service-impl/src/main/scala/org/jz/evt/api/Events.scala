package org.jz.evt.api

import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json
import java.time.ZonedDateTime

// TODO use only these classes?
object Events {

  object EvtEvent {
    val Tag: AggregateEventTag[EvtEvent] = AggregateEventTag[EvtEvent]
  }
  sealed trait EvtEvent extends AggregateEvent[EvtEvent] {
    override def aggregateTag: AggregateEventTag[EvtEvent] = EvtEvent.Tag
  }

  object EvtCreatedEvent {
    implicit val format: Format[EvtCreatedEvent] = Json.format
  }
  final case class EvtCreatedEvent(id: UUID, name: String, availableTickets: Int) extends EvtEvent

  object ReservationCreatedEvent {
    implicit val format: Format[ReservationCreatedEvent] = Json.format
  }

  final case class ReservationCreatedEvent(
      id: UUID,
      evtId: UUID,
      userId: UUID,
      ticketsCount: Int,
      reservationTime: ZonedDateTime
  )

  object ReservationTimeExtendedEvent {
    implicit val format: Format[ReservationTimeExtendedEvent] = Json.format
  }

  final case class ReservationTimeExtendedEvent(
      id: UUID,
      evtId: UUID,
      userId: UUID,
      ticketsCount: Int,
      newReservationTime: ZonedDateTime
  )

  object ReservationCanceledEvent {
    implicit val format: Format[ReservationCanceledEvent] = Json.format
  }

  final case class ReservationCanceledEvent(id: UUID, evtId: UUID)

}
