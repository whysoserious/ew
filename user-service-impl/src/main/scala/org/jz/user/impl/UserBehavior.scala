package org.jz.user.impl

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import Commands.UserCommand
import Events.UserEvent

object UserBehavior {

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[UserCommand, UserEvent, UserAggregate] =
    EventSourcedBehavior
      .withEnforcedReplies[UserCommand, UserEvent, UserAggregate](
        persistenceId = persistenceId,
        emptyState = UserAggregate.initial,
        commandHandler = (state, cmd) => state.applyCommand(cmd),
        eventHandler = (state, evt) => state.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[UserCommand]): Behavior[UserCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    apply(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, UserEvent.Tag)
      )
  }

}
