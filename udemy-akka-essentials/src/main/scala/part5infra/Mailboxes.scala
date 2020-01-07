package part5infra

import akka.actor.ActorSystem.Settings
import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, PoisonPill, Props, Terminated}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config
import akka.routing._
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.concurrent.duration._

object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  // lets focus on interesting use cases

  // Case 1 custom priority mailbox eg support ticketing system
  // eg ticket name start with p0 this is most important then p1 p2 p3 priority order for a ticket system

  // Step 1 mailbox definition
  class SupportTicketPriorityMailbox(settings: Settings, // we need to have Setting of ActorSystem type
                                     config: Config)    // we need to have config of type safe type
  extends UnboundedPriorityMailbox(// this should extend a built in class for mailboxes
    PriorityGenerator{ // it takes first argument of type PriorityGenerator which takes the partial function [priority] anything => int return type [priority]
      case message: String if message.startsWith("[P0]") => 0 // lower means higher priority
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
      case _ => 4
    })

  // Step 2 ake it known in the configuration [application.conf]
  // support-ticket-dispatcher

  // Step 3 attach mailbox dispatcher to the Actor

  val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
//  supportTicketActor ! "[P3] this thing would be nice to have"
//  supportTicketActor ! "[P0] this needs to be solved Now!"
//  supportTicketActor ! "[P1] do this one you have the time"
  // once run we will get p0 then p1 then p3 msg

  // suppose we add PoisonPill in first still it will run fine and add Thread.sleep lets c
//  supportTicketActor ! PoisonPill
//  Thread.sleep(1000)
//  supportTicketActor ! "[P3] this thing would be nice to have"
//  supportTicketActor ! "[P0] this needs to be solved Now!"
//  supportTicketActor ! "[P1] do this one you have the time"
  // Thread.sleep result in deadletter all msg but if not then it works fine with PoisonPill
  // means at what time we can send msg so that we can prioritise our msg
  // we cant know this and cant do it bcz its a thread based system


  // Case 2 Control aware mailbox
  // some msg need to be solved first no matter what msg are stacked first
  // we will use unbounded control aware mailbox [unbounded means store any number of msg]

  // Step 1 mark imp message as control message

  case object ManagementTicket extends ControlMessage // control message type of akka dispatcher

  // Step 2 configure who gets the mailbox
  // method 1
  // ake actor attach to the mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
//  controlAwareActor ! "[P0] this thing would be nice to have"
//  controlAwareActor ! "[P1] this needs to be solved Now!"
//  controlAwareActor ! ManagementTicket
  // if we run we see ManagementTicket is accessed first

  // method 2 by using the deployment config

  val alternativeControlAwareActor = system.actorOf(Props[SimpleActor],"altControlAwareActor")
  alternativeControlAwareActor ! "[P0] this thing would be nice to have"
  alternativeControlAwareActor ! "[P1] this needs to be solved Now!"
  alternativeControlAwareActor ! ManagementTicket
  // same behaviour we see here in this



}

// lets learn about stashing in part6pattern package now
