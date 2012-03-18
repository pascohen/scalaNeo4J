package test.pcohen
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Traverser.Order
import org.neo4j.graphdb.StopEvaluator
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.Direction
import scala.collection.JavaConversions._
import org.neo4j.graphdb.TraversalPosition
import scala.math.Ordered
import org.neo4j.graphdb.PropertyContainer

object NodeHelper {

  val DB_PATH = "/home/pcohen/utils/neo4j/latest/data/graph.db"
  val NAME_KEY = "nodename"
  val VERSION_KEY = "version"
  val RELATIONSHIP_KEY = "relationType"
  val DEFAULT_RELATIONSHIP_TYPE_VALUE = RelationshipClass.DEFAULT_CLASS

  private val graphDb: GraphDatabaseService = new EmbeddedGraphDatabase(DB_PATH)
  private val nodeIndex = graphDb.index().forNodes("nodes")

  def registerShutdownHook =
    Runtime.getRuntime.addShutdownHook(new Thread() { override def run = shutdown })

  private def shutdown = {
    graphDb.shutdown
  }

  /**
   * 
   * CRUD Part
   * 
   */
  def createRootNode(rootName: String = "root", properties: Set[(String, Any)] = Set()): Node = {
    val rootNode = graphDb.createNode

    rootNode.setProperty(NAME_KEY, rootName)
    rootNode.setProperty(VERSION_KEY, 1)

    properties foreach { tuple =>
      rootNode.setProperty(tuple._1, tuple._2)  
    } 
    nodeIndex.add(rootNode, NAME_KEY, rootName)
    rootNode
  }

  def createNode(nodeName: String, version: Int, nodeProperties: Set[(String, Any)] = Set(),
      parent: Node, 
      relationType: RelationshipType,
      relationProperties: Set[(String, Any)] = Set((RELATIONSHIP_KEY,DEFAULT_RELATIONSHIP_TYPE_VALUE.toString()))
      ): Node = {   
    val node = graphDb.createNode
   
    node.setProperty(NAME_KEY, nodeName)   
    node.setProperty(VERSION_KEY, version)
   
    nodeProperties foreach { tuple =>      
      node.setProperty(tuple._1, tuple._2)     
    }
   
    addRelationship(parent, node, relationType, relationProperties)    
    nodeIndex.add(node, NAME_KEY, nodeName)
    node
  }

  def addRelationship(from: Node, to: Node, relationType: RelationshipType, 
      properties: Set[(String, Any)]=Set((RELATIONSHIP_KEY,DEFAULT_RELATIONSHIP_TYPE_VALUE.toString()))): Relationship = {
    val relationship = from.createRelationshipTo(to, relationType)     
    properties foreach { p => relationship.setProperty(p._1, p._2) } 
    relationship
  }
    
  def addProperty(node: PropertyContainer, property: (String, Any)) = {  
    node.setProperty(property._1, property._2)
  }
  
   def addProperties(node: PropertyContainer, properties: (String, Any)*): Unit = {
    properties foreach { property =>
      addProperty(node, property)
    }
  }

  def addProperties(node: PropertyContainer, properties: Set[(String, Any)]): Unit = {
    for (p <- properties) {
      addProperty(node, p) 
    }
  }
  
  def setRelationshipClass(r:Relationship, cl: RelationshipClass.Value) = {
    r.setProperty(RELATIONSHIP_KEY, cl.toString())
  }
  
  
  /**
   * Access
   */

  private def findByMaxVersion(iterator: Iterator[Node], maxVersion: Int = Int.MaxValue): Option[Node] = {
    iterator.isEmpty match {
      case true => None
      case _ =>
        val filteredIterator = iterator.filter { n => n.getProperty(VERSION_KEY).asInstanceOf[Int] <= maxVersion }
        filteredIterator.isEmpty match {
          case true => None
          case _ => Some(filteredIterator.maxBy[Int] { n => n.getProperty(VERSION_KEY).asInstanceOf[Int] })
        }
    }
  }

  def getNode(name: String, maxVersion: Int = Int.MaxValue): Option[Node] = {
    val nodes = nodeIndex.get(NAME_KEY, name)
    val it = nodes.iterator()
    findByMaxVersion(it, maxVersion)
  }
  
  def getNodeWithVersion(name: String, version: Int): Option[Node] = {
    val nodes = nodeIndex.get(NAME_KEY, name)
    val it = nodes.iterator()
    val node = it.filter({n => n.getProperty(VERSION_KEY).asInstanceOf[Int] == version})
    if (node.hasNext) {
      Some(node.next())
    } else {
      None
    }
  }

  def traverseNode(name: String, relationType: RelationshipType, maxVersion: Int = Int.MaxValue): Iterator[Node] = {
    val node = getNode(name, maxVersion)
    traverseNode(node, relationType, maxVersion)
  }
  

  private def traverseNodeWithRelationshipFilter(node: Option[Node], relationType: RelationshipType, maxVersion: Int, relationshipFilter: Relationship => Boolean = { r => true }): Iterator[Node] = {

    val returnableEvaluator = new ReturnableEvaluator {
      override def isReturnableNode(position: TraversalPosition): Boolean =
        {
          val nodeName = position.currentNode().getProperty(NAME_KEY)
          val version = position.currentNode().getProperty(VERSION_KEY).asInstanceOf[Int]

          if (position.notStartNode()) {
            val relations = position.previousNode().getRelationships(relationType, Direction.INCOMING)
            val iterator = relations.iterator().filter(relationshipFilter)
            val nodes = iterator.map[Node]({ r => r.getStartNode() })
            val siblings = nodes filter ({ n => n.getProperty(NAME_KEY) == nodeName })
            val n = findByMaxVersion(siblings, maxVersion)
            n match {
              case Some(maxNode) => maxNode == position.currentNode()
              case _ => findByMaxVersion(siblings) == position.currentNode()
            } 
          } else {
            true
          }
        }
    }
    
    node match {
      case Some(n) => val trav = n.traverse(Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, returnableEvaluator,
      relationType, Direction.INCOMING)
      trav.iterator()
      case _ => Iterator()
    }  
  }

  def traverseNode(node: Option[Node], relationType: RelationshipType, maxVersion: Int): Iterator[Node] = {
    traverseNodeWithRelationshipFilter(node, relationType, maxVersion)
  }

  def getAvailableVersions(nodeName: String): List[Int] = {
    val nodes = nodeIndex.get(NAME_KEY, nodeName)
    val versions = nodes.iterator.map { n => n.getProperty(VERSION_KEY).asInstanceOf[Int] }
    versions.toList.sort { (a, b) => a < b }
  }

 
  def getProperty(node: Option[Node], propertyKey: String, relationType: RelationshipType, maxVersion: Int = Int.MaxValue): Option[Any] = {
    val tree = traverseNode(node, relationType, maxVersion)
    val filteredTree = tree.filter(n => n.hasProperty(propertyKey))
    if (filteredTree.hasNext) {
      Some(filteredTree.next.getProperty(propertyKey))
    } else {
      None
    }
  }
  
  /**
   * Experimental
   */
  
   def traverseNode(node: Option[Node], relationType: RelationshipType, relationProperty: String, maxVersion: Int): Iterator[Node] = {
    traverseNodeWithRelationshipFilter(node, relationType, maxVersion, { r: Relationship => r.getProperty(RELATIONSHIP_KEY)==relationProperty })
  }

  def traverseNodeWithRelationProperty(name: String, relationType: RelationshipType, relationProperty: String, maxVersion: Int = Int.MaxValue): Iterator[Node] = {
    val node = getNode(name, maxVersion) 
    traverseNode(node, relationType, relationProperty,maxVersion) 
  }
  
  /**
   * 
   * Fill and test
   * 
   */

  def fillDB1 = {
    val tx = graphDb.beginTx

    try {

    val rootProps: Set[(String, Any)] = Set(("propA_1", "xxx-0"), ("propB_1", "yyy-0"),("propB_3","vvv-0"))
    
    val root = createRootNode(properties=rootProps)

    val nodeA1Props: Set[(String, Any)] = Set(("propA_1","xxx-1-1"))   
    val nodeA1 = createNode("classA:child1", 1,nodeA1Props,root, RelationTypes.CLASS_A)

    val nodeA2Props: Set[(String, Any)] = Set(("propA_1","xxx-1-2"))
    val nodeA2 = createNode( "classA:child1", 2, nodeA2Props,root, RelationTypes.CLASS_A)
    
    val nodeA3Props: Set[(String, Any)] = Set(("propA_1", "xxx-2-1"), ("propA_2", "zzz-2-1"))   
    val nodeA3 = createNode("classA:child2", 1, nodeA3Props,nodeA1, RelationTypes.CLASS_A)

    addRelationship(nodeA2, nodeA3, RelationTypes.CLASS_A)
    
    
    val nodeB1Props: Set[(String, Any)] = Set(("propB_1","yyy-1-1"))   
    val nodeB1 = createNode("classB:child1", 1,nodeB1Props,root, RelationTypes.CLASS_B)
  
    val nodeB2Props: Set[(String, Any)] = Set(("propB_1","yyy-2-1"))   
    val nodeB2 = createNode("classB:child2", 1,nodeB2Props,nodeB1, RelationTypes.CLASS_B)
   
    val nodeB3Props: Set[(String, Any)] = Set(("propB_2","www-2-2"))   
    val nodeB3 = createNode("classB:child2", 2,nodeB3Props,nodeB1, RelationTypes.CLASS_B)
   
    
    val nodeB4Props: Set[(String, Any)] = Set(("propB_2","www-3-1"))   
    val nodeB4 = createNode("classB:child3", 1,nodeB4Props,nodeB2, RelationTypes.CLASS_B)
   
    addRelationship(nodeB3, nodeB4, RelationTypes.CLASS_B)
       
    val leaf = createNode(nodeName="leaf", version=1,parent=nodeA3, relationType=RelationTypes.CLASS_A)
    addRelationship(nodeB4,leaf, RelationTypes.CLASS_B)

    tx.success
    } finally {
      tx.finish
    }
  }
  
   def fillDB2 = {
    val tx = graphDb.beginTx

    try {

    val rootProps: Set[(String, Any)] = Set(("propA_1", "xxx-0"), ("propB_1", "yyy-0"),("propB_3","vvv-0"))
    
    val root = createRootNode("newRoot", rootProps)

    val nodeA1Props: Set[(String, Any)] = Set(("propA_1","xxx-1-1"))   
    val nodeA1 = createNode("classAV2:child1", 1,nodeA1Props,root, RelationTypes.CLASS_A)

    val nodeA2Props: Set[(String, Any)] = Set(("propA_1","xxx-1-2"))
    val nodeA2 = createNode( "classAV2:child1", 2, nodeA2Props,root, RelationTypes.CLASS_A)
    
    val nodeA3Props: Set[(String, Any)] = Set(("propA_1", "xxx-2-1"), ("propA_2", "zzz-2-1"))   
    val nodeA3 = createNode("classAV2:child2", 1, nodeA3Props,nodeA1, RelationTypes.CLASS_A)
    
    addRelationship(nodeA2, nodeA3, RelationTypes.CLASS_A)



    addRelationship(nodeA1, root, RelationTypes.CLASS_B)
    addRelationship(nodeA2, nodeA1, RelationTypes.CLASS_B)

    
    val nodeB1Props: Set[(String, Any)] = Set(("propB_1","yyy-1-1"))   
    val nodeB1 = createNode("classBV2:child1", 1,nodeB1Props,nodeA2, RelationTypes.CLASS_B)
  
    val nodeB2Props: Set[(String, Any)] = Set(("propB_1","yyy-2-1"))   
    val nodeB2 = createNode("classBV2:child2", 1,nodeB2Props,nodeB1, RelationTypes.CLASS_B)
   
    val nodeB3Props: Set[(String, Any)] = Set(("propB_2","www-2-2"))   
    val nodeB3 = createNode("classBV2:child2", 2,nodeB3Props,nodeB1, RelationTypes.CLASS_B)
   
    val nodeB4Props: Set[(String, Any)] = Set(("propB_2","www-3-1"))   
    val nodeB4 = createNode("classBV2:child3", 1,nodeB4Props,nodeB2, RelationTypes.CLASS_B)
   
    addRelationship(nodeB3, nodeB4, RelationTypes.CLASS_B)
       
    val leaf = createNode(nodeName="leafV2", version=1,parent=nodeA3, relationType=RelationTypes.CLASS_A)
    addRelationship(nodeB4,leaf, RelationTypes.CLASS_B)

    tx.success
    } finally {
      tx.finish
    }
  }
  
  def main(args: Array[String]): Unit = {
    registerShutdownHook
   //fillDB1
   //fillDB2
    println("Querying for DB1")
    println(getAvailableVersions("leaf"))
    val g = traverseNode("leaf", RelationTypes.CLASS_B, 1)
    if (g != null) {
      g foreach { n => 
        println(n.getProperty(NAME_KEY) + " - " + n.getProperty(VERSION_KEY))
       // n.getPropertyKeys() foreach { k => println(k+" => "+n.getProperty(k))}
        }
    }
    println("end")

    val node = getNode("leaf")
    node match {
      case Some(_) => println(getProperty(node, "propB_1", RelationTypes.CLASS_B, 1))
      case _ =>
    }
    
    println("Querying for DB2")

    println(getAvailableVersions("leafV2"))
    val g2 = traverseNode("leafV2", RelationTypes.CLASS_B, 2)
    if (g2 != null) {
      g2 foreach { n => 
        println(n.getProperty(NAME_KEY) + " - " + n.getProperty(VERSION_KEY))
       // n.getPropertyKeys() foreach { k => println(k+" => "+n.getProperty(k))}
        }
    }
    println("end")

    val node2 = getNode("leafV2")
    node2 match {
      case Some(_) => println(getProperty(node2, "propA_1", RelationTypes.CLASS_A, 1))
      case _ =>
    }

  }
}