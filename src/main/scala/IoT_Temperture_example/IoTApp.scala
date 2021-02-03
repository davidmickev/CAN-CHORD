package IoT_Temperture_example

import akka.actor.typed.ActorSystem

object IoTApp {
  def main(args: Array[String]): Unit = {
    // Create ActorSystem and top level supervisor
    ActorSystem[Nothing](IotSupervisor(), "iot-system")
  }
}
