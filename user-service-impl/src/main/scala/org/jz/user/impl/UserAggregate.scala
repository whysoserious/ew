package org.jz.user.impl

import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.ReplyEffect
import Commands._
import Events._
import org.jz.user.impl.CommandResponses.Accepted
import org.jz.user.impl.CommandResponses.Rejected
import org.jz.user.impl.CommandResponses.UserCreated
import play.api.libs.json.Format
import play.api.libs.json.Json

object UserAggregate {
  def initial: UserAggregate = UserAggregate(name = "", reservedTicketsCnt = 0, maxReservedTicketsCnt = 10)

  val typeKey: EntityTypeKey[UserCommand] = EntityTypeKey[UserCommand]("UserAggregate")

  implicit val format: Format[UserAggregate] = Json.format
}

case class UserAggregate(name: String, reservedTicketsCnt: Int, maxReservedTicketsCnt: Int) {

  def applyCommand(cmd: UserCommand): ReplyEffect[UserEvent, UserAggregate] =
    cmd match {
      case x: Create                   => onCreate(x)
      case x: AdjustReservedTicketsCnt => onAdjustReservedTicketsCnt(x)
    }

  private def onCreate(cmd: Create): ReplyEffect[UserEvent, UserAggregate] = {
    Effect
      .persist(UserCreatedEvent(cmd.id, cmd.name, cmd.reservedTicketsCnt, cmd.maxReservedTicketsCnt))
      .thenReply(cmd.replyTo) { _ =>
        UserCreated(cmd.id, cmd.name, cmd.reservedTicketsCnt, cmd.maxReservedTicketsCnt)
      }
  }

  private def onAdjustReservedTicketsCnt(cmd: AdjustReservedTicketsCnt): ReplyEffect[UserEvent, UserAggregate] = {
    val newQuantity = reservedTicketsCnt + cmd.cnt
    if (newQuantity < 0) {
      Effect.reply(cmd.replyTo)(Rejected("Number of reserved tickets cannot be less than 0"))
    } else if (newQuantity > maxReservedTicketsCnt) {
      Effect.reply(cmd.replyTo)(Rejected(s"Number of reserved tickets cannot be greater than $maxReservedTicketsCnt"))
    } else {
      Effect
        .persist(ReservedTicketsCntAdjustedEvent(cmd.id, cmd.cnt))
        .thenReply(cmd.replyTo) { _ =>
          Accepted
        }
    }
  }

  def applyEvent(evt: UserEvent): UserAggregate = {
    evt match {
      case x: UserCreatedEvent                => createUser(x)
      case x: ReservedTicketsCntAdjustedEvent => adjustReservedTicketsCnt(x)
    }
  }

  private def createUser(evt: UserCreatedEvent): UserAggregate = {
    copy(
      name = evt.name,
      reservedTicketsCnt = evt.reservedTicketsCnt,
      maxReservedTicketsCnt = evt.maxReservedTicketsCnt
    )
  }

  private def adjustReservedTicketsCnt(evt: ReservedTicketsCntAdjustedEvent): UserAggregate = {
    copy(reservedTicketsCnt = reservedTicketsCnt + evt.cnt)
  }

}
