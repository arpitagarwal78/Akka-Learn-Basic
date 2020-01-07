package playground

import akka.actor.ActorSystem

object Playground extends App {

  // do new class then select object from dropdown and make it as Playground object class
  // then select actor sytem

  val actorSystem = ActorSystem("HelloAkka") // create a akka actor system
  println(actorSystem.name) // just printing the actor name and run it
  // NOTE we need app to run this so extend it then right click run and we can see
  // HelloAkka in console thus our actor is initialized
}
