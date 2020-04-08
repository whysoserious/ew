package org.jz.user.impl

import java.util.UUID

import akka.actor.typed.ActorRef
import org.jz.user.impl.CommandResponses.{Confirmation, UserCreated}

object Commands {

  sealed trait UserCommand

  // TODO do we need id here? Maybe it should be generated in the event and returned in a CommandResponse
  final case class Create(
      id: UUID,
      name: String,
      reservedTicketsCnt: Int = 0,
      maxReservedTicketsCnt: Int = 10,
      replyTo: ActorRef[UserCreated]
  ) extends UserCommand

  final case class AdjustReservedTicketsCnt(id: UUID, cnt: Int, replyTo: ActorRef[Confirmation]) extends UserCommand




}
