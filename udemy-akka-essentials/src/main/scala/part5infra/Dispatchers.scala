package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.concurrent.duration._


object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0
    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatchersDemo")//, ConfigFactory.load().getConfig("dispatchersDemo")) // method 2 config need to added


  // Method one to attach dispatcher
  val actors = for(i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"),s"counter_$i") // configure dispatcher in application.conf

  val r = new Random()
//  for(i <- 1 to 1000) {
//    actors(r.nextInt(10)) ! i // randomly select actor and send i
//    // run
//  }
  // we see all counter 30 msg are processed before move to other actor
  // all actor receive 30 msg and we have 3 thread thread pool size first 30 msg is processed first then move on other
  // if we change thread pool to 1 then all will execute sequentially with 30 count with each
  // note at firt 30 msg as 1000 i is ther later on thread get chance

  // method 2
  // val rtjvmActors = system.actorOf(Props[Counter], "rtjvm") // name should be same

  // Note dispatcher should implement ExecutionContextTrait

  class DBActor extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")//context.dispatcher // method $$ lookup
    override def receive: Receive = {
      case message => Future { // thus in this case we need context as above
        Thread.sleep(5000)
        log.info(s"Success: $message")
        // now both the things happen inside future
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor])
//  dbActor ! "Meaning of life is 42"
  // after 5 sec this msg is logged
  // running of Future inside actor is discourage
  // bcz if it is a long blocking code it will starve the context dispatcher of running thread which is used for handling msg
  // thus delivery of msg to actor will be starved

  // eg lets create a non blocking actor

  val nonBLockingActor = system.actorOf(Props[Counter])
  for(i <- 1 to 1000) {
    val message = s"important message $i "
    dbActor ! message
    nonBLockingActor ! message
  }
  // so when we run we see a hicup of 5 sec dur to Future
  // in order to limit the hicup stop use the context dispatcher which is common use dedicated dispatcher
  // method $$ above
  // after that lookup if u run the context won't be block and it used dedicated my-dispatcher for dbActor not the common context dispatcher
  // method 2 is to use router discuss in router lecture

  // now lets goto application.conf
  // and learn about different dispatcher
  // given in application.conf file comment part


}

// Now lets learn about Mailboxes
// it is data structure in actor ref that stores messages --> Mailboxes