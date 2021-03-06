package rep.storage.test

import org.iq80.leveldb._
import org.fusesource.leveldbjni.JniDBFactory._
import java.io._;

object dbtest {
  var db :DB = null;
  var db1:DB = null;
  
  def main(args: Array[String]): Unit = {
    val leveldbOptions = new Options().createIfMissing(true)
    def leveldbfactory = factory;
    //def leveldbfactory1 = factory;
    
    
		
		try {
			db = leveldbfactory.open(new File("/Users/repchain/leveldbdata/example"), leveldbOptions);
			db1 = leveldbfactory.open(new File("/Users/repchain/leveldbdata/example1"), leveldbOptions);
			
			db.put(bytes("Tampa"), bytes("rocks"));
			db1.put(bytes("Tampa1"), bytes("rocks1"));
			
			val value = asString(db.get(bytes("Tampa")));
			val value1 = asString(db1.get(bytes("Tampa1")));
			
			
			System.out.println("key="+"Tampa"+"\tvalue="+value);
			System.out.println("key="+"Tampa1"+"\tvalue="+value1);
			
			db.delete(bytes("Tampa"));
			db1.delete(bytes("Tampa1"));
			
			val value3 = asString(db.get(bytes("Tampa")));
			val value4 = asString(db1.get(bytes("Tampa1")));
			
			System.out.println("key="+"Tampa"+"\tvalue="+value3);
			System.out.println("key="+"Tampa1"+"\tvalue="+value4);
			
			
			 var data = "";
        var i = 0;
        for( i <- 0 to 1024) {
            data+= 'a'+scala.util.Random.nextInt(26);
        }
        
        for( i <- 0 to 5*1024) {
            db.put(bytes("row"+i), bytes(data));
        }

        var as = db.get("row".getBytes());
        
       
        var r = new Range(bytes("row"),bytes("s"));
        var approximateSizes =  db.getApproximateSizes(r);
        println(approximateSizes);
        println(1, approximateSizes.length);
        
       //  var approximateSizes :Long[] = dbgetApproximateSizes(new Range(bytes("row"), bytes("s")));
			
		  //println(approximateSizes[0])
		}catch{
		   case e: NullPointerException => println(e.printStackTrace()); System.exit(-1) 
			 case unknown => println(unknown.printStackTrace()); System.exit(-1)
		}finally {
		  if(db != null){
			  try {
				db.close();
			} catch  {
				case unknown => println(unknown.printStackTrace()); System.exit(-1)
			}
		  }
		}

  }
}
