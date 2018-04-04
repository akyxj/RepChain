package rep.ui.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.{Flow, Source}
import akka.actor._
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.javadsl.model.ws._
import akka.stream.scaladsl.Sink

import scala.concurrent.forkjoin.ThreadLocalRandom
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import StatusCodes._
import Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

import akka.stream.actor._
import rep.protos.peer._
import akka.util.ByteString
import rep.log.EventActor
import rep.api.SwaggerDocService
import rep.api.rest._
import rep.utils.GlobalUtils
import rep.sc.Sandbox.SandboxException

object EventServer {

implicit def myExceptionHandler = ExceptionHandler {
  case e: SandboxException =>
    extractUri { uri =>
      complete(HttpResponse(Accepted, 
          //entity = s"""{"SandboxException":"${e.getMessage}"}""")
          entity = HttpEntity(ContentTypes.`application/json`,
              s"""{"err": "${e.getMessage}"}"""))
      )
      //complete(HttpResponse(InternalServerError, entity = s"SandboxException:${e.getMessage}"))
    }
}  
  //传入publish Actor
  //必须确保ActorSystem 与其他actor处于同一system，context.actorSelection方可正常工作
  def start(sys:ActorSystem ,port:Int) {
    implicit val _ = sys.dispatcher
    implicit val system =sys
    implicit val materializer = ActorMaterializer()
    val route_evt =
     (get & pathPrefix("swagger")) {
        getFromResourceDirectory("swagger")
      }~
     (get & pathPrefix("web")) {
        getFromResourceDirectory("web")
      }~
      path("event") {
        get {
          //must ref to the same actor
         val source = Source.actorPublisher[Event](Props[EventActor]).map(evt =>  BinaryMessage(ByteString(evt.toByteArray)))   
          extractUpgradeToWebSocket { upgrade =>
            complete(upgrade.handleMessagesWithSinkSource(Sink.ignore, source))
          }
        }
      }

    //for swagger ui
    //val ar = sys.actorSelection(GlobalUtils.peerApiPath)
    val ra = sys.actorOf(Props[RestActor],"api")
    
    Http().bindAndHandle(
        route_evt       
        ~ cors() {new SystemService(ra).route }
        ~ cors() {new BlockService(ra).route }
        ~ cors() {new ChainService(ra).route }
        ~ cors() {new TransactionService(ra).route }
        ~ cors() {new CertService(ra).route }
        ~ cors() {new HashVerifyService(ra).route }
        ~ new SwaggerDocService(sys).routes
        
        ,"0.0.0.0", port)
    println(s"Event Server online at http://localhost:$port")
  }
  
}


class EventServer extends Actor{
  override def preStart(): Unit =EventServer.start(context.system, 8081)

  def receive = {
    case Event =>
  }
}