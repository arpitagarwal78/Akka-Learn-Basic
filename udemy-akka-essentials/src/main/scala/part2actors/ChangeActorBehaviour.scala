package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangeActorBehaviour.FussyKid.KidAccept
import part2actors.ChangeActorBehaviour.Mom.MomStart
object ChangeActorBehaviour extends App {

  // let two Actor Mother and Kid
  // Kid become sad if get milk
  // Kid become happy if get chocolate by mother

  object FussyKid {

    case object KidAccept

    case object KidReject

    val HAPPY = "happy"
    val SAD = "sad"

  }

  class FussyKid extends Actor {

    import FussyKid._
    import Mom._

    var state = HAPPY

    override def receive: Receive = {
      case Food(VEGITABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender ! KidAccept
        else sender ! KidReject
    }
  }


  object Mom {

    case class MomStart(kidRef: ActorRef)

    case class Food(food: String)

    case class Ask(message: String) // ask any question [do you want to play ?]

    val VEGITABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {

    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test interaction
        // test interaction to check become logic
//        kidRef ! Food(VEGITABLE)
//        kidRef ! Ask("Do you want to play?")

        // test interaction to check un become logic
        kidRef ! Food(VEGITABLE)
        kidRef ! Food(VEGITABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE) // remove one chocolate u will find kid is sad our stack logic is intact
        kidRef ! Ask("Do you want to play?")


      case KidAccept => println("Yeh! ,my kid is happy")
      case KidReject => println("My kid is sad but healthy")
    }

  }

  // so kid responds based on the different types of states
  val system = ActorSystem("ChangingActorBehaviourDemo")
  val kid = system.actorOf(Props[FussyKid],"kid")
  val mom = system.actorOf(Props[Mom],"mom")

  mom ! MomStart(kid)

  // it is working fine but our logic will blow off if we have lot of states for kid not just accept reject
  // we should limit using variable HAPPY for state
  // thus lets do state less fussy kid


  class StatelessFussyKid extends Actor {

    import FussyKid._
    import Mom._

    // how to switch state around
    def happyReceive: Receive = {
      case Food(VEGITABLE) => // change handler to sad receive
        //context.become(sadReceive) // line thr in become example basic
        context.become(sadReceive, false) // line not thr in become example basic
      case Food(CHOCOLATE) => // remain in this state happy
      case Ask(_) => sender ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGITABLE) => // remain in this state sad
        context.become(sadReceive, false) // line not thr in become example basic
      case Food(CHOCOLATE) => // change handler to happy receive
        // context.become(happyReceive) // line not thr in become example basic
        context.unbecome() // line not thr in become example basic
      case Ask(_) => sender ! KidReject
    }


    override def receive: Receive = happyReceive // initially it returns happy receive
  }

  // now how to change state receive is given above
  // use context become to do so...

  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessKid")
  // same behaviour is seen here as well
  mom ! MomStart(statelessFussyKid)


  // now lets see how to revert the current behaviour
  // context.become(x) is equal to context.become(x, true) [default]
  // then it discard first receive and replace with second
  // but if suppose i do context.become(x, false) then it will stack both the receives
  // so if we send Food(veg) --> sadReceive
  // if we send Food(chocolate) --> happyReceive
  // but in this implementation it will be stack of implementation meaning
  // Stack:
  // 1. happyReceive [we start with]
  // 2. we receive Food(veg) --> it's like stack.push(sadReceive)
  // 3. so it become a)sadReceive at top then b)happyReceive
  // 4. we receive Food(chocolate) --> stack.push(happyReceive, false)
  // 5. so now stack is a) happyReceive b)sadReceive at top then c)happyReceive
  // 6. each time akka called top most receive stack handler
  // 7. in order to pop the top most element out we use context.unbecome
  // Lets check unbecome behaviour
  //
  // Food(veg) , Food(veg), Food(chocolate) Food(chocolate) [behaviours]
  // 1. happyReceive start point
  // 2. 1st event --> a) sadReceive b) happyReceive
  // 3. 2nd event --> a) sadReceive b) sadReceive c) happyReceive
  // 4. 3rd revent ---> pop first out
  // a)sadReceive b) happyReceive
  // 5. again if receive Food(chocolate) --> pop as unbecome and left with happyReceives

  // Exercise 3 implement counter with NO MUTABLE state and become
}
