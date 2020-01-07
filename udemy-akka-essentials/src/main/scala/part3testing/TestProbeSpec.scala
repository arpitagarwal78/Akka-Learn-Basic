package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

object TestProbeSpec {
  // lets we have same word -- count master slave hierarchy
  // send some work to the master
  // master send slave work
  // slave process and reply
  // master aggregates the result
  // master send total word count to original requester

  case class Register(slaveRef: ActorRef)
  case class Work(text: String)
  case class Report(newTotalWordCount: Int)
  case object RegistrationAck

  case class SlaveWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender ! RegistrationAck
        context.become(online(slaveRef, 0))
      case _ =>
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender)
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef,newTotalWordCount))
    }
  }

  // now slave actor is also define ..... no need for that as we are only testing this actor only
  // thus we need to have an entity that will behave like slave
  // that entity is called test probe

}

class TestProbeSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  import TestProbeSpec._
   override def afterAll(): Unit = {
     TestKit.shutdownActorSystem(system)// system is the member of test kit
   }

  "a master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") // it is slave actor, it is special actor with assertion capability

      master ! Register(slave.ref)
      expectMsg(RegistrationAck) // run and test passed as on sender we get RegisterAck
      // now lets test assertions on Slave Actor
    }

    // lets send some to slave and assert what slave ref receive
    "send work to slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave") // it is slave actor, it is special actor with assertion capability
      master ! Register(slave.ref)
      expectMsg(RegistrationAck) // run and test passed as on sender we get RegisterAck
      // now lets test assertions on Slave Actor

      // lets send some work as
      val workLoadString = "I love learning akka"
      master ! Work(workLoadString)

      slave.expectMsg(SlaveWork(workLoadString,testActor)) // slave have same types of assertion capabilities
      // as testActor sends message [sender of Work is the testActor as testActor sends this master ! Work(workLoadString)
      // run it
      // passed this is interaction between master and slave actor

      // test probes can also send / reply with message as
      slave.reply(WorkCompleted(3, testActor)) // thus we are mocking the reply back to master actor

      expectMsg(Report(3)) // this will be receive bby sender testActor so we haven't used any slave. here
      // run test passed
      // lets see two peaces of work
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workLoadString = "I love learning akka"
      master ! Work(workLoadString) // 1st tym
      master ! Work(workLoadString) // 2nd tym
      // so we should get two report messages so how to check that -->
      // Report(3) and Report(6)
      // we have a slave as passive so we can instruct slave test probe as

      slave.receiveWhile() { // only act when slave receive this
        case SlaveWork(`workLoadString`, `testActor`) => // ` to match exact values
          slave.reply(WorkCompleted(3, testActor))

      }

      expectMsg(Report(3))
      expectMsg(Report(6)) // rest logic of master is done automatically

      // lets now see timed assertions --> TimeAssertionSpecs

    }

  }

}
