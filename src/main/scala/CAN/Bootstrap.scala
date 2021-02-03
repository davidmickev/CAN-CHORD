package CAN

import akka.actor.ActorPath
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.util.Random

object Bootstrap{
  def apply():  Behavior[Command] = Behaviors.setup(context => new Bootstrap(context))

  trait Command
  /** For setting up initial nodes when DNS is created */
  case class initializeZones() extends Command
  /** For new nodes that want access to an existing node in CAN */
  case class getNodeInNetwork(p: Procedure[Node.Command]) extends Command
}

/** Maintains a list of active nodes in the content-addressable network */
class Bootstrap(context: ActorContext[Bootstrap.Command]) extends AbstractBehavior[Bootstrap.Command](context){
  // Importing relevant commands
  import Node.{acquiredNodeInNetwork,findZone,setZone,initializeNeighbors}
  import Bootstrap._
  import DNS.{insert, keyLookup}
  /** List of up and running nodes in network */
  var active_nodes: List[ActorRef[Node.Command]] = List.empty[ActorRef[Node.Command]]

  override def onMessage(msg: Bootstrap.Command): Behavior[Bootstrap.Command] = {
    msg match {
      /* For setting up initial nodes when DNS is created */
      case initializeZones() =>
        initializeNeighborsFromBoot()

      /* For new nodes that want access to an existing node in CAN (Randomly chosen) */
      case getNodeInNetwork(p) =>
        val nodeInNetwork = getRandomNode
        val new_node = p.getReference.get
        new_node ! acquiredNodeInNetwork(Procedure[Node.Command]().withReference(nodeInNetwork))
        context.log.info(s"$thisPath: New Node Procedure :: acquiredNodeInNetwork(Procedure) => New Node")

      /* For insertion of new (key, Value) into distributed map
        TODO: change to one random node only, AFTER routing (Zone.closetToP) is completed */
      case insert(p) =>
        active_nodes.head !  findZone(p)
        context.log.info(s"$thisPath: Insert ${p.getDataToStore.get} Procedure :: findZone(kv) => Node In Network")

      case keyLookup(p) =>
        active_nodes.head !  findZone(p)
        context.log.info("$thisPath: Key Lookup DONE")
    }
    this
  }


  /**  init 16x16 conceptual grid into 4 coordinate planes ( + ) where center of + is ( x = 7, y = 7 )
      Self defined circular coordinate plane ( + ) meaning List (1) would be top left,
      then (2) right, then (3) below 1 and 4 below 2
      1 = (0,7),(0,7)  -> (x,y) = (0,0),(7,7)
      2 = (7,15),(0,7) -> (x,y) = (7,0),(15,7)
      3 = (0,7),(7,15) -> (x,y) = (0,7),(7,15)
      4 = (7,15),(7,15)-> (x,y) = (7,7),(15,15)                                                   */
  def initializeNeighborsFromBoot(): Unit = {
    context.log.info("BOOTSTRAP: INITIALIZING FIRST FOUR NODES IN C.A.N.")
    val initialZones = List(Zone((0, 7), (0, 7)),
                            Zone((7, 15), (0, 7)),
                            Zone((0, 7), (7, 15)),
                            Zone((7, 15), (7, 15)))
    // Four nodes created and assigned zones
    for(i <- 0 until 4){
      val new_node = context.spawn(Node(),s"CAN-node-$i")
      active_nodes +:= new_node
      new_node ! setZone(Procedure[Node.Command]().withZone(initialZones(i)))
    }
    // Each node will query each other node and set neighbors appropriately
    active_nodes.foreach(node => {
      node ! initializeNeighbors(active_nodes)
    })
  }

  /** To obtain an arbitrary active content-addressable network node */
  def getRandomNode: ActorRef[Node.Command] = active_nodes(new Random().nextInt(active_nodes.length))

  /** Alias for context.self.path */
  def thisPath: ActorPath = context.self.path
}

