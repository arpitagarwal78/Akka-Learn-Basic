package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}
import akka.dispatch.sysmsg.Terminate
import part4faulttolerance.StartingStoppingActors.Parent.StartChild

object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorDemo")

  // parent actor
  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }
  class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name)=>
        log.info(s"Stopping child $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)// this not only stop itself but all child actor
      case message =>
        log.info(message.toString)

    }

  }

  // child actor
  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  import Parent._
//  val parent = system.actorOf(Props[Parent], "parent")
//  parent ! StartChild("child1")
//  val child = system.actorSelection("/user/parent/child1") // child actor created
//  child ! "HI Kid!"
  // run the actor work fine
//    [DEBUG] [04/26/2019 15:18:01.241] [main] [EventStream(akka://StoppingActorDemo)] logger log1-Logging$DefaultLogger started
//    [DEBUG] [04/26/2019 15:18:01.242] [main] [EventStream(akka://StoppingActorDemo)] Default Loggers started
//    [INFO] [04/26/2019 15:18:01.337] [StoppingActorDemo-akka.actor.default-dispatcher-2] [akka://StoppingActorDemo/user/parent] Starting child child1
//    [INFO] [04/26/2019 15:18:01.345] [StoppingActorDemo-akka.actor.default-dispatcher-4] [akka://StoppingActorDemo/user/parent/child1] HI Kid!

//  // lets now stop the child
//  parent ! StopChild("child1") // context.stop doent stop immediately
//  for(_ <- 1 to 100) child ! "Are you thr?" // thus immediately we send 100 msg but we will see it wont stop immediately child actor
//  // after stopping as well child actor still receive few messages

//  parent ! StartChild("child2")
//  val child2 = system.actorSelection("/user/parent/child2") // child actor created
//  child2 ! "Hi child2!"
//
//  parent ! Stop
//  for(_ <- 1 to 50) parent ! "Are you thr parent? " // should not be received
//  for(i <- 1 to 100) child2 ! s"[$i]Are you thr child2? " // child stop before parent stop



  // method 2 using special message
//  val looseActor = system.actorOf(Props[Child])
//  looseActor ! PoisonPill // this is a special message for actor
//  looseActor ! "Are you thr loose actor ?"  // this log to deadletter
//
//  val abruptlyTerminateActor = system.actorOf(Props[Child])
//  abruptlyTerminateActor ! "You will be terminated"
//  abruptlyTerminateActor ! Kill // this raise actor kill exception actor throw this exception on killing
//  abruptlyTerminateActor ! "You have been terminated?"

  // death watch notify if actor dies
  class Watchers extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)// this watches child and if it dies actor receive terminated message to this actor
      case Terminated(ref) =>
        log.info(s"the reference i am watching is stopped $ref")


    }
  }

  val watchers = system.actorOf(Props[Watchers],"watcher")
  watchers ! StartChild("watchedchild")
  val watchedChild = system.actorSelection("/user/watcher/watchedchild")
  Thread.sleep(500)
  watchedChild ! PoisonPill // this will kill child and watcher will receive the terminated call


}

// next is akka lifecycle

// lets understand about actor instance what it can have
// 1. it can have methods like receive method
// 2. it can have state or variables
// 3. Actor reference -> actorOf [incarnation] responsible for comm it have unique UUID given by actorsystem and mailbox and one actor ref
// 4. actor path may or may not have actor reference in the path

// Actors can be STARTED, SUSPENDED, RESUMED, RESTARTED, STOPPED
// STARTED --> create new actorRef with a UUID in given path
// SUSPEND --> actor will enqueue msg in mailbox but not process and will resume processing on intimation
// RESUMED --> actor will continue processing more messages
// RESTARTING --> it consist of following states
//                1. Actor is suspended
//                2. Actor instance is swapped as following
//                    2a. old instance call preRestart
//                    2b. then it replaces actor instance
//                    2c. new actor instance call postRestart
//                3. Actor is resumed
// NOTE : Internal state is destroyed while restarting the actor
// STOPPING --> this also consist of following paths [it frees the actor ref for the following path]
//            1. it calls postStop
//            2. all watching actor receive terminated message with actor ref
// After actor stop diff actor can be created in the same path with different UUID and actor ref
// all message in queue are lost
// goto ActorLifecycle file