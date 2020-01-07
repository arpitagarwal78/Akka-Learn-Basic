package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, Kill, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Terminated}
import akka.dispatch.sysmsg.Terminate
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import part4faulttolerance.StartingStoppingActors.Parent.StartChild

object SupervisionSpec {

  case object Report

  class Supervisor extends Actor {
    // default supervisor strategy is to restart the actor but to define its own strategy we have to do this

    // this apply supervisor strategy with failed children
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      // here we will define actions to take in failure
      case _:NullPointerException => Restart
      case _:IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }


    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSupervisor extends Supervisor {
    // this apply supervisor strategy with all the children
    override val supervisorStrategy = AllForOneStrategy() {
      // here we will define actions to take in failure
      case _:NullPointerException => Restart
      case _:IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case Report => sender ! words
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if(sentence.length > 20 ) throw new RuntimeException("sentence is too big")
        else if(!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with upper case")
        else words += sentence.split(" ").length
      case _ => throw new Exception("can only receive strings")
    }
  }
}

// now lets test our ator supervisor strategy
class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
 with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  import SupervisionSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A supervisor" should {
    "resume its child in case of minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef] // we should get a reference
      child ! "I love Akka"
      child ! Report
      expectMsg(3) // 3 word count

      child ! "Akka is awesome because I am learning to think in a whole new way" // this throw runtime exception apply supervisor strategy
      // this is resume so wont affect it
      child ! Report
      expectMsg(3) // as state is not change
    }

    "restart its child in case of empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef] // we should get a reference

      child ! "I love Akka"
      child ! Report
      expectMsg(3) // 3 word count


      child ! "" // this restart which elimates or destroy the instance
      child ! Report
      expectMsg(0) // we will get 0 as it re initialise the state
    }

    "terminate its child in case of major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef] // we should get a reference

      // register for death watch
      watch(child)
      child ! "akka is nice" // illegalargument result in stop actor

      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)

    }

    "escalte its error when doent know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "Supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef] // we should get a reference

      // register for death watch
      watch(child)
      child ! 43 // escalate actor // this stop all children and escalate exception to parent

      val terminatedMessage = expectMsgType[Terminated] // expect the child should be terminated [Stop children]
      assert(terminatedMessage.actor == child)
      // now exception is not in child but in parent in Supervisor
//        [ERROR] [04/29/2019 21:22:27.509] [SupervisionSpec-akka.actor.default-dispatcher-2] [akka://SupervisionSpec/user/Supervisor] can only receive strings
//        java.lang.Exception: can only receive strings

    }
  }


  "A kinder supervisor" should {
    "not kill children in case it restarted or escalated the failure" in {
      // let different actor
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "NoDeathOnRestartSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef] // we should get a reference

      child ! "Akka is cool"
      child ! Report

      expectMsg(3)


      child ! 45 // is exception --> escalates to User Gaurdian it restart this supervisor but at restart i wont kill child
      // thus this actor will be 0 msg as re initialise
      child ! Report // as user gaurdian restart everything thus 0 msg
      expectMsg(0)

    }
  }

  "All for one supervisor" should {
    "apply all for one strategy" in {
      // let different actor
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "AllForOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef] // we should get a reference

      supervisor ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef] // we should get a reference

      child ! "Akka is cool"
      child ! Report

      expectMsg(3)

      child2 ! "Akka is cool"
      child2 ! Report

      expectMsg(3)


      // child one raises null pointer result in restart of all the actor
      EventFilter[NullPointerException]() intercept {
        child ! ""

      }
      Thread.sleep(500) // to see supervisor strategy apply
      child2 ! Report // as user gaurdian restart everything thus 0 msg
      expectMsg(0) // should be 0 as restarted child2 because of child failure allinone strategy

    }
  }

}

// now there is a problem if a actor is again and again starting [restarting] suppose actor coordinate with database
// which went down thus again and again restarting is not a valid operation
// thus we need to have some exponential delay to overcome with this types of problems