package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text

//class NodeOnSubmit extends StatefulSnippet {
 class NodeOnSubmit {
 
 private object node extends RequestVar[Option[Node]](None)
  
 // def dispatch() = {case "render" => render
 // case "displayNode" => displayNode}  
  
  //private var node:Option[Node]=None
 // private var nodeName = ""
 // private var version = Int.MaxValue
  
  def process2(nodeName:String, version:Int=Integer.MAX_VALUE) = {
      println("======"+nodeName+" "+version)
      NodeHelper.getNode(nodeName,version)
    }

def render = {
    // define some variables to put our values into
    var nodeName = ""
    var version = Int.MaxValue
    
    // process the form
     def process() = {
      println("======"+nodeName+" "+version)
      node.set(NodeHelper.getNode(nodeName,version))
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
      case Some(n:Node) => <span>TOTO {n.getProperty(NodeHelper.NAME_KEY)} - {n.getProperty(NodeHelper.VERSION_KEY)}</span>
      case _ => <span>None</span>
    }
  }
}