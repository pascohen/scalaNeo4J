package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text

class NodeVersionList {
 
 private object nodeName extends RequestVar[String]("")
 private object versionList extends RequestVar[List[Int]](List())
  
 def render = {
    // define some variables to put our values into
    var nName = ""
    
    // process the form
     def process() = {
      versionList.set(NodeHelper.getAvailableVersions(nName))
      nodeName.set(nName)
    }
    
    // associate each of the form elements
    // with a function... behavior to perform when the
    // for element is submitted
    "name=nodeName" #> SHtml.onSubmit(nName = _) & // set the name
    // when the form is submitted, process the variable
    "type=submit" #> SHtml.onSubmitUnit(process)
  }
   
   
 def displayVersionList(a:NodeSeq) = {
   nodeName.is match {
     case "" => <span></span> 
   	 case s =>  <span>List of available versions for {s}</span>
   	 <ul>
   	 {(NodeSeq.Empty /: versionList)((l, r) => l ++ (<li>Version = {r}</li>))}
   	 </ul>
   }
  }
}
