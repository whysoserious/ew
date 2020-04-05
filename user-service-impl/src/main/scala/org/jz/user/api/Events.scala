package org.jz.user.api

import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json

object Events {

  object UserEvent {
    val Tag: AggregateEventTag[UserEvent] = AggregateEventTag[UserEvent]
  }
  sealed trait UserEvent extends AggregateEvent[UserEvent] {
    override def aggregateTag: AggregateEventTag[UserEvent] = UserEvent.Tag
  }

  // TODO do we need id in events
  case class UserCreatedEvent(id: UUID, name: String, reservedTicketsCnt: Int = 0, maxReservedTicketsCnt: Int = 10)
      extends UserEvent
  object UserCreatedEvent {
    implicit val format: Format[UserCreatedEvent] = Json.format
  }

  case class ReservedTicketsCntAdjustedEvent(id: UUID, cnt: Int) extends UserEvent
  object ReservedTicketsCntAdjustedEvent {
    implicit val format: Format[ReservedTicketsCntAdjustedEvent] = Json.format

  }

}
