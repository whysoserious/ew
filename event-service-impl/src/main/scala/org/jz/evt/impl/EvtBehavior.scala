package org.jz.evt.impl

import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityContext
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import com.lightbend.lagom.scaladsl.persistence.AkkaTaggerAdapter
import Commands.EvtCommand
import Events.EvtEvent

object EvtBehavior {

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[EvtCommand, EvtEvent, EvtAggregate] =
    EventSourcedBehavior
      .withEnforcedReplies[EvtCommand, EvtEvent, EvtAggregate](
        persistenceId = persistenceId,
        emptyState = EvtAggregate.initial,
        commandHandler = (state, cmd) => state.applyCommand(cmd),
        eventHandler = (state, evt) => state.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[EvtCommand]): Behavior[EvtCommand] = {
    val persistenceId: PersistenceId = PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    apply(persistenceId)
      .withTagger(
        AkkaTaggerAdapter.fromLagom(entityContext, EvtEvent.Tag)
      )
  }

}
