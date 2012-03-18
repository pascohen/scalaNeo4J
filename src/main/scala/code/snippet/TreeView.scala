package code.snippet

import net.liftweb._
import http._
import util.Helpers._
import scala.xml.NodeSeq
import test.pcohen.NodeHelper
import org.neo4j.graphdb.Node
import scala.xml.Text
import test.pcohen.RelationTypes
import scala.collection.JavaConversions._
import S._
import net.liftweb.util._
import net.liftweb.common._
import scala.xml._


class TreeView {
 
 private object nodeName extends RequestVar[String]("")
 private object version extends RequestVar[Int](0)
 private object relationType extends RequestVar[RelationTypes.Value](null)
  
 private def getRelationTypesAsList:Seq[(RelationTypes.Value,String)] = {
   RelationTypes.values.map { it => (it,it.name())}.toList
 }
 
 def query(xhtml : NodeSeq) : NodeSeq = {  
    val options = getRelationTypesAsList
    val relationTypes = SHtml.selectObj[RelationTypes.Value](options, Full(relationType), { it => relationType.set(it)})
     
    bind("entry", xhtml,
       "nodeName" -> SHtml.text(nodeName, nodeName.set(_)),
       "relationType" -> relationTypes,
       "version" -> SHtml.text({if (version.is != Int.MaxValue) {Integer.toString(version.is)} else { "" }}, { it => version.set(Integer.parseInt(it))}),
       "submit" -> SHtml.submit("Submit", {() => }))
  }
   
   
 def displayHierarchy(a:NodeSeq) = {
   (nodeName.is,version.is) match {
     case (s,i) if s != "" && i > 0 =>  
       <div><span>Hierarchy for {s} at version {i}</span>
       {val n = NodeHelper.getNode(s,i)
       val l = NodeHelper.traverseNode(n,relationType.is,i)
       <ul>
       	{(NodeSeq.Empty /: l)( (n,it) => n ++ <li>{it.getProperty(NodeHelper.NAME_KEY)} - {it.getProperty(NodeHelper.VERSION_KEY)} 
       	<ul>
       	{(NodeSeq.Empty /: it.getPropertyKeys().filter(k => k!=NodeHelper.NAME_KEY && k!=NodeHelper.VERSION_KEY))( (n2,it2) => n2 ++ <li>{it2} - {it.getProperty(it2)}</li>)}        		
        </ul>  
       	</li>)}
       </ul>}
       </div>
   	 case _ => <div></div> 
   	 
   }
  }
}