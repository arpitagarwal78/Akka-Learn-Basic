package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Exercise3 extends App {

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {

    import Counter._

    override def receive: Receive = countReceive(0) // receive with argument [default arg is 0]

    def countReceive(currentCount: Int): Receive = {
      case Increment =>
        println(s"[countReceive($currentCount)] incrementing.....")

        context.become(countReceive(currentCount + 1))
      case Decrement =>
        println(s"[countReceive($currentCount)] decrementing.....")
        context.become(countReceive(currentCount - 1))
      case Print => println(s"[countReceive($currentCount)] my current count is $currentCount")
    }
  }

  import Counter._
  val system = ActorSystem("CounterApp")
  val counter = system.actorOf(Props[Counter],"counter")
  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print


  // above is a stateless actor

  // Exercise4 voting system
}
