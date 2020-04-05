package org.jz.evt.impl

import akka.Done
import akka.persistence.query.NoOffset
import akka.persistence.query.Offset

import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag

import java.util.UUID

import org.jz.evt.api.Events._
import org.jz.evt.api.EvtView

import scala.concurrent.Future

object EvtViewRepository {

  private var catalog = Map.empty[UUID, EvtView]

  def createTables(): Future[Done] =
    Future.successful(Done)

  def loadOffset(tag: AggregateEventTag[EvtEvent]): Future[Offset] =
    Future.successful(NoOffset)

  def handleEvent(event: EvtEvent, offset: Offset): Future[Done] = {
    event match {
      case EvtCreatedEvent(id, name, availableTickets) =>
        catalog = catalog + (id -> EvtView(id, name, availableTickets, List.empty))
    }
    Future.successful(Done)
  }

  def getCatalog(evtId: UUID): Option[EvtView] = {
    catalog.get(evtId)
  }
}
