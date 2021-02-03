package CANTesting

import java.lang.Thread.sleep

import CAN.User
import CAN.User.insertConfig
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/* Verifies our CAN algorithm */
class CANSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike{
  import CAN.User.lookup
  // Initialize zones and Huge config file inserted into CAN
  val config: Config = ConfigFactory.load("application.conf")
  var dictionary: Map[String, String] = Map.empty[String, String]
  // Creating dictionary defined in config file: application.conf
  dictionary = config.as[Map[String, String]]("dictionary")


  "User actor import values from application.conf" in {
    val DNS = spawn(CAN.DNS(), "DNS")
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
    //
    val user = spawn(User(), "User")
    val config: Config = ConfigFactory.load("simpleData.conf")
    // CAN setup with (K, V) pairs inserted
    user ! insertConfig(config)
  }

  "CAN movie title lookups" in {
    val DNS = spawn(CAN.DNS(), "DNS")
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

    val user2 = spawn(User(), "User")

    val config: Config = ConfigFactory.load("application.conf")
    user2 ! insertConfig(config)
    // Lookup every movie title placed into CAN (100 movie titles to enforce)
    dictionary.foreach(movie => user2 ! lookup(movie._1))
    // One minute wait
    sleep(6000)
    // Response
  }

}
