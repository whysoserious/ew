package org.jz.user.impl

import akka.Done
import akka.persistence.query.NoOffset
import akka.persistence.query.Offset

import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag

import java.util.UUID

import org.jz.user.api.Events._
import org.jz.user.api.UserView

import scala.concurrent.Future

object UserViewRepository {

  private var catalog = Map.empty[UUID, UserView]

  def createTables(): Future[Done] =
    Future.successful(Done)

  def loadOffset(tag: AggregateEventTag[UserEvent]): Future[Offset] =
    Future.successful(NoOffset)

  def handleEvent(event: UserEvent, offset: Offset): Future[Done] = {
    event match {
      case UserCreatedEvent(id, name, reservedTicketsCnt, maxReservedTicketsCnt) =>
        catalog = catalog + (id -> UserView(id, name, reservedTicketsCnt, maxReservedTicketsCnt))
      case ReservedTicketsCntAdjustedEvent(id, cnt) => {
        catalog = catalog.updatedWith(id) {
          case Some(userView) => Some(userView.adjustNumberOfReservedTickets(cnt))
          case None           => None
        }
      }
    }
    Future.successful(Done)
  }

  def getCatalog(foodCartId: UUID): Option[UserView] = {
    catalog.get(foodCartId)
  }
}
