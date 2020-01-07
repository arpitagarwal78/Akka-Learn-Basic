package part6patterns

import akka.actor.ActorSystem.Settings
import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config
import akka.routing._
import com.typesafe.config.ConfigFactory

object StashDemo extends App {

  // Let thr be a resource actor which process request if on else postpone it

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // resouce actor is in close state
  // on receive of open it opens
  // it can read / write
  // when receive close it close
  // note read / write msg are postpone if close

  // eg
  // [Open, Read, Read, Write]
  // Open then Read Read and Write

  // [Read, Open, Write]
  // Read it is postpone and put in stash
  // Open it will switch state to open
  // The stash is prepended to mailbox
  // mailbox is [Read , Write] and handled


  class ResourceActor extends Actor with ActorLogging with Stash { // mix the Stash trait for stashing

    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        unstashAll() // this is done if it become open prepend the msg those are stash in mailbox queue
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the closed state")
        stash()

    }

    def open: Receive = {
      case Read =>
        log.info(s"I have read $innerData")
      case Write(msg) =>
        log.info(s"I am writing $msg")
      case Close =>
        log.info(s"closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }

  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read





}

// lets learn ASK -> ASK Spec test suit
