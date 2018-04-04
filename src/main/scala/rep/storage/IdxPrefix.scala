package rep.storage

object IdxPrefix {
     val  Height:String   = "rechain_height";
	   val  MaxFileNo:String  = "rechain_file_no";
	   val  TotalAllTxNumber:String = "rechain_total_all_tx_number";
	    //每次存储时都需要修改这个内容
	   val  IdxBlockPrefix:String = "b_b_";//存储内容为blockindex完整值
	   val  IdxBlockHeight:String = "b_h_";//存储内容为block对应的hash值
	   val  IdxTransaction:String = "tx_";//存储内容为对应的block的hash值
	   val  WorldStateKeyPreFix:String = "c_";//存储内容为每个链码的worldstate
	   val  WorldStateLeafPreFix4Chain:String = "merkleleafhash4ch_"
	   val  WorldStateMerklePreFix4Chain:String = "merkletreehash4ch_"
	   val  WorldStateLeafPreFix:String = "merkleleafhash_"
	   val  WorldStateMerklePreFix:String = "merkletreehash_"
	   
	   val  WorldStateMerkleForCCID:String = "merkle4bucket"//某个链码的merkle值
	   val  WorldStateMerkleForBucketNumber:String = "merkle4bucketnumber"//某个链码的某个桶的merkle值
	   val  WorldStateDataForBucketNumber:String = "data4bucketnumber"//某个链码的某个桶的数据
	   val  WorldStateBucketCountForCCID:String = "bucketcountinccid"//某个链码桶的个数
	   val  ChainCodeList:String = "merklechaincodelist"//系统中所有链码的列表
	   val  WorldStateForInternetPrefix = "repchain_merkle_"//系统中有关链码key的前缀
	   
	   val GlobalWorldStateValue = "global_worldstate_value"//整个系统的worldstate值
	   
	   
	   ////////////////以下部分为日志前缀定义//////////////////////////////
	   val Log_LevelDBOp_Prefix:String = "Log_LevelDBOp_"
}