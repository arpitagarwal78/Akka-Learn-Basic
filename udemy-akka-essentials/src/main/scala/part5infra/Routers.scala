package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object Routers extends App {

  // master consist of 5 slave which forward request to them

  // how master route to slave method 1
  // step 1 create routee
  class Master extends Actor with ActorLogging {
    private val slaves = for(i <- 1 to 5) yield {
       val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    // step 2 define router
    private val router = Router(RoundRobinRoutingLogic(),//router logic [AlgoName + RoutingLogic]
      slaves // this  should be routee
    )// this router is passed with all the slave which act as round robin routee

    override def receive: Receive = {
      // handle the death of routees as dead watch we added [Terminated]
      case Terminated(ref) =>
        router.removeRoutee(ref) // remove the routee reference
        // create new one
        val slave = context.actorOf(Props[Slave])
        context.watch(slave)
        router.addRoutee(slave) // add the new routee
      case message =>
        // step 3 route the msg
        router.route(message,sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }


  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo")) // load routersdemo from application.conf [method 3]
  val master = system.actorOf(Props[Master])
//  for(i <- 1 to 10){
//    master ! s"[$i] Hello from the world!" // slave should receive msg in round robin
//  }
  // thus we verified that the round robin fashion is working fine
  // other options of router algo is [AlgoName + _] ==>
  // round robin
  // random
  // smallest mailbox [actor with fewest msg in queue]
  // broadcast [send to all]
  // scatter gather first [broadcast to everyone and wait for reply and discard other reply]
  // tail-chopping forward msg to all actor sequentially and wait for the first reply and other reply discarded
  // consistent hashing all msg with the same hash get to the same actor
  // here we will use only round robin just we are foccused on learn about how to construct router than algo
  // lets c another method in creating routers this is much simpler

// method 2 router actor with its own children [PoolRouter]

  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolActor") // round robin actor will create
  // 5 actor of type Slave [AlgoName + Pool]
  // pool means router actor has its own children
  // thus if we send 10 msg to pool actor
//  for(i <- 1 to 10){
//    poolMaster ! s"[$i] Hello from the world!" // slave should receive msg in round robin
//  } // actor name is a, b , c , d but all msg are routed in round robin


  // lets learn router through configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2") // this name should be same as define in application.conf
  // it will check what also and number of instance
  // note config is loaded in ActorSystem [method 3]
//  for(i <- 1 to 10){
//    poolMaster2 ! s"[$i] Hello from the world!" // slave should receive msg in round robin
//  }
  // method 3 also works fine


  // method 4 router actor created elsewhere [GroupActor]
  // in another part of application i creates slave as
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  // lets create groupactor around them
  // for which we need path as
  val slavePath = slaveList.map(slaveRef => slaveRef.path.toString)

  val groupMaster = system.actorOf(RoundRobinGroup(slavePath).props())
//  for(i <- 1 to 10){
//    groupMaster ! s"[$i] Hello from the world!" // slave should receive msg in round robin
//  }

  // we can even configure group by application.conf
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2") // no need to supply pros its already thr in config
  // all the path of slave and already created above in slaveList line
  for(i <- 1 to 10){
    groupMaster2 ! s"[$i] Hello from the world!" // slave should receive msg in round robin
  }

  // handle special message
  groupMaster2 ! Broadcast("Hello, everyone") // this is broadcast to every single actor

  // poisonpill and kill not routed handle by routing actor
  // addRoutee, removeRoutee and getRoutee are handle by only routing actor
}

// now lets learn about dispatchers
// they are in charge of delivering and handling msg inside an actor
// Dispatchers file