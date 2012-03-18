package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text
import scala.collection.JavaConversions._

 class NodeDetail {
 
 private object node extends RequestVar[Option[Node]](None)
 
  def render = {
    // define some variables to put our values into
    var nodeName = ""
    var version = Int.MaxValue
    
    // process the form
     def process() = {
      node.set(NodeHelper.getNodeWithVersion(nodeName,version)) 
    }
    
    // associate each of the form elements
    // with a function... behavior to perform when the
    // for element is submitted
    "name=nodeName" #> SHtml.onSubmit(nodeName = _) & // set the name
    // set the age variable if we can convert to an Int
    "name=version" #> SHtml.onSubmit(s => asInt(s).foreach(version = _)) &
    // when the form is submitted, process the variable
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
  
 //def displayNode(ns:NodeSeq) = {
 def displayNode(a:NodeSeq) = {
    node.is match {
      case Some(n) => <span>
        {n.getProperty(NodeHelper.NAME_KEY)} - {n.getProperty(NodeHelper.VERSION_KEY)}
        </span>
        <ul>
        {(NodeSeq.Empty /: n.getPropertyKeys()) ((l,k) => l++ <li>Key {k} => {n.getProperty(k)}</li>)}
        </ul>

      case _ => <span></span>
    }
  }
}