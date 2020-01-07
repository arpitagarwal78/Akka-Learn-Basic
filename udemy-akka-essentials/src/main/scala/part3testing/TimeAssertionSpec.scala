package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random


object TimeAssertionSpec {
  // now there are lot of actor that response at very long for query for a large peace of work
  // eg hard computation or waiting for resource
  // some actor response very fast

  // so lets define a class worker actor

  case class WorkResult(result: Int)

  class WorkerActor extends Actor {

    override def receive: Receive = {
      case "work" => // long computation will be here
        Thread.sleep(500) // half sec thread sleep
        sender ! WorkResult(42)
      case "workSequence" => // quick reply
        val r = new Random()
        for (i <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender ! WorkResult(1)
        }

    }
  }

  // lets test it
  // as this is time sensitive so we need to time box are thread

}

class TimeAssertionSpec extends TestKit(ActorSystem("TimeAssertionSpec",
  ConfigFactory.load.getConfig("specialTimeAssertionConfig"))) // this is pass to take akka timeout expectMsg
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  import TimeAssertionSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }


  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    // time box test
    "reply with a meaning of life in a timely manner" in {
      within(500 millis, 1 second) { // within 500 mili second or at most 1 sec get reply
        workerActor ! "work"
        expectMsg(WorkResult(42)) // this pass as worker actor sleep for 500 mili
        // Note if this happens soon then also it will fail bcz it should be at least 500 milis
      }

    }

    // will fail
//    "reply with a meaning of life in a failed" in {
//      within(300 millis) { // within 300 mili second [atmost] this should fail as actor sleep for 500 mili
//        workerActor ! "work"
//        expectMsg(WorkResult(42)) // fail
//      }
//
//    }

    "reply with a valid work at resonable cadence" in {
      within(1 second) {
        workerActor ! "workSequence" // this will send bunch of messages
        val results = receiveWhile[Int] (max = 2 second, idle= 500 millis, messages = 10) { // at max of 2 sec idle time 500 milis get 10 message
          // within 2 sec i should receive 10 message at least 500 milis sec time apart or delay
          case WorkResult(result) =>
            result // this give back a sequence of this result

        }

        assert(results.sum > 5) // just check result will be greater than 5
      }

    }

    // doent hae any influence to test probe
    "reply to a test probe in a timely manner" in {
//      val probe = TestProbe()
//      probe.send(workerActor, "work")
//      probe.expectMsg(WorkResult(42)) // this will pass as not time box but lets timebox



      within(1 second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42)) // this is not confirm to within 1 sec but own configuration
        // thus this uses its own configuration not uses within configuration
        // lets set expectMessage timeout in configuration [by default it is 3 mili]
        // goto application.conf
        // thus within block is permisive with one sec but expectMsg will fail as it will timeout in 0.3 sec that is less than 500 mili
      }
    }


  }


}

// now lets learn about intercepting logs InterceptingLogSpec