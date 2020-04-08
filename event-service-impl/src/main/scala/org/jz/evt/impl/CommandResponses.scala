package org.jz.evt.impl

import java.time.ZonedDateTime
import java.util.UUID

import play.api.libs.json._

object CommandResponses {

  sealed trait CommandResponse

  sealed trait EvtCreatedResponse

  object EvtCreated {
    implicit val format: Format[EvtCreated] = Json.format
  }

  final case class EvtCreated(id: UUID, name: String, availableTickets: Int) extends EvtCreatedResponse

  sealed trait ReservationCreatedResponse

  object ReservationCreated {
    implicit val format: Format[ReservationCreated] = Json.format
  }

  final case class ReservationCreated(
      id: UUID,
      evtId: UUID,
      userId: UUID,
      ticketsCount: Int,
      reservationTime: ZonedDateTime,
      canceled: Boolean
  ) extends ReservationCreatedResponse

  object ReservationTimeExtended {
    implicit val format: Format[ReservationTimeExtended] = Json.format
  }

  final case class ReservationTimeExtended(
      id: UUID,
      evtId: UUID,
      userId: UUID,
      ticketsCount: Int,
      reservationTime: ZonedDateTime,
      canceled: Boolean
  )

  object ReservationCanceled {
    implicit val format: Format[ReservationCanceled] = Json.format
  }

  final case class ReservationCanceled(id: UUID, evtId: UUID)

  case object CommandResponse {
    implicit val format: Format[CommandResponse] = new Format[CommandResponse] {
      override def reads(json: JsValue): JsResult[CommandResponse] = {
        if ((json \ "reason").isDefined)
          Json.fromJson[Rejected](json)
        else
          Json.fromJson[Accepted](json)
      }

      override def writes(o: CommandResponse): JsValue = {
        o match {
          case acc: Accepted => Json.toJson(acc)
          case rej: Rejected => Json.toJson(rej)
        }
      }
    }
  }

  final case object Accepted extends Accepted {
    implicit val format: Format[Accepted] =
      Format(Reads(_ => JsSuccess(Accepted)), Writes(_ => Json.obj()))
  }

  sealed trait Accepted extends CommandResponse

  object Rejected {
    implicit val format: Format[Rejected] = Json.format
  }
  
  final case class Rejected(reason: String) extends CommandResponse with EvtCreatedResponse with ReservationCreatedResponse

}
