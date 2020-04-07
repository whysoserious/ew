package org.jz.user.impl

import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import Events._

class UserViewProcessor(readSide: SlickReadSide, repository: UserViewRepository) extends ReadSideProcessor[UserEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[UserEvent] =
    readSide
      .builder[UserEvent]("user-view")
      .setGlobalPrepare(repository.createTable())
      .setEventHandler[UserCreatedEvent] { envelope =>
        repository.createUserView(envelope.event)
      }
      .setEventHandler[ReservedTicketsCntAdjustedEvent] { envelope =>
        repository.adjustNumberOfReservedTickets(envelope.event)
      }
      .build()

  override def aggregateTags: Set[AggregateEventTag[UserEvent]] = UserEvent.Tag.allTags
}
