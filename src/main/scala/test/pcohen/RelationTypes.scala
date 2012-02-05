package test.pcohen
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.RelationshipType
import org.neo4j.graphdb.RelationshipType

object RelationTypes  extends Enumeration {
  val CLASS_A = Value("ClassA")
  val CLASS_B = Value("ClassB")

  implicit def toRelationshipType(v: Value) = new RelationshipType { def name() = v.toString }
}

/*
class RelationTypes(val relationClass:String) extends RelationshipType {
  def name() = { toString() }
}*/