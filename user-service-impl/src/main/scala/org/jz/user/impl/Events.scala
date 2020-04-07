package org.jz.user.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventShards
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger
import play.api.libs.json.Format
import play.api.libs.json.Json

object Events {

  object UserEvent {
    val Tag: AggregateEventShards[UserEvent] = AggregateEventTag.sharded[UserEvent](numShards = 10)
  }
  sealed trait UserEvent extends AggregateEvent[UserEvent] {
    override def aggregateTag: AggregateEventTagger[UserEvent] = UserEvent.Tag
  }

  object UserCreatedEvent {
    implicit val format: Format[UserCreatedEvent] = Json.format
  }
  final case class UserCreatedEvent(
      id: UUID,
      name: String,
      reservedTicketsCnt: Int = 0,
      maxReservedTicketsCnt: Int = 10
  ) extends UserEvent

  object ReservedTicketsCntAdjustedEvent {
    implicit val format: Format[ReservedTicketsCntAdjustedEvent] = Json.format
  }
  case class ReservedTicketsCntAdjustedEvent(id: UUID, cnt: Int) extends UserEvent

}
