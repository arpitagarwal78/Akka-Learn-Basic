package part2actors


import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.util.{Failure, Success}

object Excercise2 extends App {

  case class Deposit(amount: Double)
  case class Withdraw(amount: Double)
  case class Statement()

  class Bank extends Actor {

    var funds: Double = 0.0
    override def receive: Receive = {
      case Deposit(amount) =>
        funds += amount
        context.sender ! Success(s"Deposit amount of $amount is passed")
      case Withdraw(amount) =>
        val withdraw = funds - amount
        if(withdraw < 0) {
          context.sender ! Failure( new Exception(s"Withdraw amount of $amount is failed"))

        } else {
          funds -= amount
          context.sender ! Success(s"Withdraw amount of $amount is passed")
        }
      case Statement =>
        println(s"The required statement is $funds")
    }
  }


  case class RequestTractionAmount(amount: Double, actorRef: ActorRef)
  case class RequestStatement(actorRef: ActorRef)

  class Person extends Actor {
    override def receive: Receive = {
      case RequestTractionAmount(n, ref) =>
        if(n > 0) {
          ref ! Deposit(n)
        } else {
          ref ! Withdraw(-n)
        }
      case RequestStatement(ref) =>
        ref ! Statement
      case Success(msg) =>
        println(msg)
      case Failure(msg) =>
        println(msg)
    }
  }

  val system = ActorSystem("BankScenario")
  val person = system.actorOf(Props[Person], "person")
  val bank = system.actorOf(Props[Bank], "bank")

  person ! RequestTractionAmount(1000.0, bank)
  person ! RequestStatement(bank)
  person ! RequestTractionAmount(-100.0, bank)
  person ! RequestTractionAmount(-1000.0, bank)
  person ! RequestStatement(bank)
  person ! RequestTractionAmount(5000.0, bank)
  person ! RequestTractionAmount(-4000.0, bank)
  person ! RequestStatement(bank)
  person ! RequestTractionAmount(-40000.0, bank)

  // we are not using synchronisaton ??? as it might cause raise condition

  // lets answer both

  // ordering of msg / race condition / async means for Actor ?? / thread in actor
  // actor system have a thread pool
  // actor have message handler [receieve] and message queue [mail box]
  // actor needs a thread it launches few thread can launch millions of actor
  // sending msg to actor --> actor mailbox [thread safe]
  // actor needs thread to execute at some time it gets thread and read from mailbox and do operation after some
  // time it may release the actor [thread] and do something else
  // one thread operate on actor on one time so need to do synchronisation
  // thread may never release actor in middle of operation
  // akka offers minimum one delivery of msg
  // sender receiver pair msg send order is always guaranteed

  // next learn how actor change behaviour ChangeActorBehaviour.scala

}
