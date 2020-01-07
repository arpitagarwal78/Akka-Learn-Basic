package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  // part1 is create ActorSystem first
  val actorSystem = ActorSystem("firstActorSystem") // HEAVY WEIGHT DATA STRUCUTRE
  // that maintains all actors [only alpha numeric characters]
  println(actorSystem.name)

  // part2 it is like human talking send and receive msg
  // Actor are uniquely identified like human name and identity
  // actor send receive msg async
  // they react acc to thr time
  // you can invade them

  // word count actor [declare a class]

  // entends actor to make it actor
  class WordCountActor extends Actor {

    // this is internal data
    var totalWords = 0

    // receive method for receiving interaction msg
    // behaviour
    override def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word-counter] I have receive a message as $message")
        totalWords += message.split(" ").length
      case msg => println(s"[word-counter] I cant understand ${msg.toString}")
    }
  }

  // part 3 is instantiate actor in actor system
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "WordCounter") // we are adding actor to actor system
  val wordCounterAnother = actorSystem.actorOf(Props[WordCountActor], "WordCounter2")
  // the above give us Actor reference and communication is only possible with this not otherwise

  // part 4 communicate
  wordCounter ! "I am learning Akka and it's pretty damn cool!"
  wordCounterAnother ! "this is a new msg"
  // this sending of messsage is async
  // note we cant create actor by new it can only be created by actorSystem
  // ! - it is a tell operator


  // but how we instantiate actor by new
  // this is done by props as
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
    }
  }
  val person = actorSystem.actorOf(Props(new Person("Raj")))
  // we can say new Person from props method not but not outside
  // new Person("he") [can't be done error while running]
  person ! "hi"

  // the above type is not encourage rather do like below

  class PersonNew(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
    }
  }

  object PersonNew {
    def props(name: String) = Props(new PersonNew(name))
  }

  val newPerson = actorSystem.actorOf(PersonNew.props("Ria"))
  newPerson ! "hi"
  // goto actor capabilities
}
