package rep.api

import scala.reflect.runtime.{universe=>ru}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.`package`.Info
import rep.api.rest._
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition

/**集成Swagger到AKKA HTTP
 * @constructor 创建提供Swagger文档服务的实例
 * @param system 传入的AKKA系统实例 
 * 
 */
class SwaggerDocService(system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(
    ru.typeOf[SystemService], ru.typeOf[ChainService],
    ru.typeOf[BlockService],ru.typeOf[TransactionService],
    ru.typeOf[CertService], ru.typeOf[HashVerifyService])
//  override val host = "localhost:8081"
  override val host = "192.168.2.69:8081"
  override val info = Info(version = "0.5")
  override val externalDocs = Some(new ExternalDocs("Core Docs", "http://acme.com/docs"))
  override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
}