package org.jz.user.impl

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.NotUsed
import akka.persistence.query.Sequence
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.server.LagomApplication
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ReadSideTestDriver
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.lightbend.lagom.scaladsl.testkit.TestTopicComponents
import org.jz.user.api.{Quantity, UserData, UserService, UserView}
import org.jz.user.impl.Events.UserCreatedEvent
import org.jz.user.impl.Events.UserEvent
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UserServiceSpec extends AsyncWordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(ServiceTest.defaultSetup.withJdbc(true)) { ctx =>
    new LagomApplication(ctx) with UserServiceComponents with TestTopicComponents with LocalServiceLocator {
      override lazy val readSide: ReadSideTestDriver = new ReadSideTestDriver()(materializer, executionContext)
    }
  }

  override def afterAll(): Unit = server.stop()

  // TODO Sending requests via serviceClient does not update read store
  private val client: UserService               = server.application.serviceClient.implement[UserService]
  private val testDriver: ReadSideTestDriver = server.application.readSide
  private val offset: AtomicInteger = new AtomicInteger()
  private implicit val exeCxt: ExecutionContext = server.actorSystem.dispatcher

  private def feedEvent(userId: UUID, event: UserEvent): Future[Done] = {
    testDriver.feed(userId.toString, event, Sequence(offset.getAndIncrement))
  }
  "The UserService" should {

    "create a new user with custom name" in {
      for {
        userData <- client.createUser().invoke(UserData("Foo Bar"))
      } yield {
        userData should matchPattern { case UserView(_, "Foo Bar", 0, 10) => }
      }
    }

    "return user data result when requested user exists in the db" in {
      val userId = UUID.randomUUID()
      val userView = for {
        _        <- feedEvent(userId, UserCreatedEvent(userId, "Foo"))
        userView <- client.getUser(userId).invoke(NotUsed)
      } yield {
        userView
      }
      whenReady(userView) {
        _ should matchPattern { case UserView(_, "Foo", 0, 10) => }
      }
    }

    "throw exception when requested resource does not exist" in {
      // TODO Nicer syntax?
      val error = recoverToExceptionIf[NotFound] {
        client.getUser(UUID.randomUUID()).invoke(NotUsed)
      }
      whenReady(error) {
        _ shouldBe a[NotFound]
      }
    }
    "adjust number of reserved tickets" in {
      val userId = UUID.randomUUID()
      val result = for {
        _      <- feedEvent(userId, UserCreatedEvent(userId, "Foo"))
        result <- client.addTickets(userId).invoke(Quantity(1))
      } yield {
        result
      }
      whenReady(result) {
        _ shouldBe a[Done]
      }
    }

    "set number of reserved tickets over the max value" in {
      val userId = UUID.randomUUID()
      val error = recoverToExceptionIf[BadRequest] {
        for {
          _      <- feedEvent(userId, UserCreatedEvent(userId, "Foo"))
          result <- client.addTickets(userId).invoke(Quantity(42))
        } yield {
          result
        }
      }
      whenReady(error) {
        _ shouldBe a[BadRequest]
      }
    }

     "set number of reserved tickets below 0" in {
       val userId = UUID.randomUUID()
       val error = recoverToExceptionIf[BadRequest] {
         for {
           _      <- feedEvent(userId, UserCreatedEvent(userId, "Foo"))
           result <- client.addTickets(userId).invoke(Quantity(-42))
         } yield {
           result
         }
       }
       whenReady(error) {
         _ shouldBe a[BadRequest]
       }
     }

  }

}
