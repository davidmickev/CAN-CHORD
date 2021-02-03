package CAN
import java.lang.Thread.sleep

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Simulation {
  // Initialize zones and Huge config file inserted into CAN
  val config: Config = ConfigFactory.load("application.conf")

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[Driver.lookup] = ActorSystem(Driver(), "driver")
    var dictionary: Map[String, String] = Map.empty[String, String]
    // Creating dictionary defined in config file: application.conf
    dictionary = config.as[Map[String, String]]("dictionary")
    dictionary.foreach(movie_data => system ! Driver.lookup(movie_data._1, system.ref))

  }
}

object Driver {
  import User.{insertConfig, lookup, Command}


  final case class lookup(movieTitle: String, replyTo: ActorRef[Driver.lookup]) extends Command
  final case class queryResponse(value: String)
  // Command to gracefully stop the Actor
  case object Passivate extends Command

  def apply(): Behavior[lookup] =
    Behaviors.setup { context =>
      val DNS = context.spawn(CAN.DNS(), "DNS")
      context.log.info("DNS Actor Created: " + DNS.path.name)
      // Sleep to Construct Nodes
      // used by 'time' method
      implicit val baseTime: Long = System.currentTimeMillis
      // 2 - create a Future
      val f = Future {
        sleep(100)
      }
      // Non-blocking
      f.onComplete {
        case Success(value) => println(s"Zones and nodes initialized! = $value")
        case Failure(e) => e.printStackTrace()
      }
      val user = context.spawn(User(), "User")
      context.log.info("User Actor Created: " + user.path.name)
      // InsertConfig
      user ! insertConfig(Simulation.config)

      Behaviors.receiveMessage { message =>
        /*
        context.log.info("Ready for some key lookups" + user.path.name)
        val replyTo = context.spawn(User(), message.key)
        user ! User.("MoneyTrain", replyTo)
        val value = Some(DNS.getDNSActor.dictionary(message.key))
        replyTo ! User.queryResponse(message.key, value)
        */
        Behaviors.same
      }
    }

}
/*
class Driver(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {

  context.log.info("Driver Application started")

  import Driver.{lookup, queryResponse, Passivate}
  import DNS.keyLookup
  var movieTitleResponse: String = "diehard"

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    msg match {
      case lookup(movieTitle, replyTo) =>
        context.log.info("Command to read temperature received")
        Driver.DNS ! keyLookup(movieTitle)
        replyTo ! lookup(movieTitle, replyTo)
        this
      case queryResponse(value) =>
        movieTitleResponse = value

      case Passivate =>
        Behaviors.stopped
    }
  }
}

 */