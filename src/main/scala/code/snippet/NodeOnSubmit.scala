package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text

class NodeOnSubmit extends StatefulSnippet {
 
  
  def dispatch() = {case "render" => render
  case "displayNode" => displayNode}  
  
  private var node:Option[Node]=None
  private var nodeName = ""
  private var version = Int.MaxValue
  
  def process() = {
      println("======"+nodeName+" "+version)
      val _node = NodeHelper.getNode(nodeName,version)
      if (_node != null) {
        node = Some(_node)
      } else {
        node = None
      }  
    }

def render = {
    // define some variables to put our values into
    //var nodeName = ""
    //var version = Int.MaxValue
    
    // process the form
    
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
    node match {
      case Some(n:Node) => <span>TOTO {n.getProperty(NodeHelper.NAME_KEY)}</span>
      case _ => <span>None</span>
    }
  }
}