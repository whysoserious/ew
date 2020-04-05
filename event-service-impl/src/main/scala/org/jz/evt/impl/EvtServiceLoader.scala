package org.jz.evt.impl

import akka.cluster.sharding.typed.scaladsl.Entity

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._

import com.softwaremill.macwire._

import org.jz.evt.api.EvtService

import play.api.libs.ws.ahc.AhcWSComponents
import org.jz.user.api.UserService

// TODO start Cassandra in docker
class EvtServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new EvtServiceApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new EvtServiceApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[EvtService])
}

abstract class EvtServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  readSide.register(new EvtProcessor())

  override lazy val lagomServer: LagomServer = serverFor[EvtService](wire[EvtServiceImpl])

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = EvtSerializerRegistry

  lazy val userService: UserService = serviceClient.implement[UserService]

  clusterSharding.init(
    Entity(EvtState.typeKey)(
      entityContext => EvtBehavior.create(entityContext)
    )
  )

}
