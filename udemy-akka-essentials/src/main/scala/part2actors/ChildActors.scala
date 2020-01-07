package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.Parent.CreateChild

object ChildActors extends App {

  // actors can create others actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {

    import Parent._
    //var child: ActorRef = null // can be removed when using become
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child..")
        // create new actor HERE
        val childRef = context.actorOf(Props[Child],name)
        //child = childRef // can be removed when using become
        context.become(withChild(childRef))
//      case TellChild(message) =>   //      case TellChild(message) =>
//        if (child != null) child forward message
    }

    def withChild(childRef: ActorRef): Receive ={
      case TellChild(message) =>
        if (childRef != null) childRef forward message
    }
  }


  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }


  import Parent._
  val system = ActorSystem("ParentChild")
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("child")
  parent ! TellChild("Hey Kid!")
  // OUTPUT
  // akka://ParentChild/user/parent creating child..
  // akka://ParentChild/user/parent/child I got: Hey Kid!
  // Acotors can create other actors by context.actorOf
  // this create actors hierarchy
  // parent -> child
  // we can create any deep hierarchy

  // child is own by parent but who own parent -->
  // answer is guardian actor it is of 3 types (top-level actor)
  // 1. /system --> system guardian [eg. managing logging system etc.] manage system
  // 2. /user --> user guardian every created actor is a user guardian actor eg. akka://ParentChild/user/parent/child I got: Hey Kid!
  // user path denotes the child [manage all other actor]
  // 3. / --> root guardian it manages both user and system level guardian if this throw exception all actor system die


  // Actor Selection
  // we can create a selection by
  val childSelection = system.actorSelection("/user/parent/child") // we can pass path as a string
  // now we can send messsage to that actor with the same path
  childSelection ! TellChild("I found you!")

  // if suppose we send message to a actor path not present then it will simply drop the message by dead letter with Actor not found
  val invalidChildSelection = system.actorSelection("/user/parent/child1")
  invalidChildSelection ! TellChild("I found you!") // [akka://ParentChild/user/parent/child1] Message [part2actors.ChildActors$Parent$TellChild] without sender to Actor[akka://ParentChild/user/parent/child1] was not delivered. [1] dead letters encountered. If this is not an expected behavior, then [Actor[akka://ParentChild/user/parent/child1]] may have terminated unexpectedly, This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.



  // lets see a piece of danger --> NEVER PASS MUTABLE ACTOR STATE, OR 'THIS' REFERENCE TO CHILD ACTOR
  // lets see eg...


  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCard = context.actorOf(Props[CreditCard], "card")
        creditCard ! AttachToAccount(this) // danger we passed this [lets we though ah is same as object oriented]
      case Deposit(funds) =>
        deposit(funds)
      case Withdraw(funds) =>
        withdraw(funds)


    }

    def deposit(funds: Int) = {
        println(s"${self.path} depositing funds $funds on top of account $amount")
        amount += funds
      }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdraw funds $funds from account $amount")
      amount -= funds
    }
  }


  object CreditCard {
    case class AttachToAccount(bankAccount: NaiveBankAccount) // lets a junior programmer autocomplete by NaiveBankAccount
    // which is a type actor QUESTIONABLE

    case object CheckStatus
  }
  class CreditCard extends Actor {
    import CreditCard._

    override def receive: Receive = {
      case AttachToAccount(account) =>
        context.become(attachToAccount(account))
    }

    def attachToAccount(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message have been processed")
        // after that we withdraw the funds as
      account.withdraw(1) // now this is the problem
    }
  }

  import NaiveBankAccount._
  import CreditCard._
  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account") // this is creating credit card actor
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)

  Thread.sleep(500)

  val ccSelection = system.actorSelection("user/account/card")
  ccSelection ! CheckStatus

  // lets run now
//  akka://ParentChild/user/account/card your message have been processed
//    akka://ParentChild/user/account withdraw funds 1 from account 100

  // the second output is illigitimate because we have directly called a method on it
  // this is hard to debug on big app
  // as check status calls withdraw and it by pass all the logic and security issues
  // be very careful
  // problem is we have passed by this actual reference of the actor in AttachToAccount(bankAccount: NaiveBankAccount)
  // by creditCard ! AttachToAccount(this)
  // this breaks actor concurrency and actor protection

  // we have voilated msg sending to Actor
  // this is coiling over in Actor and should be prevented in every case

  // we need to change that to actor reference to resolve 80 % of the problem

  // Exercise 4 for child actor [ChildActorExercise]

}
