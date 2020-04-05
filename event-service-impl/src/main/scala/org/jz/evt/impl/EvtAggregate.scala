package org.jz.evt.impl

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect

import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry

import java.util.UUID

import org.jz.evt.api.Commands._
import org.jz.evt.api.Events._

import play.api.libs.json.Format
import play.api.libs.json.Json

import scala.collection.immutable.Seq
import java.time.ZonedDateTime
import play.api.libs.json.Reads
import play.api.libs.json.JsValue
import play.api.libs.json.JsSuccess
import play.api.libs.json.Writes
import org.jz.evt.api.CommandResponses._
object EvtBehavior {

  def create(entityContext: EntityContext[EvtCommand]): Behavior[EvtCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    create(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, EvtEvent.Tag)
      )
  }

  private[impl] def create(persistenceId: PersistenceId) =
    EventSourcedBehavior
      .withEnforcedReplies[EvtCommand, EvtEvent, EvtState](
        persistenceId = persistenceId,
        emptyState = EvtState.initial,
        commandHandler = (state, cmd) => state.applyCommand(cmd),
        eventHandler = (state, evt) => state.applyEvent(evt)
      )
}

case class EvtState(
    name: String,
    availableTickets: Int,
    reservations: Map[UUID, Reservation],
    canceled: Boolean
) {
  def applyCommand(cmd: EvtCommand): ReplyEffect[EvtEvent, EvtState] =
    cmd match {
      case x: Create                => onCreate(x)
      case x: CreateReservation     => onCreateReservation(x)
      case x: ExtendReservationTime => onExtendReservationTime(x)
      case x: CancelReservation     => onCancelReservation(x)
    }

  private def onCreate(cmd: Create): ReplyEffect[EvtEvent, EvtState] = {
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

  private def onCreateReservation(cmd: CreateReservation): ReplyEffect[EvtEvent, EvtState] = {
    val now: ZonedDateTime = ZonedDateTime.now()
    if (cmd.reservationTime.isBefore(now)) {
      Effect.reply(cmd.replyTo)(Rejected("Reservation time cannot be set in past."))
    } else if (cmd.ticketsCount <= 0) {
      Effect.reply(cmd.replyTo)(Rejected("Number of reserved tickets cannot be less than 0"))
    } else {
      Effect
        .persist(ReservationCreatedEvent(cmd.id, cmd.evtId, cmd.userId, cmd.ticketsCount, cmd.reservationTime))
        .thenReply(cmd.replyTo) { _ =>
          ReservationCreated(cmd.id, cmd.evtId, cmd.userId, cmd.ticketsCount, cmd.reservationTime)
        }
    }
  }

  private def onExtendReservationTime(cmd: ExtendReservationTime): ReplyEffect[EvtEvent, EvtState] = {
    ???
  }

  private def onCancelReservation(cmd: CancelReservation): ReplyEffect[EvtEvent, EvtState] = {
    ???
  }

  def applyEvent(event: EvtEvent): EvtState = {
    event match {
      case x: EvtCreatedEvent => createEvent(x)
    }
  }

  private def createEvent(event: EvtCreatedEvent): EvtState = {
    copy(
      name = event.name,
      availableTickets = event.availableTickets,
      reservations = Map.empty,
      canceled = false
    )
  }

}

object Reservation {
  implicit val format: Format[Reservation] = Json.format
}

// TODO do we need id here?
final case class Reservation(id: UUID, userId: UUID, ticketsCnt: Int, reservationTime: ZonedDateTime)

object EvtState {
  def initial: EvtState =
    EvtState(
      name = "",
      availableTickets = 0,
      reservations = Map.empty,
      canceled = false
    )

  val typeKey = EntityTypeKey[EvtCommand]("EvtAggregate")

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

  implicit val format: Format[EvtState] = Json.format
}

// TODO move to separate file
object EvtSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[EvtCreated],
    JsonSerializer[ReservationCanceled],
    JsonSerializer[ReservationCreated],
    JsonSerializer[ReservationTimeExtended],
    JsonSerializer[CommandResponse],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected],
    JsonSerializer[EvtCreatedEvent],
    JsonSerializer[ReservationCanceledEvent],
    JsonSerializer[ReservationCreatedEvent],
    JsonSerializer[ReservationTimeExtendedEvent],
  )
}
