package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}
import akka.dispatch.sysmsg.Terminate
import part4faulttolerance.StartingStoppingActors.Parent.StartChild

object ActorLifecycle extends App {

  object StartChild
  class LifecycleActor extends Actor with ActorLogging {


    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }

    // preStart call once actor is initialise
    override def preStart(): Unit = {
      log.info("I am starting")
    }

    // preStart call once actor is stop
    override def postStop(): Unit = {
      log.info("I am stopped")
    }

    // both the method postStop and preStart do nothing
  }

  val system = ActorSystem("LifecycleDemo")
//  val parent = system.actorOf(Props[LifecycleActor], "parent")
//  parent ! StartChild // this will trigger the parent to create a child
//  parent ! PoisonPill // kill the parent [child start first then the parent stops]

  // thus first parent starts then child and methog preStart is called first then later postStop on stopping
  //    DEBUG] [04/29/2019 08:55:16.654] [main] [EventStream(akka://LifecycleDemo)] logger log1-Logging$DefaultLogger started
  //    [DEBUG] [04/29/2019 08:55:16.656] [main] [EventStream(akka://LifecycleDemo)] Default Loggers started
  //    [INFO] [04/29/2019 08:55:16.749] [LifecycleDemo-akka.actor.default-dispatcher-3] [akka://LifecycleDemo/user/parent] I am starting
  //    [INFO] [04/29/2019 08:55:16.751] [LifecycleDemo-akka.actor.default-dispatcher-2] [akka://LifecycleDemo/user/parent/child] I am starting
  //    [INFO] [04/29/2019 08:55:16.760] [LifecycleDemo-akka.actor.default-dispatcher-4] [akka://LifecycleDemo/user/parent/child] I am stopped
  //    [INFO] [04/29/2019 08:55:16.762] [LifecycleDemo-akka.actor.default-dispatcher-2] [akka://LifecycleDemo/user/parent] I am stopped



  // lets see restart hooks

  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor with ActorLogging {

    val child = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check

    }
  }

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("Supervised child started")

    override def postStop(): Unit = log.info("Supervised child stop")

    // this is called by old actor reference before it is swapped
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.info(s"Supervised actor restarting because of ${reason.getMessage}")

    }


    // this is called by new actor ref after swap
    override def postRestart(reason: Throwable): Unit = {
      log.info("Supervised Actor restarted")

    }

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now ")
        throw new RuntimeException("I failed")
      case Check =>
        log.info("alive and kicking")
    }
  }


  val supervisor = system.actorOf(Props[Parent], "Supervisor")
  // let us fail the child
  supervisor ! FailChild
  supervisor ! CheckChild // inspite of exception actor is restarted and it handles the message once restarted completed
//  /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java "-javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=60538:/Applications/IntelliJ IDEA.app/Contents/bin" -Dfile.encoding=UTF-8 -classpath /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/tools.jar:/Users/arpiagar/Arpit/WorkingDir/Temp/AkkaProjects/udemy-akka-essentials/target/scala-2.12/classes:/Users/arpiagar/.sbt/boot/scala-2.12.7/lib/scala-library.jar:/Users/arpiagar/.ivy2/cache/com.typesafe/config/bundles/config-1.3.2.jar:/Users/arpiagar/.ivy2/cache/com.typesafe.akka/akka-actor_2.12/jars/akka-actor_2.12-2.5.13.jar:/Users/arpiagar/.ivy2/cache/com.typesafe.akka/akka-testkit_2.12/jars/akka-testkit_2.12-2.5.13.jar:/Users/arpiagar/.sbt/boot/scala-2.12.7/lib/scala-reflect.jar:/Users/arpiagar/.ivy2/cache/org.scala-lang.modules/scala-java8-compat_2.12/bundles/scala-java8-compat_2.12-0.8.0.jar:/Users/arpiagar/.ivy2/cache/org.scala-lang.modules/scala-xml_2.12/bundles/scala-xml_2.12-1.0.6.jar:/Users/arpiagar/.ivy2/cache/org.scalactic/scalactic_2.12/bundles/scalactic_2.12-3.0.5.jar:/Users/arpiagar/.ivy2/cache/org.scalatest/scalatest_2.12/bundles/scalatest_2.12-3.0.5.jar part4faulttolerance.ActorLifecycle
//    [DEBUG] [04/29/2019 09:15:56.900] [main] [EventStream(akka://LifecycleDemo)] logger log1-Logging$DefaultLogger started
//  [DEBUG] [04/29/2019 09:15:56.901] [main] [EventStream(akka://LifecycleDemo)] Default Loggers started
//    [INFO] [04/29/2019 09:15:56.977] [LifecycleDemo-akka.actor.default-dispatcher-2] [akka://LifecycleDemo/user/Supervisor/supervisedChild] Supervised child started
//    [WARN] [04/29/2019 09:15:56.979] [LifecycleDemo-akka.actor.default-dispatcher-2] [akka://LifecycleDemo/user/Supervisor/supervisedChild] child will fail now
//  [ERROR] [04/29/2019 09:15:56.985] [LifecycleDemo-akka.actor.default-dispatcher-4] [akka://LifecycleDemo/user/Supervisor/supervisedChild] I failed
//    java.lang.RuntimeException: I failed
//    at part4faulttolerance.ActorLifecycle$Child$$anonfun$receive$3.applyOrElse(ActorLifecycle.scala:84)
//  at akka.actor.Actor.aroundReceive(Actor.scala:517)
//  at akka.actor.Actor.aroundReceive$(Actor.scala:515)
//  at part4faulttolerance.ActorLifecycle$Child.aroundReceive(ActorLifecycle.scala:62)
//  at akka.actor.ActorCell.receiveMessage(ActorCell.scala:588)
//  at akka.actor.ActorCell.invoke(ActorCell.scala:557)
//  at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:258)
//  at akka.dispatch.Mailbox.run(Mailbox.scala:225)
//  at akka.dispatch.Mailbox.exec(Mailbox.scala:235)
//  at akka.dispatch.forkjoin.ForkJoinTask.doExec(ForkJoinTask.java:260)
//  at akka.dispatch.forkjoin.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1339)
//  at akka.dispatch.forkjoin.ForkJoinPool.runWorker(ForkJoinPool.java:1979)
//  at akka.dispatch.forkjoin.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
//
//    [INFO] [04/29/2019 09:15:56.985] [LifecycleDemo-akka.actor.default-dispatcher-3] [akka://LifecycleDemo/user/Supervisor/supervisedChild] Supervised actor restarting because of I failed
//    [INFO] [04/29/2019 09:15:56.987] [LifecycleDemo-akka.actor.default-dispatcher-3] [akka://LifecycleDemo/user/Supervisor/supervisedChild] Supervised Actor restarted
  // THus if child actor throws an exception the message which cause the actor to die is removed from queue and restarted the actor
  // thus this is the default supervision strategy
  // we will learn more about it later
  // lets now learn about Supervision [Handle our children actor failure]
  // if actor fail it suspends its children and send message to parent and parent decide what next to happen
  // thus it can resume the actor, stop the actor or restart the actor or can escalate its failure and fail itself
  // SupervisionSpec


}
