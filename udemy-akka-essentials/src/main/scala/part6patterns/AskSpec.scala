package part6patterns

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util
import scala.util.Success

import akka.pattern.pipe // pipeTo



object AskSpec {

  // assume this code is somewhere else in system
  // key value actor
  case class Read(key: String)
  case class Write(key: String, value: String)
  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kvMap: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read value of key $key")
        sender() ! kvMap.get(key) // this will be option[string]
      case Write(key, value) =>
        log.info(s"Writing a value $value for a key $key")
        context.become(online(kvMap + (key -> value)))
    }
  }


  // we will use this KV class to user authenticator
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }
  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    // logistics for ask
    implicit val timeout: Timeout = Timeout(2 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) =>
        authDb ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)
    }


    def handleAuthentication(username: String, password: String) = {
      //        authDb ! Read(username) // authenticate user by geting user password and then context become to check its a pain
      //        // as this actor may be bombarded by auth and we have to authenticate with respect to sender
      //        // now which user to authenticate we need to keep state for sender
      //        // to make it easy we do
      //        context.become(waitingForPassword(username, sender()))

      // we do ask
      val future = authDb ? Read(username) // it returns the future for potential response
      // this wont work bcz ? and onComplete work in diffrent thread and sender() will become authDB in this case
      val originalSender = sender()
      future.onComplete{
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND) // when no password in returned
        case Success(Some(dbPassword)) =>
          if(dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case util.Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
      // so what to do // NEVER ACCESS ACTOR INSTANCE OR MUTABLE STATE IN ON COMPLETE
      // use originalSender

    }

    //    def waitingForPassword(str: String, ref: ActorRef): Receive = {
    //      case password: Option[String] => // do password check here
    //    }
  }




  // how to handle it diffrently

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuthentication(username: String, password: String): Unit = {
      val future = authDb ? Read(username) // Future[Any]
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]
      val responseFuture = passwordFuture.map{
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if(dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)

      } // for compiler this will be Future[Any] as it can be AuthSuccess or AuthFailure

      responseFuture.pipeTo(sender()) // when future completes sends the response to actor ref
      // this sender is same as authManager as we are not doing future on complete call back

    }
  }
}

class AskSpec extends TestKit(ActorSystem("AskSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    authenticatorTestSuit(Props[AuthManager])
  }

  "An Pipe authenticator" should {
    authenticatorTestSuit(Props[PipedAuthManager])
  }

  def authenticatorTestSuit(props: Props) = {
    import AuthManager._

    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! Authenticate("daniel", "rtcv")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate incorrect password" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("daniel", "rtcv")
      authManager ! Authenticate("daniel", "gg")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("daniel", "rtcv")
      authManager ! Authenticate("daniel", "rtcv")
      expectMsg(AuthSuccess)
    }

  }

}
