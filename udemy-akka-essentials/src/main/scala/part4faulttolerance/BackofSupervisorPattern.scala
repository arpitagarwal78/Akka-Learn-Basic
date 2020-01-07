package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, Kill, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Terminated}
import akka.dispatch.sysmsg.Terminate
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import part4faulttolerance.StartingStoppingActors.Parent.StartChild
import java.io.File
import scala.io.Source
import akka.pattern.{Backoff, BackoffSupervisor}
import scala.concurrent.duration._

object BackoffSupervisorPattern extends App {

  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {

    var dataSource: Source = null

    override def preStart(): Unit = {
      log.info("persistent actor starting..")
    }

    override def postStop(): Unit = {
      log.warning("persistent actor is stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("persistent actor restarting..")
    }

    override def receive: Receive = {
      case ReadFile =>
        if(dataSource==null) {
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/importantdata.txt")) // important.txt
          log.info("I have just read some IMPORTANT data " + dataSource.getLines().toList)
        }
    }
  }
  val system = ActorSystem("BackoffSupervisorPattern")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor])
//  simpleActor ! ReadFile // this will give us data for a file
  // lets change file name to importantdata.txt so that it fails
  // thus we see when we do ReadFile it restart the actor
  // but if the service is down this will again and again crash actor
  // thus this we use backoff supervisor pattern

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasedPersistentActor],
      "SimpleBackOffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

//  val simpleBackOffSupervisor = system.actorOf(simpleSupervisorProps,"SimpleSupervisor")
//  simpleBackOffSupervisor ! ReadFile // supervisor will forward to file and restarted after 3 sec

  // i have created an actor simple supervisor this creates a child called SimpleBackoffActor of type FileBAsedPersitentActor
  // supervisor receive msg and forward to child and strategy is default restart one
  //    - After failure the next attempt is made after 3 sec
  //    - After that next attempt is made after 2x previous attempt -> 3 , 6 , 12 , 24 sec [0.2 adds little amount of time]
  // thus if we run it will start on 3 sec first


  // lets c diff backoff actor which acts on stop and cutomises supervison strategy

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop( // kick in when actor stop
      Props[FileBasedPersistentActor],
      "StopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(OneForOneStrategy() {// to add supervisor strategy
      case _ => Stop // for any kind of exception stop the actor
    })
  )

//  val simpleStopSupervisor = system.actorOf(stopSupervisorProps,"SimpleStopSupervisor")
//  simpleStopSupervisor ! ReadFile // supervisor will forward to file and stop the actor and then backoff kicks in and
  // restarts the actor


  // lets create simpleClass which extends the filebasedactor

  class EagerFBActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("EagerActor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/importantdata.txt"))
    }
  }

  val eagerFBActor = system.actorOf(Props[EagerFBActor])
  // if we run then akka throw actor ini exception bcz of file not found [default strategy is stop the actor]
  // lets create a back off supervisor actor which will start this in exponential delay

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop( // kick in when actor stop
      Props[EagerFBActor],
      "EagerActor",
      1 seconds,
      30 seconds,
      0.1
    ).withSupervisorStrategy(OneForOneStrategy() {// to add supervisor strategy
      case _ => Stop // for any kind of exception stop the actor
    })
  )

  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps,"SimpleEagerSupervisor")
  // the child will die while initialising result in stop of actor
  // trigger supervision strategy which is to stop eager actor
  // then babckoff will kick in after 1 sec then 2 , 4 , 8 , 16 sec max cap 30 sec [after that backoff stop kicking in]
  // thus we can see it retries the start of actor and dies eventually after 30 sec
  // if that file is available after some time then that actor will start



}

// lets learn about schedulers and timers eg. run this code after some time from now or in every interval
// package part5infra