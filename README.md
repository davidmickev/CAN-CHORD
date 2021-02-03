# CS441_Fall2020_Final_Project
[Repository Link](https://bitbucket.org/jsanch75/group_13/src/master/)

# Group Members
Deividas Mickevicius : dmicke5

Jacob Sanchez : jsanch75

Alex Jalomo : ajalom2

# Prerequisites
Installed [sbt-1.4.1](https://www.scala-sbt.org/download.html) 

## Instructions
* Setup
    * Download the project [source](https://bitbucket.org/jsanch75/group_13/src/master/) 
    * or CL `git clone https://mttdavid@bitbucket.org/jsanch75/group_13_courseproject.git`
    * Open/import the project in your IDE:

* Options to run
    * `sbt clean compile run`
    
* Testing
    * `sbt clean compile test`
    
* EC2 AWS Instance
    * Documentation on cloud operations and setup described at the bottom of the project
        
# Docker Instance: 

### Prerequisites
    
Install [Docker](https://www.docker.com/get-started)

## Instructions 

1. Open docker CLI
2. Project [source](https://hub.docker.com/layers/129769520/mttdavid/can-chord/course-project/images/sha256-dd86ad599e9a2168849acda91f3020072cf9a1829529b06125ac1b5db20e3ec4?context=explore) 
    * Use `docker pull mttdavid/can-chord:course-project`
3. docker run -it (Image-Name)
4. `sbt clean compile run` 

# Citation
Software and design is based on: 

* Content-Addressable Network (CAN) as a distributed infrastructure that provides hash table-like functionality on Internet-like
scales. The CAN is scalable, fault-tolerant and completely self-organizing,
and we demonstrate its scalability, robustness and low-latency properties
through simulation.

* Chord: A Scalable Peer-to-peer Lookup Service for Internet Applications

* Current version of Akka

The source to the documentation can be found at:

https://people.eecs.berkeley.edu/~sylvia/papers/cans.pdf

http://www.diva-portal.org/smash/get/diva2:836192/FULLTEXT01.pdf

https://pdos.csail.mit.edu/papers/chord:sigcomm01/chord_sigcomm.pdf

https://doc.akka.io/docs/akka/current/

# Visualization with R for Chord

Utilizing the Circilize methodology we map the values from the configuration file into R representing which location each Movie gets assigned to
the chord. There were 5 different algorithm procedures in R all utilizing the SHA-1 methodology to encrypt the name.
The results on the image become very pixelated as the size of entries get larger.

![Alt text](Images/Rplot.png?raw=true "CHORD")


# Design Architecture and Implementations

We are using typed Akka Behaviors model system to interact and manipulate Nodes through actors.
The distributed data stored can be found in 'aplication.conf' which contains (key,value)->(Movie, "Year,Revenue") pairs.

The movies dataset that was used can be found on [kaggle](https://www.kaggle.com/rounakbanik/the-movies-dataset?select=movies_metadata.csv)
and was parsed with the parser.py script located under PythonParser that takes the first valid 250+ movies that have a title and budget/year.

Actors that simulate nodes in the simulated cloud have corresponding hash values are generated using unique names that will be assigned to these nodes and they will be inserted based on those hashes

## Content-Addressable Network (CAN)

Our implementation creates a 2-dimensional [0, 16.00]x[0, 16.00] coordinate space for the CAN. 
The first node to join the CAN becomes the owner of the entire CAN space. For every new node that enters, if a zone is split, that node becomes the owner of that space.
                
#### Abstractions: Procedures
Our implementation includes via Akka Actor messages/commands to accomplish functionalities 
such as:
 
 • The insertion of movie data
 
 • The retrieval of movie data
 
 • The insertion of a new node into the CAN
 
Such functionalities require much distinct and indistinct information. 
The trivial way to ensure critical information is present, is to add numerous 
parameters to distinct messages/commands. 
In order to reduce commands having numerous parameters, 
the case class Procedure[T] is introduced in our implementation. 

A Procedure instance essentially encapsulates all required information for 
a given procedures. A procedure can be the rout or actions taken to produce
the mentioned functionalities. Information can be extracted form the Procedure 
instances once the destination has been reached. The extraction of information 
can be unsafe if the Procedure instance does not encapsulate required information.
The future version of Procedure will try to enforce required data at compile time via
Scala phantom types. CHORD in the future will be reimplemented to take advantage 
of the generality and clearness that Procedure gives.                         

#### Architecture And Construction Of A CAN
Our architecture and construction of a CAN overlay consists of three steps:
##### Entities And Responsibilities:
###### DNS Actor
   • Singleton Actor that simulations the entry point for a given User Actor.
   
   • Communication point for user to insert and retrieve data.
   
   • DNS forward data insertion and data retrievals procedures to a bootstrap node.

###### Bootstrap Actor
   • First bootstrap node is responsible for the first four zones/nodes initializations.
   
   • A bootstrap node forwards data insertion and data retrievals procedures 
    to an arbitrary active node in the network.
   
   • A bootstrap node reboots failed CAN nodes for which it is responsible for.

###### CAN Node Actor 
   • Determines optimal next neighbor to forward procedure if its zone does not contain the desired destination.

   • Updates/Initializes its neighbors_table appropriately on command.

   • Send a split procedure to itself when congested, meaning the node is responsible for to much data.

    
##### Overlay routing.
   • The nodes are able to route messages in the CAN overlay utilizing only information about neighbouring nodes
and their zones. 
   
   • Since the CAN space is a 2-dimensional coordinate grid, this becomes a matter of routing along a straight
line. (Vector (x,x),(y,y) to P)


### Further Overview CAN Driver

We use typed Akka model system to interact and simulate Content Addressable Network.
Our (CAN) is a robust, scalable, distributed systems designed for efficient search of data
stored in a DHT. 

#### Overview Implementations
 
To send immutable messages between typed actor models to construct and manipulate Nodes within CAN
Each can stores a chunk- called a zone of entire hash table containing multiple key value pairs
Each node holds information about a small number of adjacent zones in the table (neighbor table)
Can space is divided amongst nodes, it can grow incrementally by allocating its own portion of coordinate space to another node by splitting half of its allocated zone in half retaining half and giving other part to new node

[ 1 ] -> [ 1|2 ]  (Same Area)
1)	New node finds a node in CAN
2)	Find a node whose zone will be split
3)	Update neighbors on split so routing can include new node

Bootstrap – New CAN node can discover IP address of any node currently in system -> Use some bootstrap mechanism

Assume can has associated DNS and this resolves IP address of one or more CAN bootstrap nodes.

Bootstrap nodes maintain partial list of CAN nodes in the system

New node -> Looks up CAN domain name in DNS to retrieve boostrap IP address.

Bootsrap then supplies IP address of several random chosen nodes in system

Finding a zone – New node randomly chooses point P in space and sends a join request for destination P

Then node splits its zone in half and assigns one half to new node.

For 2-d space, zone is split in X then Y dimension. 

Then (key,value) pairs from the half zone are handed over to new node

Joining the routing (update) update neighbors to eliminate and update old / new neighbors

Node Leave - If no neighbors. It becomes empty space, otherwise if has 2 people in one node, its given to neighbor whose zone is smallest

When node fails/leaves/dies it initiates takeover mechanism and starts a takeover 
When timer expires a node sends TAKEOVER message conveying its own zone volume to all of failed node’s neighbors

On receipt of TAKEOVER msg, a node cancels its own timer if the zone volume in the message is smaller that its zone value or replies with its own takeover msg.

This just ensures neighbors are updates and neighboring node is chosen while still active/alive

Zone overloading - (repeated (key,value) pairs that are frequently accessed)

Advantages : reduced path length, number of hops, which is less latency, 
Improved fault tolerance because a zone is vacant only when all the nodes in a zone crash at same time
Negatives: overloading adds complexity because nodes must track set of peers

On join – send message to random point on space, the existing node in space knows its zone coordinates and those of its neighbors, and  instead of directly splitting zone, the first node compares the volume of its zone to its neighbors in the coordinate space to accommodate the split.

Total volume = V(t) and n is total number on nodes which will be assigned to a zone of voume V(t/n) to each node

Caching and replication techniques

Replication: A overloaded(work) node can replicate the key and data at each of neighboring nodes for load balancing

#### EVALUATION SYSTEMS
    *    Entry | Direction
    1.     0   | Left
    2.     1   | Up
    3.     2   | Right
    4.     3   | Down
    5.     4   | default (self)

#### RunTime 

For a d dimensional space partitioned into n equal zones, the average routing path length is (d/4)(^1/d) and individual nodes maintain 2d neighbors. And path length grows at O(n^1/d)

## Chord

Implementation details can be found in [previous project repository](https://bitbucket.org/jsanch75/group_13/src/master/).
The focus of this README.md discussed the implementation and design of [CAN](https://people.eecs.berkeley.edu/~sylvia/papers/cans.pdf).

# AWS EC2

Setup tutorials used

   * https://docs.aws.amazon.com/AmazonECS/latest/developerguide/getting-started-ecs-ec2.html
   * https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-user.html
   * https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html

1. Setup ECS AWS AdministratorAccess user with IAM policy.
2. Register a task definition under https://console.aws.amazon.com/ecs/
    1. Select launch type compatibility page, select EC2 
    2. Configure task and container definitions page, scroll down and choose Configure via JSON.
    3. Save
![Alt text](Images/Tasks.png?raw=true "Tasks")
3. Create Cluster
    1. On the Clusters page, choose Create Cluster.
    2. On the Select cluster template page, choose EC2 Linux + Networking.
    3. For EC2 instance type, choose either the t2.micro or t3.micro
    4. Number of instances, type 1
    5. EC2 Ami Id, use the default value which is the Amazon Linux 2 Amazon ECS-optimized AMI. 
    6. In the Networking section, for VPC choose either Create a new VPC to have Amazon ECS create a new VPC for the cluster to use
    7. In the Container instance IAM role section, choose Create new role to have Amazon ECS create a new IAM role for your container instances, or choose an existing Amazon ECS container instance (ecsInstanceRole) role that you have already created. For more information, see Amazon ECS Container Instance IAM Role.
![Alt text](Images/Clusters.png?raw=true "Cluster")
4. Create a service
    1. In the navigation pane, choose Clusters.
    2. Select the cluster you created in the previous step.
    3. Services tab, choose Create.
    4. Configure service section, do the following:
        1. Launch type, select EC2
        2. Name your app
        3. Select previous cluster
        4. Select your service name
        5. Select number of tasks (1)
        6. Default for rest
    5. Verify Docker Image / Task is complete under Task definitions.
        1. Create new revision or run task
        2. Wait for docker image to download / setup
    6. View your service
        1. Under Services, check your service-name
        2. Choose Tasks.
        3. Confirm task is in Running state or completed with exit code 0 to signal task end.
![Alt text](Images/Exit.png?raw=true "Task Complete")
    7. Additional fun
        1. Configure CLI 
        2. Have fun (:
        
        
           
