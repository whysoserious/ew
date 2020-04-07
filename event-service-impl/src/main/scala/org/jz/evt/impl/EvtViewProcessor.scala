package org.jz.evt.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import org.jz.evt.impl.Events.{EvtCreatedEvent, EvtEvent, ReservationCreatedEvent}


class EvtViewProcessor(readSide: SlickReadSide, repository: EvtViewRepository) extends ReadSideProcessor[EvtEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[EvtEvent] =
    readSide
      .builder[EvtEvent]("evt-view")
      .setGlobalPrepare(repository.createTable())
      .setEventHandler[EvtCreatedEvent] { envelope =>
        repository.createEvtView(envelope.event)
      }
      .setEventHandler[ReservationCreatedEvent] { envelope =>
        repository.createReservation(envelope.event)
      }
      .build()

  override def aggregateTags: Set[AggregateEventTag[EvtEvent]] = EvtEvent.Tag.allTags
}
