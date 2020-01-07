package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  // same as log inside the actor
  system.log.info("Scheduling reminder for SimpleActor")

  // write scheduler
  implicit val executionContext = system.dispatcher // method 2 to pass execution context
  // import system.dispatcher // method 3 to pass execution context
//  system.scheduler.scheduleOnce(1 second) { // this need execution context to schedule task
//    simpleActor ! "reminder"
//  }//(system.dispatcher) -> method 1 to pass execution context
  // for this we need execution context thus dispatcher is used
  // we can also pass system dispatcher like

  // thus if we run this we will see Scheduling reminder for SimpleActor message followed by 1 sec delay and
  // reminder message


  // lets schedule a repeated message
//  val routine: Cancellable = system.scheduler.schedule(1 second, 2 second) { // after 1 sec 2 sec interval delay
//    simpleActor ! "heartbeat" // every 2 sec one heart beat send
//
//  }

  // lets cancel the schedule
//  system.scheduler.scheduleOnce(5 second) { // after 5 sec call this and only once
//    routine.cancel() // cancel the heart beat after 5 sec
//
//  }


  // lets do an exercise Actor receives a msg and u have 1 sec to send it another msg
  // if time window expires actor stop itself else if new msg it gets it reset the time window until case 1

  class SelfClosingActor extends Actor with ActorLogging {

    var schedule = createTimeoutWindow() // create window on start
    def createTimeoutWindow(): Cancellable = context.system.scheduler.scheduleOnce(1 second) {
      self ! "timeout" // with every 1 sec i get this message
    }

    override def receive: Receive = {
      case "timeout" =>
        log.info("stopping myself")
        context.stop(self)
      case message =>
        log.info(s"receive msg $message, staying alive")
        schedule.cancel()// clear schedule
        schedule = createTimeoutWindow() // again start timeout window
    }
  }

//  val selfClosingActor = system.actorOf(Props[SelfClosingActor],"SelfCLosingActor")
//  system.scheduler.scheduleOnce(250 millis) { // this message will be delivered
//    selfClosingActor ! "ping"
//  }
//  // actor will close itself as after 2 sec we are sending pong which will go in deadletter
//
//  system.scheduler.scheduleOnce(2 second) {
//    system.log.info("sending pong to selfClosingActor")
//    selfClosingActor ! "pong"
//  }

  // lifecycle of a scheduler is difficult to maintain from within the actor for that purpose timers comes into picture
  // eg.

  case object TimerKey // this is used for comparision of timer thr will be one timer for a particular key [key are objects] [identifier]
  case object Start // msg send to myself
  case object Reminder
  case object Stop
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {

    // how to start timer
    timers.startSingleTimer(TimerKey,
      Start,
      500 millis // initial delay
    )


    override def receive: Receive = {
      case Start =>
        log.info("Bootstarpping")
        // after receive start msg create periodic timer with same periodic key
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second) // previous timer is cancel automatically and new Reminder msg is set as periodic timer
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)

    }

  }

  val timerBasedHeartBeatActor = system.actorOf(Props[TimerBasedHeartBeatActor],"timerBasedHeartBeatActor")
  system.scheduler.scheduleOnce(5 seconds){
    timerBasedHeartBeatActor ! Stop
  }
  // we will see generation [1] timer schedule with same TimerKey which is started and replace by generation 2 reminder timer
  // then stopped after 5 seconds

  // timer are used for self actor and scheduler for scheduling task
}

// lets learn about routers now
// this is used to delegate work to other actors
// goto Routers


