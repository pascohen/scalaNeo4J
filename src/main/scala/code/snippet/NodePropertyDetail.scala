package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text
import test.pcohen.RelationTypes

 class NodePropertyDetail {
 
 private object nodeProperty extends RequestVar[Option[Any]](None)
 
  def render = {
    // define some variables to put our values into
    var nodeName = ""
    var propertyName = ""
    var relationType = ""
    var version = Int.MaxValue
    
    // process the form
     def process() = {
      val n = NodeHelper.getNode(nodeName,version)
    val p = NodeHelper.getProperty(n,propertyName,RelationTypes.CLASS_A,version)
      println("N ==> "+n+" "+propertyName)
      
      val node2 = NodeHelper.getNode("leafV2")
      println(NodeHelper.getProperty(node2, "propA_1", RelationTypes.CLASS_A, 1))
      println("Prop ==> "+p)
      nodeProperty.set(p)
    }
    
    // associate each of the form elements
    // with a function... behavior to perform when the
    // for element is submitted
    "name=nodeName" #> SHtml.onSubmit(nodeName = _) & // set the name
     "name=propertyName" #> SHtml.onSubmit(propertyName = _) & // set the name
    // set the age variable if we can convert to an Int
    "name=version" #> SHtml.onSubmit(s => asInt(s).foreach(version = _)) &
    // when the form is submitted, process the variable
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
  
 //def displayNode(ns:NodeSeq) = {
 def displayDetail(a:NodeSeq) = {
    nodeProperty.is match {
      case Some(n) => <span>
        Property value is {n}
        </span>
      case _ => <span></span>
    }
  }
}