package org.jz.user.impl

import java.util.UUID

import akka.Done
import org.jz.user.api.UserView
import org.jz.user.impl.Events.{ReservedTicketsCntAdjustedEvent, UserCreatedEvent}
import slick.dbio.{DBIO, Effect}
import slick.jdbc.PostgresProfile.api._
import slick.sql.FixedSqlAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserViewRepository(database: Database) {

  class UserViewTable(tag: Tag) extends Table[UserView](tag, "user_view") {
    def userId = column[UUID]("user_id", O.PrimaryKey)

    def name = column[String]("name")

    def reservedTicketsCnt = column[Int]("reserved_tickets_cnt")

    def maxReservedTicketsCnt = column[Int]("max_reserved_tickets_cnt")

    def * = (userId, name, reservedTicketsCnt, maxReservedTicketsCnt) <> ((UserView.apply _).tupled, UserView.unapply)
  }

  val userViewTable = TableQuery[UserViewTable]

  def createTable(): FixedSqlAction[Unit, NoStream, Effect.Schema] = userViewTable.schema.createIfNotExists

  def findById(userId: UUID): Future[Option[UserView]] =
    database.run(findByIdQuery(userId))

  def createUserView(event: UserCreatedEvent): DBIO[Done] = {
    findByIdQuery(event.id)
      .flatMap {
        case None =>
          userViewTable += UserView(event.id, event.name, event.reservedTicketsCnt, event.maxReservedTicketsCnt)
        case _ => DBIO.successful(Done)
      }
      .map(_ => Done)
      .transactionally
  }

  def adjustNumberOfReservedTickets(event: ReservedTicketsCntAdjustedEvent): DBIO[Done] = {
    findByIdQuery(event.id)
      .flatMap {
        case Some(userView) =>
          userViewTable.insertOrUpdate(userView.copy(reservedTicketsCnt = userView.reservedTicketsCnt + event.cnt))
        case None => throw new RuntimeException(s"User with ID ${event.id} does not exist in read database")
      }
      .map(_ => Done)
      .transactionally
  }

  private def findByIdQuery(userId: UUID): DBIO[Option[UserView]] = {
    userViewTable
      .filter(_.userId === userId)
      .result
      .headOption
  }
}
