package rep.storage.test

import rep.storage._
import jnr.ffi.mapper.DataConverter

object test {
  
  def testop={
    val dataaccess :ImpDataAccess = ImpDataAccess.GetDataAccess("1")
    //dataaccess.BeginTrans
    dataaccess.Put("c_sdfs_a", "a".getBytes)
    dataaccess.Put("c_sdfs_a1", "b".getBytes)
    dataaccess.Put("c_sdfs_a2", "c".getBytes)
    dataaccess.Put("a3", "d".getBytes)
    //dataaccess.CommitTrans
    //dataaccess.RollbackTrans
    
    dataaccess.printlnHashMap(dataaccess.FindByLike("c"))
    println(dataaccess.GetComputeMerkle4String)
    
  }
  
  def testop1={
    val dataaccess :ImpDataAccess = ImpDataAccess.GetDataAccess("1")
    dataaccess.printlnHashMap(dataaccess.FindByLike("c"))
    println(dataaccess.GetComputeMerkle4String)
    
    /*dataaccess.Put("c_sdfs_a", "a".getBytes)
    dataaccess.Put("c_sdfs_a1", "b".getBytes)
    dataaccess.Put("c_sdfs_a2", "c".getBytes)
    dataaccess.Put("a3", "d".getBytes)*/
    
    
    
    val preload :ImpDataPreload = ImpDataPreloadMgr.GetImpDataPreload("1","lllll")
    preload.Put("c_sdfs_a", "1".getBytes)
    preload.Put("c_sdfs_a1", "2".getBytes)
    preload.Put("c_sdfs_a2", "3".getBytes)
    println(preload.GetComputeMerkle4String)
    println(dataaccess.GetComputeMerkle4String)
    
    val preload1 :ImpDataPreload = ImpDataPreloadMgr.GetImpDataPreload("1","lllll")
    preload1.Put("c_sdfs_a", "1".getBytes)
    preload1.Put("c_sdfs_a1", "2".getBytes)
    preload1.Put("c_sdfs_a2", "3".getBytes)
    println(preload1.GetComputeMerkle4String)
    println(dataaccess.GetComputeMerkle4String)
    
    
    ImpDataPreloadMgr.Free("1","lllll")
  }
  
  def main(args: Array[String]): Unit = {
    testop1
  }
}