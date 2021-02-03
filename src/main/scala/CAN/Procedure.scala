package CAN

import akka.actor.typed.ActorRef

object Procedure extends Enumeration {
  def apply[T](): Procedure[T] = new Procedure()

  /** To identify purpose once zone is found from routing to it */
  type routing_type = Value
  val KEY_STORE, KEY_LOOKUP, NEW_NODE = Value
}

/**  A Procedure Instance can encapsulate the follow information
*   reference:          For the next send or a reply to
*   visited:            For book-keeping visited nodes
*   neighborsToUpdate:  For updating node when a split has occurred
*   location:           This is the P location (From: new_node -> random P or key -> hashed -> deterministic (X, Y))
*   keyValue:           Key value to store at location
*   keyLookup           Key to query DHT map at the node key was mapped to
*   routingPurpose:     For next actions based on the purpose of finding a zone
*   zone:               For zone assignment/querying
* */
case class Procedure[T](routingPurpose: Option[Procedure.routing_type] = None,
                        visited: Option[List[ActorRef[Node.Command]]] = None,
                        neighborsToUpdate: Option[List[Neighbor]] = None,
                        neighbor: Option[ActorRef[Node.Command]] = None,
                        key_lookup: Option[String] = None,
                        key_value: Option[(String, String)] = None,
                        location: Option[(Double, Double)] = None,
                        reference: Option[ActorRef[T]] = None,
                        zone: Option[Zone] = None,
                        user: Option[ActorRef[User.Command]] = None,
                        split: Option[(ActorRef[Node.Command], ActorRef[Node.Command])] = None,
                        KV_transfers: Option[Map[String, String]] = Some(Map())
                       ) {
  import Procedure.routing_type

  /** Returns the routing purpose associated with the find zone command */
  def getRoutingPurpose: Option[routing_type] = routingPurpose
  /** Returns the neighbor reference associated with the set neighbor command */
  def getNeighbor : Option[ActorRef[Node.Command]] = neighbor
  /** Returns the request (key ,value) to store in distributed hash map */
  def getDataToStore: Option[(String, String)] = key_value
  /** Returns the key associated with the key look up command */
  def getKeyQuery: Option[String] = key_lookup
  /** Returns the location associated with the find zone command */
  def getLocation: Option[(Double, Double)] = location
  /** Returns the reference associated with multiple commands */
  def getReference: Option[ActorRef[T]] = reference
  /** Returns the user reference associated with key look up or key_value store commands */
  def getUser: Option[ActorRef[User.Command]] = user
  /** Returns the zone associated with multiple commands */
  def getZone : Option[Zone] = zone
  /** Returns a list of key_value pairs to transfer associated with the join network command */
  def getKeyValueTransfers: Option[Map[String, String]] = KV_transfers

  def withUser(u: ActorRef[User.Command]): Procedure[T] =
    this.copy(user = Some(u))

  def withKeyToFind(s: String):  Procedure[T] =
    this.copy(key_lookup = Some(s))

  def withKeyValueToStore(pair: (String, String)): Procedure[T] =
    this.copy(key_value = Some(pair))

  def withLocation(xy: (Double, Double)) : Procedure[T] =
    this.copy(location = Some(xy))

  def withNeighborsToUpdate(n: List[Neighbor]): Procedure[T] =
    this.copy(neighborsToUpdate = Some(n))

  def withRoutingPurpose(rp: routing_type): Procedure[T] =
    this.copy(routingPurpose = Some(rp))

  def withReference(r: ActorRef[T]): Procedure[T] =
    this.copy(reference = Some(r))

  def withNeighbor(n: ActorRef[Node.Command]): Procedure[T] =
    this.copy(neighbor = Some(n))

  def withZone(z: Zone): Procedure[T] =
    this.copy(zone = Some(z))

  def withOccupant(split: (ActorRef[Node.Command], ActorRef[Node.Command])): Procedure[T] =
    this.copy(split = Some(split))

  def withVisited(v: ActorRef[Node.Command]): Procedure[T] = {
    visited match {
      case Some(list) =>  this.copy( visited = Some(v :: list) )
      case None =>        this.copy( visited = Some(List(v)) )
    }
  }

  def withKeyValueTransfer(key: String, value: String): Procedure[T] = {
    KV_transfers match{
      case None => this.copy(KV_transfers = Some(Map(key -> value)))
      case Some(map) => this.copy(KV_transfers = Some(map + (key -> value)))
    }
  }


  /**  To check if a neighbor was visited to then forward
   * or NOT forward depending on return value            */
  def wasVisited(v: ActorRef[Node.Command]): Boolean = {
    visited match {
      case Some(list) =>  list.contains(v)
      case None =>        false
    }
  }
}
