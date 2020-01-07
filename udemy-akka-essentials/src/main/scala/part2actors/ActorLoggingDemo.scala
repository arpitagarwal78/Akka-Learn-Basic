package part2actors
import akka.actor.{Actor, ActorRef, ActorSystem, Props, ActorLogging}
import akka.event.Logging

object ActorLoggingDemo extends App {

  class SimpleActorWithExplicitLogging extends Actor {

    //define simple member logger as
     val logger = Logging(context.system, this) // pass reference of this actor
    override def receive: Receive = {
      case message => // LOG IT
      logger.info(s"Received $message")
      // level 1 debug  -> exactly what happen in application
      // level 2 info  -> most non critical
      // level 3 warn  --> they might not be source of trouble ex deadletters
      // level 4  error ---> critical error
    }
  }


  // there are couple of ways to do actor logging
  // 1. Explicit logging

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogging], "simpleloggingactor")
  actor ! "hello this is a test message"
  // output
  // [INFO] [04/13/2019 11:21:01.798] [LoggingDemo-akka.actor.default-dispatcher-3] [akka://LoggingDemo/user/simpleloggingactor] Received hello this is a test message


// Method 2 is
  // 2. ActorLogging

  class ActorWithLogging extends Actor with ActorLogging {
    // actor loggin is a trait
    override def receive: Receive = {
      case (a,b) => log.info("Two things: {} and {}", a, b) // log interpolating
      case message =>
        log.info(s"Receive : $message")
    }
  }

  val actor2 = system.actorOf(Props[ActorWithLogging], "loggingactor")
  actor2 ! "hello this is a test message"
  // output
  // [INFO] [04/13/2019 11:25:57.145] [LoggingDemo-akka.actor.default-dispatcher-2] [akka://LoggingDemo/user/loggingactor] Receive : hello this is a test message
  // level -- time --- actorsyetem -- message dispatcher -- actual message


  // lets see interpolating parameters in line 38
  actor2 ! ("hello", "baby")


  // we can also insert other loggers as log4j and slf4j easily in akka as well

  // lets learn about akka configuration
  // this is held within actor system
  // IntroAkkaConfig

}
