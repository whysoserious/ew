package org.jz.user.impl

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect

import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry

import java.util.UUID

import org.jz.user.api.Commands._
import org.jz.user.api.Events._

import play.api.libs.json.Format
import play.api.libs.json.Json

import scala.collection.immutable.Seq

object UserBehavior {

  def create(entityContext: EntityContext[UserCommand]): Behavior[UserCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    create(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, UserEvent.Tag)
      )
  }

  private[impl] def create(persistenceId: PersistenceId) =
    EventSourcedBehavior
      .withEnforcedReplies[UserCommand, UserEvent, UserState](
        persistenceId = persistenceId,
        emptyState = UserState.initial,
        commandHandler = (state, cmd) => state.applyCommand(cmd),
        eventHandler = (state, evt) => state.applyEvent(evt)
      )
}

case class UserState(name: String, reservedTicketsCnt: Int, maxReservedTicketsCnt: Int) {
  def applyCommand(cmd: UserCommand): ReplyEffect[UserEvent, UserState] =
    cmd match {
      case x: Create                   => onCreate(x)
      case x: AdjustReservedTicketsCnt => onAdjustReservedTicketsCnt(x)
    }

  private def onCreate(cmd: Create): ReplyEffect[UserEvent, UserState] = {
    Effect
      .persist(UserCreatedEvent(cmd.id, cmd.name, cmd.reservedTicketsCnt, cmd.maxReservedTicketsCnt))
      .thenReply(cmd.replyTo) { _ =>
        UserCreated(cmd.id, cmd.name, cmd.reservedTicketsCnt, cmd.maxReservedTicketsCnt)
      }
  }

  private def onAdjustReservedTicketsCnt(cmd: AdjustReservedTicketsCnt): ReplyEffect[UserEvent, UserState] = {
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

  def applyEvent(evt: UserEvent): UserState = {
    evt match {
      case x: UserCreatedEvent                => createUser(x)
      case x: ReservedTicketsCntAdjustedEvent => adjustReserverdTicketsCnt(x)
    }
  }

  private def createUser(evt: UserCreatedEvent): UserState = {
    copy(
      name = evt.name,
      reservedTicketsCnt = evt.reservedTicketsCnt,
      maxReservedTicketsCnt = evt.maxReservedTicketsCnt
    )
  }

  private def adjustReserverdTicketsCnt(evt: ReservedTicketsCntAdjustedEvent): UserState = {
    copy(reservedTicketsCnt = reservedTicketsCnt + evt.cnt)
  }

}

object UserState {
  def initial: UserState = UserState(name = "", reservedTicketsCnt = 0, maxReservedTicketsCnt = 10)

  val typeKey = EntityTypeKey[UserCommand]("UserAggregate")

  implicit val format: Format[UserState] = Json.format
}

object UserSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[UserCreatedEvent],
    JsonSerializer[ReservedTicketsCntAdjustedEvent],
    JsonSerializer[UserCreated],
    JsonSerializer[Confirmation],
    JsonSerializer[Accepted],
    JsonSerializer[Rejected]
  )
}
