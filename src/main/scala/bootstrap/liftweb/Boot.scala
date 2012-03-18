package bootstrap.liftweb

import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import Helpers._
import net.liftweb.common.Full
 
/**
  * A class that’s instantiated early and run.  It allows the application
  * to modify Lift’s environment
  */
class Boot {
  
  def boot {
    // where to search snippet
    LiftRules.addToPackages("code")     
    
    
    val entries = List(
      Menu.i("Home") / "index", // the simple way to declare a menu
      Menu.i("NodeVersionsList") / "nodeVersionList",
      Menu.i("NodeDetail") / "nodeDetail",
      Menu.i("GetProperty") / "getProperty",
      Menu.i("Tree") / "tree"
     )

    // set the sitemap. Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)
      
    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
 //   LiftRules.htmlProperties.default.set((r: Req) =>
 //     new Html5Properties(r.userAgent))  

  }
}