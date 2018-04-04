package rep
/**本包路径下的类用于处理对外部应用提供restful API以及Swagger-UI集成
 * 使用方式举例：
 *    val route_evt = (get & pathPrefix("web")) {
        getFromResourceDirectory("web")
      }
 *    val ra = sys.actorOf(Props[RestActor],"api")    
 *    Http().bindAndHandle(
 *        route_evt       
 *       ~ new TransactionService(ra).route,"0.0.0.0", port)
 * 
 * 
 */
package object api {
  
}