package test.pcohen
import scala.math.Ordered
import scala.math.Ordering

object Tests extends App {
  true
 val l = Iterator((1,"2"),(1,"11"),(88,"2"),(5,"107"),(3,"6"))
 val m1 = l.maxBy({a => Integer.parseInt(a._2)})
 
 println(m1)

 val l2 = Iterator((1,"2"),(1,"11"),(88,"2"),(5,"107"),(3,"6"))

 val l3 = l2 filter( n => Integer.parseInt(n._2) < 90)
 val m2 = l3.maxBy[Int]({a => Integer.parseInt(a._2)})
 
 println(m2)
 
}