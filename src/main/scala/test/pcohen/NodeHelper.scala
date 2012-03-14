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

  def createRootNode(properties: Set[(String, Any)]): Node = {
    val tx = graphDb.beginTx

    try {
      val rootNode = graphDb.createNode

      rootNode.setProperty(NAME_KEY, "Root")
      rootNode.setProperty(VERSION_KEY, 1)

      properties foreach { tuple =>
        rootNode.setProperty(tuple._1, tuple._2)
      }

      nodeIndex.add(rootNode, NAME_KEY, "Root")

      tx.success
      rootNode
    } finally {
      tx.finish
    }
  }

  def createNode(parent: Node, relationType: RelationshipType, nodeName: String, version: Int, properties: Set[(String, Any)] = Set()): Node = {
    val tx = graphDb.beginTx

    try {
      val node = graphDb.createNode

      node.setProperty(NAME_KEY, nodeName)
      node.setProperty(VERSION_KEY, version)

      properties foreach { tuple =>
        node.setProperty(tuple._1, tuple._2)
      }

      addRelationship(parent, node, relationType)
      nodeIndex.add(node, NAME_KEY, nodeName)

      tx.success
      node

    } finally {
      tx.finish
    }
  }

  def addRelationship(from: Node, to: Node, relationType: RelationshipType, properties: (String, Any)*): Relationship = {
    val tx = graphDb.beginTx
    try {
      val relationship = from.createRelationshipTo(to, relationType)
      relationship.setProperty(RELATIONSHIP_KEY,DEFAULT_RELATIONSHIP_TYPE_VALUE.toString())

      properties foreach { p => relationship.setProperty(p._1, p._2) }
      tx.success
      relationship
    } finally {
      tx.finish
    }
  }

  private def findByMaxVersion(iterator: Iterator[Node], maxVersion: Int = Int.MaxValue): Node = {
    iterator.isEmpty match {
      case true => null
      case _ =>
        val filteredIterator = iterator.filter { n => n.getProperty(VERSION_KEY).asInstanceOf[Int] <= maxVersion }
        filteredIterator.isEmpty match {
          case true => null
          case _ => filteredIterator.maxBy[Int] { n => n.getProperty(VERSION_KEY).asInstanceOf[Int] }

        }
    }
  }

  def getNode(name: String, maxVersion: Int = Int.MaxValue): Node = {
    val nodes = nodeIndex.get(NAME_KEY, name)
import org.neo4j.graphdb.RelationshipType
    val it = nodes.iterator()
    findByMaxVersion(it, maxVersion)
  }

  def traverseNode(name: String, relationType: RelationshipType, maxVersion: Int = Int.MaxValue): Iterator[Node] = {
    val node = getNode(name, maxVersion)
    traverseNode(node, relationType, maxVersion)
  }
  
  def traverseNodeWithRelationProperty(name: String, relationType: RelationshipType, relationProperty: String, maxVersion: Int = Int.MaxValue): Iterator[Node] = {
    val node = getNode(name, maxVersion)
    traverseNode(node, relationType, relationProperty,maxVersion)
  }

  private def traverseNodeWithRelationshipFilter(node: Node, relationType: RelationshipType, maxVersion: Int, relationshipFilter: Relationship => Boolean = { r => true }): Iterator[Node] = {
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
            val node = findByMaxVersion(siblings, maxVersion)
            if (node == null) {
              findByMaxVersion(siblings) == position.currentNode()
            } else {
              node == position.currentNode()
            }
          } else {
            true
          }
        }
    }
    if (node == null) return null

    val trav = node.traverse(Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, returnableEvaluator,
      relationType, Direction.INCOMING)
    trav.iterator()
  }

  def traverseNode(node: Node, relationType: RelationshipType, maxVersion: Int): Iterator[Node] = {
    traverseNodeWithRelationshipFilter(node, relationType, maxVersion)
  }

  def traverseNode(node: Node, relationType: RelationshipType, relationProperty: String, maxVersion: Int): Iterator[Node] = {
    traverseNodeWithRelationshipFilter(node, relationType, maxVersion, { r: Relationship => r.getProperty(RELATIONSHIP_KEY)==relationProperty })
  }

  def getAvailableVersions(nodeName: String): List[Int] = {
    val nodes = nodeIndex.get(NAME_KEY, nodeName)
    val versions = nodes.iterator.map({ n => n.getProperty(VERSION_KEY).asInstanceOf[Int] })
    versions.toList.sort({ (a, b) => a < b })
  }

  def addProperty(node: PropertyContainer, property: (String, Any)) = {
    val tx = graphDb.beginTx
    try {
      node.setProperty(property._1, property._2)
      tx.success
    } finally {
      tx.finish
    }

  }

  def addProperties(node: PropertyContainer, properties: (String, Any)*): Unit = {
    val tx = graphDb.beginTx
    try {
      properties foreach { property =>
        addProperty(node, property)
      }
      tx.success
    } finally {
      tx.finish
    }
  }

  def addProperties(node: PropertyContainer, properties: Set[(String, Any)]): Unit = {
    val tx = graphDb.beginTx
    try {
      for (p <- properties) {
        addProperty(node, p)
      }
      tx.success
    } finally {
      tx.finish
    }
  }
  
  def setRelationshipClass(r:Relationship, cl: RelationshipClass.Value) = {
    r.setProperty(RELATIONSHIP_KEY, cl.toString())
  }

  def getProperty(node: Node, propertyKey: String, relationType: RelationshipType, maxVersion: Int = Int.MaxValue): Any = {
    val tree = traverseNode(node, relationType, maxVersion)
    val filteredTree = tree.filter(n => n.hasProperty(propertyKey))
    if (filteredTree.hasNext) {
      filteredTree.next.getProperty(propertyKey)
    } else {
      null
    }
  }

  def fillDB = {
    val rootProps: Set[(String, Any)] = Set(("propA_1", "xxx-0"), ("propB_1", "yyy-0"),("propB_3","vvv-0"))
    
    val root = createRootNode(rootProps)

    val nodeA1Props: Set[(String, Any)] = Set(("propA_1","xxx-1-1"))   
    val nodeA1 = createNode(root, RelationTypes.CLASS_A, "classA:child1", 1,nodeA1Props)

    val nodeA2Props: Set[(String, Any)] = Set(("propA_1","xxx-1-2"))
    val nodeA2 = createNode(root, RelationTypes.CLASS_A, "classA:child1", 2, nodeA2Props)
    
    val nodeA3Props: Set[(String, Any)] = Set(("propA_1", "xxx-2-1"), ("propA_2", "zzz-2-1"))   
    val nodeA3 = createNode(nodeA1, RelationTypes.CLASS_A, "classA:child2", 1, nodeA3Props)
    
    addRelationship(nodeA2, nodeA3, RelationTypes.CLASS_A)
    
    
    val nodeB1Props: Set[(String, Any)] = Set(("propB_1","yyy-1-1"))   
    val nodeB1 = createNode(root, RelationTypes.CLASS_B, "classB:child1", 1,nodeB1Props)
  
    val nodeB2Props: Set[(String, Any)] = Set(("propB_1","yyy-2-1"))   
    val nodeB2 = createNode(nodeB1, RelationTypes.CLASS_B, "classB:child2", 1,nodeB2Props)
   
    val nodeB3Props: Set[(String, Any)] = Set(("propB_2","www-2-2"))   
    val nodeB3 = createNode(nodeB1, RelationTypes.CLASS_B, "classB:child2", 2,nodeB3Props)
   
    
    val nodeB4Props: Set[(String, Any)] = Set(("propB_2","www-3-1"))   
    val nodeB4 = createNode(nodeB2, RelationTypes.CLASS_B, "classB:child3", 1,nodeB4Props)
   
    addRelationship(nodeB3, nodeB4, RelationTypes.CLASS_B)
   
    
    val leaf = createNode(nodeA3, RelationTypes.CLASS_A, "leaf", 1)
    addRelationship(nodeB4,leaf, RelationTypes.CLASS_B)

    //addRelationship(node4, leaf, RelationTypes.CLASS_A,(RELATIONSHIP_KEY,RelationshipClass.TEST_CLASS.toString()))
    
  }
  
  def main(args: Array[String]): Unit = {
    registerShutdownHook
    //fillDB
    println(getAvailableVersions("sub1"))
    val g = traverseNodeWithRelationProperty("leaf", RelationTypes.CLASS_B, "Default", 2)
    if (g != null) {
      g foreach { n => 
        println(n.getProperty(NAME_KEY) + " - " + n.getProperty(VERSION_KEY))
        n.getPropertyKeys() foreach { k => println(k+" => "+n.getProperty(k))}
        }
    }
    println("end")

    val n = getNode("leaf")
    println(getProperty(n, "propB_1", RelationTypes.CLASS_B, 2))
  }
}