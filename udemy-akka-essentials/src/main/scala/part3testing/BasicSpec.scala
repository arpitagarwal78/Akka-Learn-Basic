package part3testing

import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.actor.{Actor, ActorSystem, Props}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
    with ImplicitSender // used for send - reply scenario is akka
    with WordSpecLike // this allows description of test in akka in natural language style
    with BeforeAndAfterAll // this provide some hooks that will be called once run the testkit
{

  // testing classes are generally end with Spec
  // for this class to be a test suit we extends test kit in it
  // we pass actor system to the test spec so that once it run it runs with context to actor system
  // the we mix some interfaces
  // lets define hook afterAll used to destroy testSuit

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)// system is the member of test kit
  }

  // the general structure of scala test is like

  //   "The thing been tested" should {
  //     "do this" in {
  //       // testing scenario
  //     }
  //
  //     "do other this" in {
  //       // testing scenario
  //     }
  //   }


  // lets try to see how to do asyn assertion based on the actor define above
  import BasicSpec._
  "A SimpleActor" should {
    "send back the same message" in {
      // create actor
      val echoActor = system.actorOf(Props[MySimpleActor],"simpleActor") // system comes from testkit
      val message = "hello, test"
      echoActor ! message

      expectMsg(message)// expect exact msg
      // run test
    }
  }

  "A Blackhole Actor" should {
    "send back the same message" in {
      // create actor
      val blackholeActor = system.actorOf(Props[BlackHoleActor],"blackHole") // system comes from testkit
      val message = "hello, test"
      blackholeActor ! message

      //expectMsg(message)// expect exact msg [Failed]
      // run test
      // this wait for certain amount of time and then fails if timeout of 3 sec passes
      // this timeout is configurable as
      // akka.test.single-expect-default [this configuration can be added with in configuration file]
      // lets fix it
      expectNoMessage(1 second)
    }
  }



  // now who is in the receiving send of the message
  // answer is test actor that is passed implicitly for every message so we have used implicitSender trait
  // now lets learn about assertions
  // labtestactor

  "A LabTest Actor" should {
    val labActor = system.actorOf(Props[LabTestActor],"labTestActor") // system comes from testkit
    // this is done when no need to create actor for every test

    "should convert the string in upper case" in {
      // create actor
      val message = "hello, test"
      labActor ! message

      expectMsg("HELLO, TEST")// expect exact msg
      // run test it passed

      // thr are other ways to assert ex hold on the message and then do assertions

    }


    "should convert the string in upper case hold message" in {
      // create actor
      val message = "hello, test"
      labActor ! message

      val reply = expectMsgType[String]

      assert(reply == "HELLO, TEST") // any type of complex assertions can be done here
    }

    "reply to the greeting" in {
      // create actor
      labActor ! "greeting"

      expectMsgAnyOf("hi","hello") // so it can be any message of these two
    }


    "reply with favouriteTest" in {
      // create actor
      labActor ! "favouriteTest"

      expectMsgAllOf("Scala","Akka") // so check all msg from sender
    }

    "reply with favouriteTest with different way" in {
      // create actor
      labActor ! "favouriteTest"

      val message = receiveN(2) // this return seq of any if i receive less than two msg this will fail
      // now i can do complex assertion
    }

    "reply with favouriteTest in fancy way" in {
      // create actor
      labActor ! "favouriteTest"
      // this we can pass partial function as
      expectMsgPF()  {
        case "Scala" => // only care partial function define for value as we have both akka scala this test will pass
          // note we can also change the message return if not it is okie and just check value in case match
        case "Akka" =>
      }
      // now i can do complex assertion
    }
  }


}



// we generally create a companion object to store info for our test

object BasicSpec {

  class MySimpleActor extends Actor {
    override def receive: Receive = {
      case message =>
        sender ! message // reply by same msg
    }
  }

  // lets create a blackhole actor for failure scenario

  class BlackHoleActor extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }


  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        if(random.nextBoolean()) sender ! "hi" else sender ! "hello"
      case "favouriteTest" =>
        sender ! "Scala"
        sender ! "Akka"
      case message =>
        sender ! message.toString.toUpperCase()
    }
  }

}



// after this we will learn about test probes certain kind of actors with assertion capabilities TestProbeSpec