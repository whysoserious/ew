package org.jz.user.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import org.jz.user.api.UserService
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

class UserServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new UserServiceApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new UserServiceApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[UserService])
}

trait UserServiceComponents
    extends LagomServerComponents
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext

  override lazy val lagomServer: LagomServer =
    serverFor[UserService](wire[UserServiceImpl])

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    UserSerializerRegistry

  lazy val userViewRepository: UserViewRepository =
    wire[UserViewRepository]
  readSide.register(wire[UserViewProcessor])

  clusterSharding.init(
    Entity(UserAggregate.typeKey) { entityContext =>
      UserBehavior(entityContext)
    }
  )
}

abstract class UserServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with UserServiceComponents
    with LagomKafkaComponents
