package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Excercise4 extends App {

//  case class Vote(candidate: String)
//  case object VoteStatusRequest
//  case class VoteStatusReply(candidate: Option[String])
//
//  class Citizen extends Actor {
//    var candidate: Option[String] = None
//
//    override def receive: Receive = {
//      case Vote(c) => candidate = Some(c)
//      case VoteStatusRequest =>
//        sender ! VoteStatusReply(candidate)
//    }
//  }
//
//  case class AggregateVotes(citizens: Set[ActorRef])
//
//  class VoteAggregator extends Actor {
//    var stillWaiting: Set[ActorRef] = Set()
//    var currentStats: Map[String, Int] = Map()
//
//    override def receive: Receive = {
//      case AggregateVotes(citizens) =>
//        stillWaiting = citizens
//        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
//      case VoteStatusReply(None) =>
//        // a citizen hadn't voted
//        sender ! VoteStatusRequest // one more request but if citizen didn't vote then it will be recursive but i know
//      // all my citizen have already voted
//      case VoteStatusReply(Some(candidate)) =>
//        val newStillWaiting = stillWaiting - sender
//        val currentVotesOfCandidates = currentStats.getOrElse(candidate, 0)
//        currentStats = currentStats + (candidate -> (currentVotesOfCandidates + 1))
//
//        if(newStillWaiting.isEmpty) {
//          println(s"[aggregator] poll stats: $currentStats")
//        } else {
//          stillWaiting = newStillWaiting
//          println("[aggregator] poll not over yet")
//        }
//
//
//    }
//  }
//
//
//  val system = ActorSystem("VoterSystem")
//  val alice = system.actorOf(Props[Citizen], "alice")
//  val bob = system.actorOf(Props[Citizen], "bob")
//  val charlie = system.actorOf(Props[Citizen], "charlie")
//  val daniel = system.actorOf(Props[Citizen], "daniel")
//
//  alice ! Vote("Martin")
//  bob ! Vote("Jonas")
//  charlie ! Vote("Ronald")
//  daniel ! Vote("Ronald")
//
//  val voteAggregator = system.actorOf(Props[VoteAggregator],"voteaggregator")
//  voteAggregator ! AggregateVotes(Set(alice,bob,charlie,daniel))

  // Print status of the votes Map of every candidate and number of votes they received

  // lets refract the above code to context become

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {

    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))//candidate = Some(c)
      case VoteStatusRequest =>
        sender ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest =>
        sender ! VoteStatusReply(Some(candidate))

    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor {

    override def receive: Receive = awaitingCommand

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))
    }

    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive ={
      case VoteStatusReply(None) =>
        // a citizen hadn't voted
        sender ! VoteStatusRequest // one more request but if citizen didn't vote then it will be recursive but i know
      // all my citizen have already voted
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender
        val currentVotesOfCandidates = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidates + 1))

        if(newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $newStats")
        } else {
          context.become(awaitingStatuses(newStillWaiting, newStats))
          println("[aggregator] poll not over yet")
        }
    }
  }


  val system = ActorSystem("VoterSystem")
  val alice = system.actorOf(Props[Citizen], "alice")
  val bob = system.actorOf(Props[Citizen], "bob")
  val charlie = system.actorOf(Props[Citizen], "charlie")
  val daniel = system.actorOf(Props[Citizen], "daniel")

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Ronald")
  daniel ! Vote("Ronald")

  val voteAggregator = system.actorOf(Props[VoteAggregator],"voteaggregator")
  voteAggregator ! AggregateVotes(Set(alice,bob,charlie,daniel))

  // let's discuss parent and child -> ChildActors
}
