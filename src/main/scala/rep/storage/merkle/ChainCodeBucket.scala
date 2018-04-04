package rep.storage.merkle

import rep.storage.leveldb.ILevelDB
import scala.collection.immutable
import scala.collection.mutable
import rep.storage.IdxPrefix
import rep.utils._
import rep.crypto._

class ChainCodeBucket(val leveldb : ILevelDB,val CCID : String) {
    private var repbuckets :  mutable.ArrayBuffer[KeyBucket] = new mutable.ArrayBuffer[KeyBucket]()
    private var CCMerkle : Array[Byte] = null
    private val maxBucketNumber : Integer = 1000;  
    
    load
    
    private def load={
      getChainCodeMerkleFromDB
      for( i <- 0 to (this.maxBucketNumber -1)){
         this.repbuckets  += new KeyBucket(this.leveldb,this.CCID,i)
      }
    }
    
    def Put( key:String, value:Array[Byte]){
      synchronized{
        val bucketno = math.abs(key.hashCode()) % this.maxBucketNumber
        var rbucket = this.repbuckets(bucketno)
        if(rbucket != null){
          rbucket.Put(key, value)
          computeMerkel
        }
      }
    }
    
    private def computeMerkel={
      synchronized{
        var value : Array[Byte] = null
        for(i <- 0 to (this.maxBucketNumber-1)){
          val tmp = this.repbuckets(i)
          val tv = tmp.getBucketMerkle
          if(tv != null){
            if(value == null){
              value = tv
            }else{
              value = Array.concat(value , tv)
            }
          }
        }
        if(value != null)   this.CCMerkle = Sha256.hash(value)
      }
    }
    
    def getCCMerkle:Array[Byte]={
      this.CCMerkle
    }
    
    def getCCMerkel4String:String={
       var rel:String = "" 
       val bb = getCCMerkle
       if(bb != null){
         rel =  BytesHex.bytes2hex(bb)
       }
       
       rel
    }
    
    def save={
      val key = IdxPrefix.WorldStateForInternetPrefix + CCID + "_" +IdxPrefix.WorldStateMerkleForCCID
      this.leveldb.Put(key, this.CCMerkle)
      for(i <- 0 to this.maxBucketNumber-1){
          val tmp = this.repbuckets(i)
          tmp.save
      }
    }

    
    private def getChainCodeMerkleFromDB={
      val key = IdxPrefix.WorldStateForInternetPrefix + CCID + "_" +IdxPrefix.WorldStateMerkleForCCID
      val v = leveldb.Get(key)
      if(v != null){
        this.CCMerkle = v
      }
    }
    
}