package org.jz.user.api

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.AsyncWordSpec
import org.scalatest.Matchers
import org.jz.user.impl.UserServiceApplication
import org.scalatest.BeforeAndAfterAll
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import java.util.UUID
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.transport.BadRequest

class UserServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  "The UserService" should {

    // "create a new user with custom name" in {
    //   for {
    //     userData <- client.createUser.invoke(UserData("Foo Bar"))
    //   } yield {
    //     userData should matchPattern { case UserView(_, "Foo Bar", 0, 10) => }
    //   }
    // }
    // TODO ignore instead of commenting
    "return user data result when requested user exists in the db" in {
      for {
        UserView(id, _, _, _) <- client.createUser.invoke(UserData("Foo Bar"))
        userViewResult        <- client.getUser(id).invoke(NotUsed)
      } yield {
        System.out.println("=======================================")
        System.out.println(id)
        System.out.println(userViewResult)
        userViewResult should matchPattern { case UserViewResult(None) => }
      }
    }

    // "return empty result when requested resource does not exist" in {
    //   for {
    //     userViewResult <- client.getUser(UUID.randomUUID()).invoke(NotUsed)
    //   } yield {
    //     userViewResult should matchPattern { case UserViewResult(None) => }
    //   }
    // }

    // "adjust number of reserved tickets" in {
    //   for {
    //     UserView(id, _, _, _) <- client.createUser.invoke(UserData("Foo Bar"))
    //     _                     <- client.addTickets(id).invoke(Quantity(1))
    //     userViewResult        <- client.getUser(id).invoke(NotUsed)
    //   } yield {
    //     userViewResult should matchPattern { case UserViewResult(None) => }
    //   }
    // }

    // "set number of reserved tickets over the max value" in {
    //   assertThrows[BadRequest] {
    //     for {
    //       UserView(id, _, _, maxReservedTicketsCnt) <- client.createUser.invoke(UserData("Foo Bar"))
    //       _                                         <- client.addTickets(id).invoke(Quantity(maxReservedTicketsCnt + 1))
    //     } yield {
    //       ???
    //     }
    //   }
    // }

    // "set number of reserved tickets below 0" in {
    //   assertThrows[BadRequest] {
    //     for {
    //       UserView(id, _, _, maxReservedTicketsCnt) <- client.createUser.invoke(UserData("Foo Bar"))
    //       _                                         <- client.addTickets(id).invoke(Quantity(-1))
    //     } yield {
    //       ???
    //     }
    //   }
    // }

  }

  lazy val serverSetup = ServiceTest.defaultSetup.withCassandra()

  lazy val server = ServiceTest.startServer(serverSetup) { ctx =>
    new UserServiceApplication(ctx) with LocalServiceLocator
  }

  lazy val client = server.serviceClient.implement[UserService]

  protected override def beforeAll() = server

  // protected override def afterAll() = server.stop()

}
