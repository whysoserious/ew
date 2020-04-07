package org.jz.evt.impl
import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.slick.SlickPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import org.jz.evt.api.EvtService
import org.jz.user.api.UserService
import play.api.db.HikariCPComponents
import play.api.libs.ws.ahc.AhcWSComponents

import scala.concurrent.ExecutionContext

class EvtServiceLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new EvtServiceApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new EvtServiceApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[EvtService])
}

trait EvtServiceComponents
    extends LagomServerComponents
    with SlickPersistenceComponents
    with HikariCPComponents
    with AhcWSComponents {

  implicit def executionContext: ExecutionContext

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry =
    EvtSerializerRegistry

  lazy val evtViewRepository: EvtViewRepository =
    wire[EvtViewRepository]
  readSide.register(wire[EvtViewProcessor])

  clusterSharding.init(
    Entity(EvtAggregate.typeKey) { entityContext =>
      EvtBehavior(entityContext)
    }
  )
}

abstract class EvtServiceApplication(context: LagomApplicationContext)
    extends LagomApplication(context)
    with EvtServiceComponents
    with LagomServiceClientComponents
    with LagomKafkaComponents {

  lazy val userService: UserService = serviceClient.implement[UserService]

  override lazy val lagomServer: LagomServer =
    serverFor[EvtService](wire[EvtServiceImpl])
}
