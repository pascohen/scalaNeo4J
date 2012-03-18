package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text
import test.pcohen.RelationTypes
import net.liftweb.common._
import S._
import net.liftweb.util._

import scala.xml._

class NodePropertyDetail {
 
 private object nodeProperty extends RequestVar[Option[Any]](None)
 
 private def getRelationTypesAsList:Seq[(RelationTypes.Value,String)] = {
   RelationTypes.values.map { it => (it,it.name())}.toList
 }
 
 def query(xhtml : NodeSeq) : NodeSeq = {
   
    var nodeName = ""
    var propertyName = ""
    var relationType:RelationTypes.Value = RelationTypes.CLASS_A
    val options = getRelationTypesAsList
    val relationTypes = SHtml.selectObj[RelationTypes.Value](options, Full(relationType), { it => relationType = it})
    var version = Int.MaxValue
    
     // process the form
     def process() = {
      val n = NodeHelper.getNode(nodeName,version)
      val p = NodeHelper.getProperty(n,propertyName,relationType,version)
      nodeProperty.set(p)
    }
    
    bind("entry", xhtml,
       "nodeName" -> SHtml.text(nodeName, nodeName = _),
         "propertyName" -> SHtml.text(propertyName, propertyName = _),
        "relationType" -> relationTypes,
       "version" -> SHtml.text({if (version != Int.MaxValue) {Integer.toString(version)} else { "" }}, { it => version = Integer.parseInt(it)}),
       "submit" -> SHtml.submit("Submit", process))
   
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