package part3testing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe, EventFilter}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

// this is generally used while Integration test where it is very hard to inject test probes in your system
object InterceptingLogSpec {
// lets have online shop and implement checkout flow

  // checkout actor validate payemnt card with Payment manager which will reply with Accept or Rejected
  // if payment accepted then checkout actor will talk to fulfillment to dispatch the order
  // once fulfillment actor reply back to the checkout actor then checkout actor will commence with other checkout request
  // checkout actor will instantiate two child actor one for payment and one for fulfillment

  case class Checkout(item: String, creditCard: String)
  case class AutorizeCard(creditCard: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed


  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        paymentManager ! AutorizeCard(card) // first authorize the card to payment manager child
        context.become(pendingPayment(item)) // and it wait for that item in new receive context
    }

    def pendingPayment(item: String): Receive = {// handle response from payment actor
      case PaymentAccepted => // free to perceive dispatching order
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied => throw new RuntimeException("I can't handle this anymore.")

    }

    def pendingFulfillment(item: String): Receive ={
      case OrderConfirmed =>
        // switch back to awaiting checkout
      context.become(awaitingCheckout)

    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AutorizeCard(card) => // validation credit card logic here
        if(card.startsWith("0")) {
          sender ! PaymentDenied
        } else {
          Thread.sleep(4000) // lets wait for 4 sec case two in test
          sender ! PaymentAccepted
        }
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {

    var orderId = 43
    override def receive: Receive = {// handle dispatch order for an item
      case DispatchOrder(item) =>
        log.info(s"Order $orderId for item $item has been dispatched.")
        orderId += 1 // lets just increment orderId
      OrderConfirmed
    }
  }
}

// now lets try to test whole flow from checkout actor to fulfillment actor
// it is complex as number of actor interacting with one another plus onw actor creating two child actor thus test probe injection is tough
// plus checkout actor doesnt send any confirmation for success thus dont know what to expect
// thus we have event filters lets see how
class InterceptingLogSpec extends TestKit(ActorSystem("InterceptingLogSpec",
  ConfigFactory.load.getConfig("interceptingLogMessages"))) // intercepting log listner added here
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {
  import InterceptingLogSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "a checkout low" should {


    val item = "rock the jvm akka course"
    val creditCard = "1234-1234-1234-1234"

    "correctly log the dispatch of an order" in {
      // we will write event filter here
      // we will look for log messages even we can get to the statements
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched.", occurrences = 1)  intercept {
        // this scans for all log messages for an info
        // we can match pattern on log or occurrence etc
        // we call intercept to get log messages
        // our test code
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, creditCard)
        // thus this should log message and event filter will catch that with occurrence as 1
        // logger spit the log to standard output thus to spit to event filter we need to configure it in
        // application.conf TestEventListener is the actor which listen to log
        // run test it passes
        // now lets consider a case that our payment accepted message actor takes some time
        // case two in test part added
        // this will delay my whole message and log thus test fail as timeout is 3 sec for Event Filter
        // thus we can configure it as in application.conf filter-leeway
        // then test passes now [goto intercept and see leeway time that is how we added the config for timeout]
        // Event Filter can do info warning error
        // lets c event filter as error lets it give exception payment deny


      }

    }

    val invalidCreditCard = "0000-1243-1312-2131"

    "freak out if the payment denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
        // thus accept runtime exception on 1 time in flow for invalid credit card
      }
    }



  }

  // now lets learn about synchronous testing SynchronousTestingSpec

}
