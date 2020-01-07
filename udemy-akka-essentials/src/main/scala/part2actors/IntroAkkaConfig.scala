package part2actors

import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorRef, ActorSystem, Props, ActorLogging}

object IntroAkkaConfig extends App{

  // first create a new file dummyConfig.conf in same folder
  // check that file

  class SimpleLoggingActor extends Actor with ActorLogging {
    // actor loggin is a trait
    override def receive: Receive = {
      case message =>
        log.info(s"Receive : $message")
    }
  }


  // 1. inline configuration

  val configString =
    """
      | akka {
      |   loglevel = "ERROR"
      |   }
    """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config)) // so configuration actor is loaded in this context
  val actor = system.actorOf(Props[SimpleLoggingActor], "simpleLogActor")
  actor ! "this is a test message"

  // lets create now a simple logging actor
//  /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java "-javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=57475:/Applications/IntelliJ IDEA.app/Contents/bin" -Dfile.encoding=UTF-8 -classpath /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/tools.jar:/Users/arpiagar/Arpit/WorkingDir/Temp/AkkaProjects/udemy-akka-essentials/target/scala-2.12/classes:/Users/arpiagar/.sbt/boot/scala-2.12.7/lib/scala-library.jar:/Users/arpiagar/.ivy2/cache/com.typesafe/config/bundles/config-1.3.2.jar:/Users/arpiagar/.ivy2/cache/com.typesafe.akka/akka-actor_2.12/jars/akka-actor_2.12-2.5.13.jar:/Users/arpiagar/.ivy2/cache/com.typesafe.akka/akka-testkit_2.12/jars/akka-testkit_2.12-2.5.13.jar:/Users/arpiagar/.sbt/boot/scala-2.12.7/lib/scala-reflect.jar:/Users/arpiagar/.ivy2/cache/org.scala-lang.modules/scala-java8-compat_2.12/bundles/scala-java8-compat_2.12-0.8.0.jar:/Users/arpiagar/.ivy2/cache/org.scala-lang.modules/scala-xml_2.12/bundles/scala-xml_2.12-1.0.6.jar:/Users/arpiagar/.ivy2/cache/org.scalactic/scalactic_2.12/bundles/scalactic_2.12-3.0.5.jar:/Users/arpiagar/.ivy2/cache/org.scalatest/scalatest_2.12/bundles/scalatest_2.12-3.0.5.jar part2actors.IntroAkkaConfig
//    [DEBUG] [04/13/2019 11:44:34.506] [main] [EventStream(akka://ConfigurationDemo)] logger log1-Logging$DefaultLogger started
//  [DEBUG] [04/13/2019 11:44:34.507] [main] [EventStream(akka://ConfigurationDemo)] Default Loggers started
//    [INFO] [04/13/2019 11:44:34.580] [ConfigurationDemo-akka.actor.default-dispatcher-4] [akka://ConfigurationDemo/user/simpleLogActor] Receive : this is a test message

  // instead of info we get lot of debug as loglevel set to debug fpr actor system
  // lets change it back to info
  // then we only see info message which we log
//  /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java "-javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=57482:/Applications/IntelliJ IDEA.app/Contents/bin" -Dfile.encoding=UTF-8 -classpath /Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/deploy.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/javaws.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jfxswt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/management-agent.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/plugin.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/ant-javafx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/dt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/javafx-mx.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/jconsole.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/packager.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/sa-jdi.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/lib/tools.jar:/Users/arpiagar/Arpit/WorkingDir/Temp/AkkaProjects/udemy-akka-essentials/target/scala-2.12/classes:/Users/arpiagar/.sbt/boot/scala-2.12.7/lib/scala-library.jar:/Users/arpiagar/.ivy2/cache/com.typesafe/config/bundles/config-1.3.2.jar:/Users/arpiagar/.ivy2/cache/com.typesafe.akka/akka-actor_2.12/jars/akka-actor_2.12-2.5.13.jar:/Users/arpiagar/.ivy2/cache/com.typesafe.akka/akka-testkit_2.12/jars/akka-testkit_2.12-2.5.13.jar:/Users/arpiagar/.sbt/boot/scala-2.12.7/lib/scala-reflect.jar:/Users/arpiagar/.ivy2/cache/org.scala-lang.modules/scala-java8-compat_2.12/bundles/scala-java8-compat_2.12-0.8.0.jar:/Users/arpiagar/.ivy2/cache/org.scala-lang.modules/scala-xml_2.12/bundles/scala-xml_2.12-1.0.6.jar:/Users/arpiagar/.ivy2/cache/org.scalactic/scalactic_2.12/bundles/scalactic_2.12-3.0.5.jar:/Users/arpiagar/.ivy2/cache/org.scalatest/scalatest_2.12/bundles/scalatest_2.12-3.0.5.jar part2actors.IntroAkkaConfig
//    [INFO] [04/13/2019 11:45:46.487] [ConfigurationDemo-akka.actor.default-dispatcher-3] [akka://ConfigurationDemo/user/simpleLogActor] Receive : this is a test message

  // if change to ERROR we wont see any message except critical message


  // SO above was method 1 with inline string we can pass in
  // method 2 is configuration in a file which is stored in resources inside main
  // application.conf name is imp

  // lets create other system after that

  val defaultConfigFileDemo = ActorSystem("defaultConfigFileDemoActor") // so configuration file loaded automatically for application.conf
  val actor2 = defaultConfigFileDemo.actorOf(Props[SimpleLoggingActor], "simpleLogActorForFile")
  actor2 ! "this is a test 2 message"


  // method 3 seperate configuration on same file application.conf
  // lets create actor system
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig") // my special config
  val specialConfigDemo = ActorSystem("specialConfigDemoActor", specialConfig) // so configuration file loaded automatically for application.conf
  val actor3 = specialConfigDemo.actorOf(Props[SimpleLoggingActor], "simpleLogActorForSpecialConfig")
  actor3 ! "this is a test 3 message"

  // method 4 save configuration in different files -> secrectFolder -> secretConfiguration

  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"Separate config lof level ${separateConfig.getString("akka.loglevel")}") // we can create its actor system accordingly

  // method 5 load configuration from different file format
  // eg json , properties
  // resources -> jsonFOlder -> json config file

  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"jsonConfig lof level ${jsonConfig.getString("akka.loglevel")}") // we can create its actor system accordingly

  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"propsConfig lof level ${propsConfig.getString("akka.loglevel")}") // we can create its actor system accordingly

  // lets learn about testing AKKA --> package [part3testing] [BasicSpec]


}
