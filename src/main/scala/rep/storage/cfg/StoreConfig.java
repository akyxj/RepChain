package rep.storage.cfg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import rep.storage.util.pathUtil;

/**
 * @author jiangbuyun
 * @version	1.0
 * @since	2017-09-28
 * @category	数据存储路径配置的读取类，负责读取系统数据的存储路径。
 * */
public class StoreConfig {
	private static StoreConfig sc = null;
	private Properties pps = null;
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category 私有构造类
	 * */
	private StoreConfig(){
		String cdir = "conf/";
		try{
			pps = new Properties();
			InputStream in = new BufferedInputStream(new FileInputStream(cdir+"Store.properties"));
			pps.load(in);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取存储配置类实例
	 * @param	无
	 * @return	返回存储配置类实例 StoreConfig
	 * */
	public static synchronized StoreConfig getStoreConfig(){
		if(sc == null){
			sc = new StoreConfig();
		}
		
		return sc;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取merkle分组的数目
	 * @param	无
	 * @return	int 返回分组数目
	 * */
	public int getMerkleGroup(){
		int rl = 3;
		String v = pps.getProperty("maxgroup");
		try{
			rl = Integer.parseInt(v);
		}catch(Exception e){
			rl = 3;
		}
		return rl;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取数据库的存储路径
	 * @param	无
	 * @return	String 返回数据库的存储路径
	 * */
	public String getDbPath(){
		String rel = "";
		if(this.pps != null){
			rel = pps.getProperty("dbpath","");
		}
		return rel;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	根据系统名称，获取数据库的存储路径
	 * @param	String 系统名称
	 * @return	String 返回数据库的存储路径
	 * */
	public String getDbPath(String SystemName){
		String rel = getDbPath();
		return rel + File.separator + SystemName;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取区块的存储路径
	 * @param	String 系统名称
	 * @return	String 返回区块的存储路径
	 * */
	public String getBlockPath(){
		String rel = "";
		if(this.pps != null){
			rel = pps.getProperty("blockpath","");
		}
		return rel;
	}
	
	/**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	根据系统名称，获取区块的存储路径
	 * @param	String 系统名称
	 * @return	String 返回区块的存储路径
	 * */
	public String getBlockPath(String SystemName){
		String rel = getBlockPath();
		return rel + File.separator + SystemName;
	}
	
	public long getFileMax(){
		long rl = 200*1000*1000;
		String v = pps.getProperty("filemax");
		try{
			rl = Long.parseLong(v);
		}catch(Exception e){
			rl = 200*1000*1000;
		}
		return rl;
	}
	
	public long getFreeDiskSpace(){
		String bpath = this.getBlockPath();
		/*try {
			if(pathUtil.FileExists(bpath) == -1){
				pathUtil.MkdirAll(bpath);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		File f = new File(bpath);
		long l = f.getFreeSpace();
		return l;
	}
	
	public static void main(String[] args){
		StoreConfig msc = StoreConfig.getStoreConfig();
		System.out.println(msc.getDbPath());
		System.out.println(msc.getDbPath("mySystem"));
	}
}
