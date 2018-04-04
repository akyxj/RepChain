package rep.storage.block;

import java.util.concurrent.ConcurrentHashMap;


public class BlockInstances {
	private static BlockInstances instance = null;
	
	private ConcurrentHashMap<String,BlockHelp> instances = null;
	
	private BlockInstances(){
		this.instances = new ConcurrentHashMap<String,BlockHelp>();
	}
	
	public static synchronized BlockInstances getDBInstance(){
		if(instance == null){
			instance = new BlockInstances();
		}
		return instance;
	}
	
	public BlockHelp getBlockHelp(String SystemName){
		BlockHelp bhelp = null;
		bhelp = this.instances.get(SystemName);
		if(bhelp == null){
			bhelp = CreateBlockHelp(SystemName);
		}
		return bhelp;
	}
	
	private synchronized BlockHelp CreateBlockHelp(String SystemName){
		BlockHelp lhelp = null;
		try{
			lhelp = new BlockHelp(SystemName);
			this.instances.put(SystemName, lhelp);
		}catch(Exception e){
			e.printStackTrace();
		}
		return lhelp;
	}
	
	public static void main(String[] args){
		try{
			BlockHelp bh = BlockInstances.getDBInstance().getBlockHelp("testSystem");
			String aa = "sdfsdfkjklsdfklsdflsflsdkflsdflsdfkljdflsdkf";
			int aalen = aa.getBytes().length;
			bh.writeBlock(0, 0, aa.getBytes());
			byte[] raa = bh.readBlock(0, 0, aalen);
			System.out.println(new String(raa));
			String bb = "kaaaakkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkka";
			int bblen = bb.getBytes().length;
			int start = aalen;
			bh.writeBlock(0, start, bb.getBytes());
			byte[] rbb = bh.readBlock(0, start, bblen);
			System.out.println(new String(rbb));
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
}