package org.jz.evt.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import CommandResponses._
import Events.{EvtCreatedEvent, ReservationCanceledEvent, ReservationCreatedEvent, ReservationTimeExtendedEvent}

import scala.collection.immutable.Seq

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
