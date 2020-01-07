package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorExercise extends App {

  // common thing which we gonna learn here is akka parallelism
  // distribution of worker and parent
  // Distributed word counting

  // we will create two Actor

  object WordCounterMaster {
    // 3 message types
    case class Initialise(nChildren: Int) // initialise and children message on response to that it will create children
    // of word counter worker and delegate task to it
    case class WordCountTask(id: Int, text: String) // receive a peace of text that it will send to word count childer any one
    case class WordCountReply(id: Int, count: Int) // which will reply back to word count reply

  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialise(nChildren) =>
        println("[master] initialising....")
        val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker],s"wcw_$i")
        context.become(withChildrens(childrenRefs, requestMap = Map()))
    }

    def withChildrens(childrenRefs: Seq[ActorRef], currentChildIndex: Int = 0, currentTaskId: Int = 0, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] received text as $text and sending to child $currentChildIndex")
        val task = WordCountTask(currentTaskId, text) // create a task
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length // next child index goes back to 0 when max length reached
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> sender())
      context.become(withChildrens(childrenRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I have received a reply for task id $id with count $count")
        // now here we have problem whom to send
      // sender no its not as child send msg thus i have lost track
      // thus we need to have id to WordCOuntTask and WordCOuntReply that id can bbe used by master to send reply back to sender as
      // but how to map this id to sender id so we add one more parameter to withChilders as
      // now requestMap have our actor
      requestMap(id) ! count
        // remove this id from requestMap
        context.become(withChildrens(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))

    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[worker] i have received the task with text $text with id $id")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }


  // Problem statement
  // WordCounterMaster --> initialise 10 WordCounterWorkers
  // send text to WordCounterMaster ---> send WordCOuntTask to one of its children
  // that child reply with WordCount reply to the Master
  // MAster reply to the count number to sender [let sender be Requestor Actor]
  // load balancing can bbe achieved by round robin fashion
  // eg 7 task
  // 1 2 3 4 5 WordCOuntWorker
  // 1 2 3 4 5 1 2


  // test actor class for testing

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialise(3)
        val texts = List("I love akka", "Scala is super dope!", "yes", "me 2")
        texts.foreach(text => master ! text)
      case count:Int =>
        println(s"I received a reply $count")

    }
  }


  val system = ActorSystem("roundRobinExercise")
  val testActor = system.actorOf(Props[TestActor])
  testActor ! "go"



  // NEXT WE will learn about Actor Logging
  // this is used for dumping logs
  // ActorLoggingDemo
}
