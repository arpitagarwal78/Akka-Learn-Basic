package part3testing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, EventFilter, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

object  SynchronousTestingSpec {


  case object Inc
  case object Read

  // actor to test
  class Counter extends Actor {

    var count = 0;
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender ! count
    }
  }
}

// this wont extends a test kit but only two imports
// thus sync test does not need testkit
class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll {
import SynchronousTestingSpec._

  // create actor system

  implicit val system = ActorSystem("SynchronousActorTesting")

  override def afterAll(): Unit = {
    system.terminate()
  }

  "A counter" should {
    // sync unit test

    "synchronously increase in counter" in {
      val counter = TestActorRef[Counter](Props[Counter])
      // this have capability as
      counter ! Inc // counter actor will receive actorRef

      // thus i can make assertion on actor as
      assert(counter.underlyingActor.count == 1)

    }

    "synchronously increase in counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc) // this is same as ! operator
      assert(counter.underlyingActor.count == 1)

    }

    "work on the calling thread dispatcher" in {
      // create genuine actor
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      // this means counter will work on CallingThreadDispatcher
      // thus all the message send on dispatcher will be passed to calling thread
      val probe = TestProbe()

      probe.send(counter, Read) // this testprobe will send counter with Inc message [probe help to send msg to counter]
      probe.expectMsg(Duration.Zero, 0) // thus probe should have receive 0 msg as counter we send 0
      // duration zero is like it wont wait for timeout of 3 sec as prob will already receive msg 0 dur to 67 line number
      // if we remove  withDispatcher(CallingThreadDispatcher.Id) line this will fail bcz counter starts to add in
      // asyn manner

    }
  }
}

// now lets discuss akka monitoring and watching actor methodology
// starting actor is very easy
// use system.actorOf or context.actorOf we have seen that before
// lets learn stopping actor package part4faulttolerance -> StartingStoppingActors
