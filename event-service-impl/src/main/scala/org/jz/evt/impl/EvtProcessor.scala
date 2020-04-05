package org.jz.evt.impl

import akka.Done
import akka.NotUsed
import akka.persistence.query.Offset
import akka.stream.scaladsl.Flow

import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor.ReadSideHandler

import org.jz.evt.api.Events.EvtEvent

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EvtProcessor()(implicit ec: ExecutionContext) extends ReadSideProcessor[EvtEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[EvtEvent] = {
    new ReadSideHandler[EvtEvent] {
      override def globalPrepare(): Future[Done] =
        EvtViewRepository.createTables()

      override def prepare(tag: AggregateEventTag[EvtEvent]): Future[Offset] =
        EvtViewRepository.loadOffset(tag)

      override def handle(): Flow[EventStreamElement[EvtEvent], Done, NotUsed] =
        Flow[EventStreamElement[EvtEvent]]
          .mapAsync(1) { eventElement =>
            EvtViewRepository.handleEvent(eventElement.event, eventElement.offset)
          }
    }
  }

  override def aggregateTags: Set[AggregateEventTag[EvtEvent]] =
    Set(EvtEvent.Tag)
}
