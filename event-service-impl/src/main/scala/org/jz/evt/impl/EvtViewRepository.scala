package org.jz.evt.impl

import akka.Done
import akka.persistence.query.NoOffset
import akka.persistence.query.Offset
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import java.util.UUID

import Events._
import org.jz.evt.api.EvtView
import org.jz.evt.api.ReservationView

import scala.concurrent.Future

import akka.Done
import org.jz.evt.impl.Events.{EvtCreatedEvent}
import slick.dbio.{DBIO, Effect}
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EvtViewRepository(database: Database) {

  class EvtViewTable(tag: Tag) extends Table[EvtView](tag, "Evt_view") {
    def evtId = column[UUID]("evt_id", O.PrimaryKey)

    def name = column[String]("name")

    def availableTickets = column[Int]("available_tickets_cnt")

    def * = (evtId, name, availableTickets) <> ((EvtView.apply _).tupled, EvtView.unapply)
  }

  val evtViewTable = TableQuery[EvtViewTable]

  def createTable(): FixedSqlAction[Unit, NoStream, Effect.Schema] = evtViewTable.schema.createIfNotExists

  def findById(EvtId: UUID): Future[Option[EvtView]] =
    database.run(findByIdQuery(EvtId))
  
  def createEvtView(event: EvtCreatedEvent): DBIO[Done] = {
        findByIdQuery(event.id)
          .flatMap {
            case None =>
              evtViewTable += EvtView(event.id, event.name, event.availableTickets)
            case _ => DBIO.successful(Done)
          }
          .map(_ => Done)
          .transactionally
  }
  
  def createReservation(event: ReservationCreatedEvent): DBIO[Done] = {
        findByIdQuery(event.id)
          .flatMap {
            case Some(evtView) =>
              evtViewTable.insertOrUpdate(evtView.copy(availableTickets = evtView.availableTickets + event.ticketsCount))
            case None => throw new RuntimeException(s"Evt with ID ${event.id} does not exist in read database")
          }
          .map(_ => Done)
          .transactionally
  }

  private def findByIdQuery(evtId: UUID): DBIO[Option[EvtView]] = {
    evtViewTable
      .filter(_.evtId === evtId)
      .result
      .headOption
  }
}
