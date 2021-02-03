package CAN

import akka.actor.ActorPath
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object DNS{
  // Allowing one instances of DNS actor for now, To simulate HTTP connection
  private var DNS_Singleton: Option[DNS] = None
  // Apply
  def apply(): Behavior[Command] =
    Behaviors.setup(context => {
      DNS_Singleton = Some(new DNS(context))
      DNS_Singleton.get
    })
  // Gets the DNS instance, simulating HTTP request
  def getDNSActor: DNS = DNS_Singleton.get

  trait Command
  /** Acquire A Bootstrap Node Procedure: A node requesting to gone space is
   * querying DNS for bootstrap node */
  case class bootstrap(p: Procedure[Node.Command]) extends Command
  /** Insert Procedure: Command is passed down by User to DNS, then to a Bootstrap node,
   * then to a Node in the network. Finally the routing to the node/zone of insertion begins
   * with a FindZone command */
  case class insert(p: Procedure[Node.Command]) extends Command with Bootstrap.Command
  case class keyLookup(p: Procedure[Node.Command]) extends Command with Bootstrap.Command
}

class DNS(context: ActorContext[DNS.Command]) extends AbstractBehavior[DNS.Command](context) {
  import DNS.{insert,bootstrap, keyLookup}
  import Node.acquiredBootstrap
  import Bootstrap.initializeZones

  var boots = 1
  var bootstraps: List[ActorRef[Bootstrap.Command]] = List(context.spawn(Bootstrap(), s"node-bootstrap-$boots"))
  context.log.info("DNS CREATED")
  bootstraps.head ! initializeZones()

  /**********************      COMMAND PROCESSING       **********************/
  override def onMessage(msg: DNS.Command): Behavior[DNS.Command] = {
    msg match {
      case bootstrap(procedure) =>
        procedure.getReference.get ! acquiredBootstrap(Procedure[Bootstrap.Command]().withReference(bootstraps.head))
        context.log.info(s"$thisPath: acquiredBootstrap(procedure) => New Node")

      // DNS to Boot to Zone,
      // for item in config file, we receive the (key,value) and send to bootstrap here.
      case insert(procedure) =>
        context.log.info(this.getClass + s" : inserting(${procedure.getDataToStore.get}) => Bootstrap ")
        bootstraps.head ! insert(procedure)

      case keyLookup(procedure) =>
        ///context.log.info(this.getClass + s" : Key Lookup(${procedure.getDataToStore.get}) => Bootstrap ")
        bootstraps.head ! keyLookup(procedure)

    }
    this
  }
  /**********************     END OF COMMAND PROCESSING       **********************/


  /* User obtain ActorRef to DNS Singleton */
  def connectToDNS: ActorRef[DNS.Command] = context.self

  // Alias for context.self.path
  def thisPath: ActorPath = context.self.path
}
