package part2actors

import akka.actor.{Actor, ActorSystem, Props}

// write a program to create a counter actor to increment, decrement and print
object Exercise1 extends App {

  class Counter extends Actor {
    var counter = 0

    override def receive: Receive = {
      case Increment(n) => counter += n
      case Decrement(n) => counter -= n
      case Print => println(counter)
    }
  }

  case class Increment(n: Int)
  case class Decrement(n: Int)
  case class Print()

  val system = ActorSystem("GlobalActor")
  val counterActor = system.actorOf(Props[Counter], "CounterActor")

  counterActor ! Increment(9)
  counterActor ! Print
  counterActor ! Increment(3)
  counterActor ! Decrement(7)
  counterActor ! Print
  counterActor ! Decrement(2)

  // how msg are not intermingling ????

  // Exercise2 continue

}
