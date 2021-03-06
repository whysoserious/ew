package org.jz.evt.impl

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.typed.ActorRef
import org.jz.evt.impl.CommandResponses._

trait EvtCommandSerializable

object Commands {

  sealed trait EvtCommand extends EvtCommandSerializable

  final case class Create(
      id: UUID,
      name: String,
      availableTickets: Int,
      replyTo: ActorRef[EvtCreatedResponse]
  ) extends EvtCommand

  final case class CreateReservation(
      id: UUID,
      evtId: UUID,
      userId: UUID,
      ticketsCount: Int,
      reservationTime: ZonedDateTime,
      replyTo: ActorRef[ReservationCreatedResponse]
  ) extends EvtCommand

  final case class ExtendReservationTime(
      id: UUID,
      evtId: UUID,
      newReservationTime: ZonedDateTime,
      replyTo: ActorRef[Nothing]
  ) extends EvtCommand

  final case class CancelReservation(id: UUID, evtId: UUID, replyTo: ActorRef[Nothing]) extends EvtCommand

}
