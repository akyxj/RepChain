package rep.app

import akka.remote.transport.Transport.InvalidAssociationException
import rep.app.system.ClusterSystem
import rep.app.system.ClusterSystem.InitType

/**
  * Repchain app start
  * Created by User on 2017/9/24.
  */
object Repchain_Single {
  def main(args: Array[ String ]): Unit = {
    var systemTag = "1"
    if(args!=null && args.length>0) systemTag = args(0)
    val sys1 = new ClusterSystem(systemTag, InitType.SINGLE_INIT,true)
    sys1.init
    val joinAddress = sys1.getClusterAddr
    sys1.joinCluster(joinAddress)
    sys1.start
  }
}
