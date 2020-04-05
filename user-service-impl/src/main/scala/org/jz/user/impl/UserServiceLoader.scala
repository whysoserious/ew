package org.jz.user.impl

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

import org.jz.user.api.UserService

import play.api.libs.ws.ahc.AhcWSComponents

class UserServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new UserServiceApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new UserServiceApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[UserService])
}

abstract class UserServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  readSide.register(new UserProcessor())

  override lazy val lagomServer: LagomServer = serverFor[UserService](wire[UserServiceImpl])

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = UserSerializerRegistry

  clusterSharding.init(
    Entity(UserState.typeKey)(
      entityContext => UserBehavior.create(entityContext)
    )
  )

}
