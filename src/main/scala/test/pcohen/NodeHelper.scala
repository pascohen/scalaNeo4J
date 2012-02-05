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

object NodeHelper {

  val DB_PATH = "/home/pcohen/utils/neo4j/latest/data/graph.db"
  val NAME_KEY = "nodename"
  val VERSION_KEY = "version"

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

  def addRelationship(from: Node, to: Node, relationType: RelationshipType): Relationship = {
    val tx = graphDb.beginTx
    try {
      val relationship = from.createRelationshipTo(to, relationType)
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
    val it = nodes.iterator()
    findByMaxVersion(it, maxVersion)
  }

  def traverseNode(name: String, relationType: RelationshipType, maxVersion: Int = Int.MaxValue): Iterator[Node] = {
    val node = getNode(name, maxVersion)
    traverseNode(node, relationType, maxVersion)
  }

  def traverseNode(node: Node, relationType: RelationshipType, maxVersion: Int): Iterator[Node] = {

    val returnableEvaluator = new ReturnableEvaluator {
      override def isReturnableNode(position: TraversalPosition): Boolean =
        {
          val nodeName = position.currentNode().getProperty(NAME_KEY)
          val version = position.currentNode().getProperty(VERSION_KEY).asInstanceOf[Int]

          if (position.notStartNode()) {
            val relations = position.previousNode().getRelationships(relationType, Direction.INCOMING)
            val nodes = relations.iterator().map[Node]({ r => r.getStartNode() })
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

  def getAvailableVersions(nodeName: String): List[Int] = {
    val nodes = nodeIndex.get(NAME_KEY, nodeName)
    val versions = nodes.iterator.map({ n => n.getProperty(VERSION_KEY).asInstanceOf[Int] })
    versions.toList.sort({ (a, b) => a < b })
  }

  def addProperty(node: Node, property: (String, Any)): Unit = {
    val tx = graphDb.beginTx
    try {
      node.setProperty(property._1, property._2)
      tx.success
    } finally {
      tx.finish
    }
  }

  def addProperty(node: Node, properties: Set[(String, Any)]): Unit = {
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

  def getProperty(node: Node, propertyKey: String, relationType: RelationshipType, maxVersion: Int = Int.MaxValue): Any = {
    val tree = traverseNode(node, relationType, maxVersion)
    val filteredTree = tree.filter(n => n.hasProperty(propertyKey))
    if (filteredTree.hasNext) {
      filteredTree.next.getProperty(propertyKey)
    } else {
      null
    }
  }

  def main(args: Array[String]): Unit = {
    registerShutdownHook

    /*
    val rootProps: Set[(String, Any)] = Set(("un", "one"), ("deux", "two"))
    val root = createRootNode(rootProps)

    val node1 = createNode(root, RelationTypes.CLASS_A, "sub1", 1)

    val node2Props: Set[(String, Any)] = Set(("trois", "drei"), ("quatre", "four"))

    val node2 = createNode(root, RelationTypes.CLASS_A, "sub1", 2, node2Props)

    val node3 = createNode(root, RelationTypes.CLASS_B, "sub2", 1)

    val leaf = createNode(node1, RelationTypes.CLASS_A, "leaf", 1)
    addRelationship(node2, leaf, RelationTypes.CLASS_A)
    addRelationship(node3, leaf, RelationTypes.CLASS_B)
*/
    println(getAvailableVersions("sub1"))
    val g = traverseNode("leaf", RelationTypes.CLASS_B, 3)
    if (g != null) {
      g foreach { n => println(n.getProperty(NAME_KEY) + " - " + n.getProperty(VERSION_KEY)) }
    }
    println("end")

   // val nn = getNode("sub2")
   // addProperty(nn,("trois","threee"))
    val n = getNode("leaf")
    println(getProperty(n, "trois", RelationTypes.CLASS_B, 1))
  }
}