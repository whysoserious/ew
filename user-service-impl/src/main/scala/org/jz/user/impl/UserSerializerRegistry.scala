package org.jz.user.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import Events.{ReservedTicketsCntAdjustedEvent, UserCreatedEvent}
import org.jz.user.impl.CommandResponses.{Accepted, Confirmation, Rejected, UserCreated}

import scala.collection.immutable.Seq

object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[UserCreatedEvent],
    JsonSerializer[ReservedTicketsCntAdjustedEvent],
    JsonSerializer[UserCreated],
    JsonSerializer[Confirmation],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected]
  )
}
