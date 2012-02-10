import javax.servlet.http._
import org.neo4j.graphdb._
import org.neo4j.kernel._


import org.neo4j.graphdb.Traverser.Order

import scala.collection.JavaConversions._

class HelloWorld extends HttpServlet {

 // implicit def conv(rt: RelTypes) = new RelationshipType() { def name = rt.toString }
  
  private val DB_PATH = "/home/pcohen/utils/neo4j/latest/data/graph.db"
    
  private val NAME_KEY = "nodename"

  private val graphDb: GraphDatabaseService = new EmbeddedGraphDatabase(DB_PATH)
   private val nodeIndex = graphDb.index().forNodes("nodes")


  private def registerShutdownHook =
    Runtime.getRuntime.addShutdownHook(new Thread() { override def run = shutdown })

  private def shutdown = {
    graphDb.shutdown
  }
  
   private def doTx(f: GraphDatabaseService => Unit) = {
    val tx = graphDb.beginTx
    try {
      f(graphDb)
      tx.success
    } finally {
      tx.finish
    }
  }

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = {

    registerShutdownHook
  doTx { 
    db =>
    //val node = graphDb.getNodeById(6l)
      val node = nodeIndex.get(NAME_KEY,"Leaf").getSingle()
    println("Fini Read " + node.getPropertyKeys())
    println("Leaf id is " + node.getId)
    println("Leaf id is " + node.getRelationships())
    
    //  val trav = node.traverse(Order.DEPTH_FIRST,StopEvaluator.END_OF_GRAPH,ReturnableEvaluator.ALL,
     //     conv(CLASS_B),Direction.INCOMING)
          
    //  println("trav "+trav)
    //  val it = trav.iterator()
    //  while(it.hasNext()) {
   //    println(it.next().getProperty(NAME_KEY)) 
   //   }
      
      println("FFF")
      
   //   it foreach { l => println("-->"+l)}

    resp.getWriter().print("Hello World2! "+node.getId)
  }
  }
}