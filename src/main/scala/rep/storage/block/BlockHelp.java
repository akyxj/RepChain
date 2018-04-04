package rep.storage.block;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import rep.storage.cfg.StoreConfig;
import rep.storage.util.pathUtil;

/**
 * @author jiangbuyun
 * @version	1.0
 * @since	2017-09-28
 * @category	该类主要对File System进行操作。
 * */
public class BlockHelp {
	private static String FileName = "Repchain_BlockFile_";
	private String SystemName = "";
	private String BlockDataPath = "";
	private Object synchObject = new Object();
	private long filemaxlength = 200*1024*2014;
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	构造函数
	 * @param	String SystemName 系统名称
	 * @return	无
	 * */
	public BlockHelp(String SystemName) throws Exception {
		this.SystemName = SystemName;
		StoreConfig sc = StoreConfig.getStoreConfig();
		this.filemaxlength = sc.getFileMax();
		this.BlockDataPath = sc.getBlockPath(this.SystemName);
		boolean b = pathUtil.MkdirAll(this.BlockDataPath);
		if(!b){
			throw new Exception("block store dir is null!");
		}
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取当前文件的长度
	 * @param	long fileno 文件编号
	 * @return	返回当前编号的文件长度 long
	 * */
	public long  getFileLength(long fileno)throws Exception{
		long l = -1;
		String pn = this.BlockDataPath + File.separator + FileName + fileno;
		File f = new File(pn);
		if(f.exists()){
			l = f.length();
		}
		if(l == -1) l = 0;
		return l;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	判断是否需要增加新的区块文件
	 * @param	long fileno 文件编号，int blength 当前要写入数据的长度
	 * @return	如果需要新增区块文件返回true，否则false
	 * */
	public boolean  isAddFile(long fileno,int blength)throws Exception{
		boolean b = false;
		long l = this.getFileLength(fileno);
		if((l+blength)>this.filemaxlength){
			b = true;
		}
		return b;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	内部函数，读区块文件中的指定区块信息
	 * @param	fileno Long 文件编号,startpos Long 区块信息存储的起始位置,length Int 读取数据的长度
	 * @return	返回读取的区块字节数组
	 * */
	public byte[] readBlock(long fileno,long startpos,int length)throws Exception{
		byte[] rb = null;
		String np = this.BlockDataPath + File.separator + FileName + fileno;
		synchronized(this.synchObject){
			RandomAccessFile rf = null;
			FileChannel channel = null;
			try{
				File f = new File(np);
				if(!f.exists()){
					return rb;
				}
				
				rf = new RandomAccessFile(np, "r");  
				channel = rf.getChannel(); 
				channel.position(startpos);
				ByteBuffer buf = ByteBuffer.allocate(length);   
				channel.read(buf);
				buf.flip();
				rb = buf.array();
			}catch(Exception e){
				e.printStackTrace();
				throw e;
			}finally{
				if(channel != null){
					try{
						channel.close();
					}catch(Exception ec){
						ec.printStackTrace();
					}
				}
				
				if(rf != null){
					try{
						rf.close();
					}catch(Exception er){
						er.printStackTrace();
					}
				}
			}
		}
		
		return rb;  
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	内部函数，写区块字节数组到指定文件的指定位置
	 * @param	fileno Long 文件编号,startpos Long 区块信息存储的起始位置,bb byte[] 区块字节数组
	 * @return	如果写入成功返回true，否则false
	 * */
	public boolean writeBlock(long fileno,long startpos,byte[] bb)throws Exception{
		boolean b = false;
		String np = this.BlockDataPath + File.separator + FileName + fileno;
		synchronized(this.synchObject){
			RandomAccessFile rf = null;
			FileChannel channel = null;
			try{
				rf = new RandomAccessFile(np, "rw");  
				channel = rf.getChannel(); 
				channel.position(startpos);
				ByteBuffer buf = ByteBuffer.wrap(bb);   
				channel.write(buf);
				b = true;
			}catch(Exception e){
				e.printStackTrace();
				throw e;
			}finally{
				if(channel != null){
					try{
						channel.close();
					}catch(Exception ec){
						ec.printStackTrace();
					}
				}
				
				if(rf != null){
					try{
						rf.close();
					}catch(Exception er){
						er.printStackTrace();
					}
				}
			}
		}
		
		return b;  
	}
	
	
	public static void main(String[] args){
		try{
			BlockHelp bh = new BlockHelp("testSystem");
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
