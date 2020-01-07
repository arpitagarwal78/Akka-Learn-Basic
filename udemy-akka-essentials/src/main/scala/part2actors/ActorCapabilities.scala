package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello Thr!" // replying to msg
      case message: String => println(s"[${context.self}][simple-actor] I received a message $message")
      case number: Int =>  println(s"[$self][simple-actor] I received a number $number")
      case SpecialMessage(contents) =>  println(s"[simple-actor] I received a special $contents")
      case SendMessageToYourself(contents) =>
        self ! contents
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(contents, ref) =>
        ref forward (contents + "s") // this means to keep original reference of the context with WirelessPhoneMessage
      // distorted msg intentionally

    }
  }

  val system = ActorSystem("ActorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "SimpleActor")

  simpleActor ! "hello actor"

  // part1 message can be of any type eg.
  simpleActor ! 42

  // thus tell method bundle the type with it thus catch by receive method

  // we can define our own special message as
  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("This is something special")

  // NOTE
  // messages must be IMMUTABLE [cant be changed] and SERIALIZABLE [can be send across machines / serialized]


  // part2 Actor have information about thr context
  // context in complex data structure it has access to actorSystem this actor run and it's own actor reference
  // that can be used as context.self [it is an actor with actor path] used in case message
  // context.self === this []context.self is same as self]
  // self can be used to send msg to self as

  case class SendMessageToYourself(contents: String)

  simpleActor ! SendMessageToYourself("Sending message to myself") // this will internally call the String case match
  // this all is async manner

  // part3 Actor reply to the messages
  // lets create two actor

  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  // lets make them to talk to each other
  // so we create a method that take actorRef to tell which actor to talk to

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob) // bob gets a hi from alice [Actor[akka://ActorCapabilitiesDemo/user/bob#306956566]][simple-actor] I received a message hi

  // lets make bob reply as hello thr context.sender can be used to get the sender reference as
  // thus alice receive now hello thr [Actor[akka://ActorCapabilitiesDemo/user/alice#-1150442028]][simple-actor] I received a message Hello Thr!
  // whenever a actor send msg to other actor it pass its reference
  // tell ! implicitly passed that reference so self will automatically be passed in ! thus sender reference is passed


  simpleActor ! 42 // but who is the sender here ???
  // let's try it out

  alice ! "Hi!"
  // this will tell the message to sender cant be delivered as dead letter
  // [INFO] [03/30/2019 16:08:00.093]
  // [ActorCapabilitiesDemo-akka.actor.default-dispatcher-4]
  // [akka://ActorCapabilitiesDemo/deadLetters] Message [java.lang.String]
  // from Actor[akka://ActorCapabilitiesDemo/user/alice#-1946323835]
  // to Actor[akka://ActorCapabilitiesDemo/deadLetters] was not delivered.
  // [1] dead letters encountered. If this is not an expected behavior,
  // then [Actor[akka://ActorCapabilitiesDemo/deadLetters]] may have terminated unexpectedly,
  // This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters'
  // and 'akka.log-dead-letters-during-shutdown'.

  // thus deadletter is fake actor inside actor which tell fake msg that thr is no sender [part4]


  // part 5 how actor can forward msg to one another
  // forwarding --> sending msg in chain but with original sender the first one to all chain elements
  // alice -> bob -> raj -> john [all sender reference should be alice]

  case class WirelessPhoneMessage(contents: String, ref: ActorRef)

  alice ! WirelessPhoneMessage("Hi", bob)
  // thus alice receives Hi message which it forwards to bob with original sender which is null [dead] thus
  // bob receive distorted msg with no sender
  // replace "s" with "!" that will goto dead letter



  // note receive type is
  // type Receive = PartialFunction[Any, Unit]

  // Exercise1 file continued

}
