package org.jz.user.api

import akka.actor.typed.ActorRef

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

trait UserCommandSerializable

object Commands {

  // Commands
  sealed trait UserCommand extends UserCommandSerializable

  final case class Create(
      id: UUID,
      name: String,
      reservedTicketsCnt: Int = 0,
      maxReservedTicketsCnt: Int = 10,
      replyTo: ActorRef[UserCreated]
  ) extends UserCommand

  final case class AdjustReservedTicketsCnt(id: UUID, cnt: Int, replyTo: ActorRef[Confirmation]) extends UserCommand

  // Return types for commands

  object UserCreated {
    implicit val format: Format[UserCreated] = Json.format
  }

  final case class UserCreated(id: UUID, name: String, reservedTicketsCnt: Int, maxReservedTicketsCnt: Int)

  // TODO build CommandResponse
  // TODO move command responses to a separate file
  sealed trait Confirmation

  case object Confirmation {
    implicit val format: Format[Confirmation] = new Format[Confirmation] {
      override def reads(json: JsValue): JsResult[Confirmation] = {
        if ((json \ "reason").isDefined)
          Json.fromJson[Rejected](json)
        else
          Json.fromJson[Accepted](json)
      }

      override def writes(o: Confirmation): JsValue = {
        o match {
          case acc: Accepted => Json.toJson(acc)
          case rej: Rejected => Json.toJson(rej)
        }
      }
    }
  }

  sealed trait Accepted extends Confirmation

  final case object Accepted extends Accepted {
    implicit val format: Format[Accepted] =
      Format(Reads(_ => JsSuccess(Accepted)), Writes(_ => Json.obj()))
  }

  final case class Rejected(reason: String) extends Confirmation

  object Rejected {
    implicit val format: Format[Rejected] = Json.format
  }

}
