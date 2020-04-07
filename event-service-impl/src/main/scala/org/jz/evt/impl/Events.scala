package org.jz.evt.impl

import java.time.ZonedDateTime
import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag, AggregateEventTagger}
import play.api.libs.json.{Format, Json}

object Events {

  object EvtEvent {
    val Tag: AggregateEventShards[EvtEvent] = AggregateEventTag.sharded[EvtEvent](numShards = 10)
  }
  sealed trait EvtEvent extends AggregateEvent[EvtEvent] {
    override def aggregateTag: AggregateEventTagger[EvtEvent] = EvtEvent.Tag
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
  ) extends EvtEvent

  object ReservationTimeExtendedEvent {
    implicit val format: Format[ReservationTimeExtendedEvent] = Json.format
  }

  final case class ReservationTimeExtendedEvent(
      id: UUID,
      evtId: UUID,
      userId: UUID,
      ticketsCount: Int,
      newReservationTime: ZonedDateTime
  ) extends EvtEvent

  object ReservationCanceledEvent {
    implicit val format: Format[ReservationCanceledEvent] = Json.format
  }

  final case class ReservationCanceledEvent(id: UUID, evtId: UUID) extends EvtEvent

}
