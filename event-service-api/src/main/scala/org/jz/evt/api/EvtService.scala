package org.jz.evt.api

// TODO organize imports
import akka.Done
import akka.NotUsed

import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.transport.Method

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json
import java.time.ZonedDateTime

object EvtService {
  val TOPIC_NAME = "events"
}

// TODO remove Result classes
trait EvtService extends Service {

  /**
   *  Create a new event.
   *
   *  Example: curl -v -H "Content-Type: application/json" -d '{"name": "Olympics 2021", "availableTickets": 30}' -X POST http://localhost:9000/event
   *
   */
  def createEvt(): ServiceCall[EvtData, EvtView]

  /**
   * Get an event.
   *
   * Example: curl -v http://localhost:9000/event/bd0cc462-286a-4a67-8b53-102196715f96
   *
   */
  def getEvt(evtId: UUID): ServiceCall[NotUsed, EvtView]

  /**
   * Get an event.
   *
   *  Example:
   *  curl -v -H "Content-Type: application/json" \
   *    -d '{"userId": "bd0cc462-286a-4a67-8b53-102196715f96", "ticketsCnt": 4, "reservationTime": XXX}' \
   *   -X POST \
   *   http://localhost:9000/event/bd0cc462-286a-4a67-8b53-102196715f96/reservation
   *
   */
  def createReservation(evtId: UUID): ServiceCall[ReservationData, ReservationView]

  def extendReservationTime(evtId: UUID, reservationId: UUID): ServiceCall[ReservationTime, Done]

  def cancelReservation(evtId: UUID, reservationId: UUID): ServiceCall[NotUsed, Done]

  final override def descriptor: Descriptor = {
    import Service._
    named("event-service")
      .withCalls(
        restCall(Method.POST, "/event", createEvt _),
        restCall(Method.GET, "/event/:evtId", getEvt _),
        restCall(Method.POST, "/event/:evtId/reservation", createReservation _),
        restCall(Method.PATCH, "/event/:evtId/reservation/:reservationId/extend", extendReservationTime _),
        restCall(Method.PATCH, "/event/:evtId/reservation/:reservationId/cancel", cancelReservation _)
      )
      .withAutoAcl(true)
  }

}
// Input data

object ReservationData {
  implicit val format: Format[ReservationData] = Json.format
}

final case class ReservationData(userId: UUID, ticketsCnt: Int, reservationTime: ZonedDateTime)

object EvtData {
  implicit val format: Format[EvtData] = Json.format
}

final case class EvtData(name: String, availableTickets: Int)

object ReservationTime {
  implicit val format: Format[ReservationTime] = Json.format
}

final case class ReservationTime(newTime: ZonedDateTime)

// Response data

object ReservationView {
  implicit val format: Format[ReservationView] = Json.format
}

final case class ReservationView(userId: UUID, ticketsCount: Int, reservationTime: ZonedDateTime, canceled: Boolean)

object ReservationViewResult {
  implicit val format: Format[ReservationViewResult] = Json.format
}

// TODO are these Result classes required?
final case class ReservationViewResult(content: Option[ReservationView])

object EvtView {
  implicit val format: Format[EvtView] = Json.format
}

final case class EvtView(
    id: UUID,
    name: String,
    availableTickets: Int,
    reservations: List[ReservationView]
)

// Remove
object EvtViewResult {
  implicit val format: Format[EvtViewResult] = Json.format
}

final case class EvtViewResult(content: Option[EvtView])
