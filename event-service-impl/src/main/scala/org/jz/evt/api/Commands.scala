package org.jz.evt.api

import akka.actor.typed.ActorRef

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import java.time.ZonedDateTime
import org.jz.evt.api.CommandResponses._

trait EvtCommandSerializable

object Commands {

  // Commands
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
