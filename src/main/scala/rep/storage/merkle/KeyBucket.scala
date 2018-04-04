package rep.storage.merkle

import rep.storage.leveldb.ILevelDB
import scala.collection.immutable
import scala.collection.mutable
import rep.storage.IdxPrefix
import rep.utils._
import rep.crypto._

class KeyBucket(val leveldb : ILevelDB,val CCID : String,val bucketNumber:Integer = 0) {
    private var LeafHashs : immutable.TreeMap[String,Array[Byte]] = new immutable.TreeMap[String,Array[Byte]]() 
    private var bucketMerkle : Array[Byte] = null
    
    load
    
    private def load={
      this.getBucketMerkleFromDB
      this.getBucketMerkleDataFromDB
    }
    
    
    def Put( key:String, value:Array[Byte]){
      synchronized{
        val prefix = IdxPrefix.WorldStateKeyPreFix+ CCID + "_"
        if(key.startsWith(prefix)){
          val key1 = key.substring(prefix.length(),key.length())
          val sv = Sha256.hash(value)
          this.LeafHashs += key1 -> sv
          computeMerkel
        }
      }
    }
    
    /*def getSize:Integer={
      synchronized{
        val size = this.LeafHashs.size
        size
      }
    }*/
    
    private def computeMerkel={
      synchronized{
          val source = this.LeafHashs.values.toArray
          if(source.size > 0){
             var value = source(0)
             var i = 1
             while(i < source.size){
               value = Array.concat(value , source(i))
               i += 1
             }
             this.bucketMerkle = Sha256.hash(value)
           }
       }
    }
    
    def getBucketMerkle:Array[Byte]={
      this.bucketMerkle
    }
    
    def getBucketMerkel4String:String={
       var rel:String = "" 
       val bb = getBucketMerkle
       if(bb != null){
         rel =  BytesHex.bytes2hex(bb)
       }
       
       rel
    }
    
    def save={
      val leafbytes = SerializeUtils.serialise(this.LeafHashs)
      val key4merkle = IdxPrefix.WorldStateForInternetPrefix + CCID + "_" +IdxPrefix.WorldStateMerkleForBucketNumber+"_"+String.valueOf(bucketNumber)
      val key4data = IdxPrefix.WorldStateForInternetPrefix + CCID + "_" +IdxPrefix.WorldStateDataForBucketNumber+"_"+String.valueOf(bucketNumber)
      if(this.bucketMerkle != null)this.leveldb.Put(key4merkle, this.bucketMerkle)
      this.leveldb.Put(key4data, leafbytes)
    }
    
    private def getBucketMerkleFromDB={
      val key = IdxPrefix.WorldStateForInternetPrefix + CCID + "_" +IdxPrefix.WorldStateMerkleForBucketNumber+"_"+String.valueOf(bucketNumber)
      val v = leveldb.Get(key)
      if(v != null){
        this.bucketMerkle = v
      }
    }
    
    private def getBucketMerkleDataFromDB={
      val key = IdxPrefix.WorldStateForInternetPrefix + CCID + "_" +IdxPrefix.WorldStateDataForBucketNumber+"_"+String.valueOf(bucketNumber)
      val v = leveldb.Get(key)
      if(v != null){
        var tmp = SerializeUtils.deserialise(v)
          if(tmp.isInstanceOf[immutable.TreeMap[String,Array[Byte]]] ){
            this.LeafHashs = tmp.asInstanceOf[immutable.TreeMap[String,Array[Byte]]]
          }
      }
    }
}