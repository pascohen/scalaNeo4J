import javax.servlet.http._
import org.neo4j.graphdb._
import org.neo4j.kernel._


import org.neo4j.graphdb.Traverser.Order

import scala.collection.JavaConversions._

class HelloWorldServlet extends HttpServlet {


  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = {

    resp.getWriter().print("Hello World")
  }
}