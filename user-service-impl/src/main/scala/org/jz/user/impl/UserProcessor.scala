package org.jz.user.impl

import akka.Done
import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Flow

import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor.ReadSideHandler

import org.jz.user.api.Events.UserEvent

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UserProcessor()(implicit ec: ExecutionContext) extends ReadSideProcessor[UserEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[UserEvent] = {
    new ReadSideHandler[UserEvent] {
      override def globalPrepare(): Future[Done] =
        UserViewRepository.createTables()

      override def prepare(tag: AggregateEventTag[UserEvent]): Future[Offset] =
        UserViewRepository.loadOffset(tag)

      override def handle(): Flow[EventStreamElement[UserEvent], Done, NotUsed] =
        Flow[EventStreamElement[UserEvent]]
          .mapAsync(1) { eventElement =>
            UserViewRepository.handleEvent(eventElement.event, eventElement.offset)
          }
    }
  }

  override def aggregateTags: Set[AggregateEventTag[UserEvent]] =
    Set(UserEvent.Tag)
}
