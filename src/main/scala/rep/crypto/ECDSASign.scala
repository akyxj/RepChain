package rep.crypto

import java.security._
import java.io._
import java.security.cert.{ Certificate, CertificateFactory }

import com.google.protobuf.ByteString
import fastparse.utils.Base64
import rep.utils.SerializeUtils

import scala.collection.mutable
import com.fasterxml.jackson.core.Base64Variants
//import org.bouncycastle.openssl.MiscPEMGenerator
import java.security.cert.X509Certificate
//import org.bouncycastle.openssl.PEMWriter

/**
 * 系统密钥相关伴生对象
 * @author shidianyue
 * @version	1.0
 * @since	1.0
 */
object ECDSASign extends ECDSASign {
  //TODO kami （现阶段alias和SYSName相同，将来不一定，所以目前在接口层将其分开，但是调用时用的是一个）

  //store itsself key and certification
  var keyStorePath = mutable.HashMap[String, String]()
  var password = mutable.HashMap[String, String]()
  //store the trust list of other nodes' certification
  var trustKeyStorePath = ""
  var passwordTrust = ""

  var keyStore = mutable.HashMap[String, KeyStore]()
  var trustKeyStore = KeyStore.getInstance(KeyStore.getDefaultType)
  var trustkeysPubAddrMap = mutable.HashMap[String, Certificate]()

  def apply(alias: String, jksPath: String, password: String, jksTrustPath: String, passwordTrust: String) = {
    keyStorePath(alias) = jksPath
    this.password(alias) = password
    //TODO kami 如果与之前路径不同，如何处理？
    if (trustKeyStorePath == "") {
      trustKeyStorePath = jksTrustPath
      this.passwordTrust = passwordTrust
    }
  }

  /**
   * 通过参数获取相关的密钥对、证书（动态加载）
   *
   * @param jks_file
   * @param password
   * @param alias
   * @return
   */
  def getKeyPairFromJKS(jks_file: File, password: String, alias: String): (PrivateKey, PublicKey) = {
    val store = KeyStore.getInstance(KeyStore.getDefaultType)
    val fis = new FileInputStream(jks_file)
    val pwd = password.toCharArray()
    store.load(fis, pwd)
    val sk = store.getKey(alias, pwd)
    val cert = store.getCertificate(alias)
    (sk.asInstanceOf[PrivateKey], cert.getPublicKey())
  }

  /**
   * 在信任列表中获取证书（通过alias）
   *
   * @param cert
   * @return
   */
  def getAliasByCert(cert: Certificate): Option[String] = {
    val alias = trustKeyStore.getCertificateAlias(cert)
    if (alias == null) Option.empty else Option(alias)
  }

  /**
   * 获取证书的Base58地址
   * @param cert
   * @return
   */
  def getAddrByCert(cert: Certificate): String = {
    Base58.encode(Sha256.hash(cert.getPublicKey.getEncoded))
  }

  /**
   * 获取证书的短地址（Bitcoin方法）
   * @param cert 对象
   * @return
   */
  def getBitcoinAddrByCert(cert: Certificate): String = {
    BitcoinUtils.calculateBitcoinAddress(cert.getPublicKey.getEncoded)
  }

  /**
   * 获取证书的短地址
   * @param certByte 字节
   * @return
   */
  def getBitcoinAddrByCert(certByte: Array[Byte]): String = {
    val cert = SerializeUtils.deserialise(certByte).asInstanceOf[Certificate]
    BitcoinUtils.calculateBitcoinAddress(cert.getPublicKey.getEncoded)
  }

  /**
   * 获取指定alias的证书地址
   * @param alias
   * @return
   */
  def getAddr(alias: String): String = {
    getAddrByCert(keyStore(alias).getCertificate(alias))
  }

  /**
   * 根据短地址获取证书
   * @param addr
   * @return
   */
  def getCertByBitcoinAddr(addr: String): Option[Certificate] = {
    trustkeysPubAddrMap.get(addr)
  }

  /**
   * 通过配置信息获取证书（动态加载）
   *
   * @param jks_file
   * @param password
   * @param alias
   * @return
   */
  def getCertFromJKS(jks_file: File, password: String, alias: String): Certificate = {
    val store = KeyStore.getInstance(KeyStore.getDefaultType)
    val fis = new FileInputStream(jks_file)
    val pwd = password.toCharArray()
    store.load(fis, pwd)
    val sk = store.getKey(alias, pwd)
    val cert = store.getCertificate(alias)
    // test  2017-11-23 by zyf
    //    val test = Base64Variants.getDefaultVariant.encode(cert.getEncoded)
    //    val test = Base64Variants.getDefaultVariant.encode(SerializeUtils.serialise(cert))
    //TODO by zyf 2017-12-11 主要是对pem格式证书字符串进行处理，生成证书对象，去除头尾，然后进行base64解码
    val cf = CertificateFactory.getInstance("X.509")
    val cert3 = cf.generateCertificate(new FileInputStream(new File("jks/certs/056-52103771.cert")))
    val array3 = SerializeUtils.serialise(cert3)
    val base64cert3 = Base64Variants.getDefaultVariant.encode(array3)
    val cert2 = "-----BEGIN CERTIFICATE-----\r\nMIIBmjCCAT+gAwIBAgIEWWV+AzAKBggqhkjOPQQDAjBWMQswCQYDVQQGEwJjbjEL\r\nMAkGA1UECAwCYmoxCzAJBgNVBAcMAmJqMREwDwYDVQQKDAhyZXBjaGFpbjEOMAwG\r\nA1UECwwFaXNjYXMxCjAIBgNVBAMMATEwHhcNMTcwNzEyMDE0MjE1WhcNMTgwNzEy\r\nMDE0MjE1WjBWMQswCQYDVQQGEwJjbjELMAkGA1UECAwCYmoxCzAJBgNVBAcMAmJq\r\nMREwDwYDVQQKDAhyZXBjaGFpbjEOMAwGA1UECwwFaXNjYXMxCjAIBgNVBAMMATEw\r\nVjAQBgcqhkjOPQIBBgUrgQQACgNCAAT6VLE/eF9+sK1ROn8n6x7hKsBxehW42qf1\r\nIB8quBn5OrQD3x2H4yZVDwPgcEUCjH8PcFgswdtbo8JL/7f66yECMAoGCCqGSM49\r\nBAMCA0kAMEYCIQCud+4/3njnfUkG9ffSqcHhnsuZNMQwaW62EVXbcjoiBgIhAPoL\r\nJK1D06IMoholYcsgTQb5Trrej/erZONMm1cS1iP+\r\n-----END CERTIFICATE-----\r\n"
    val cert1 = cf.generateCertificate(
      new ByteArrayInputStream(
        Base64.Decoder(cert2.replaceAll("\r\n", "").stripPrefix("-----BEGIN CERTIFICATE-----").stripSuffix("-----END CERTIFICATE-----")).toByteArray))
    val pemCertPre = Base64.Encoder(cert1.getEncoded).toBase64
//    var sw = new StringWriter()
//    //    val mis = new MiscPEMGenerator(cert)
//    var pw = new PEMWriter(sw)
//    pw.writeObject(cert1)
//    println(sw.toString())
    println(cert.isInstanceOf[X509Certificate])

    cert
  }

  /**
   * 将pem格式证书字符串转换为certificate
   * @param pem
   * @return
   */
  def getCertByPem(pemcert: String): Certificate = {
    val cf = CertificateFactory.getInstance("X.509")
    val cert = cf.generateCertificate(
      new ByteArrayInputStream(
        Base64.Decoder(pemcert.replaceAll("\r\n", "").stripPrefix("-----BEGIN CERTIFICATE-----").stripSuffix("-----END CERTIFICATE-----")).toByteArray))
    cert
  }

  
  /**
   * 获取alias的密钥对和证书（系统初始化）
   *
   * @param alias
   * @return
   */
  def getKeyPair(alias: String): (PrivateKey, PublicKey, Array[Byte]) = {
    val sk = keyStore(alias).getKey(alias, password(alias).toCharArray)
    val cert = keyStore(alias).getCertificate(alias)
    //    (sk.asInstanceOf[PrivateKey], cert.getPublicKey(), cert.getEncoded)
    (sk.asInstanceOf[PrivateKey], cert.getPublicKey(), SerializeUtils.serialise(cert))
  }

  /**
   * 获取alias的证书（系统初始化）
   *
   * @param alias
   * @return
   */
  def getCert(alias: String): Certificate = {
    keyStore(alias).getCertificate(alias)
  }

  /**
   * 在信任列表中获取alias的证书（系统初始化）
   *
   * @param alias
   * @return
   */
  def getKeyPairTrust(alias: String): PublicKey = {
    val sk = trustKeyStore.getKey(alias, passwordTrust.toCharArray)
    val cert = trustKeyStore.getCertificate(alias)
    cert.getPublicKey()
  }

  /**
   * 判断两个证书是否相同
   *
   * @param alias
   * @param cert
   * @return
   */
  def isCertTrust(alias: String, cert: Array[Byte]): Boolean = {
    val sk = trustKeyStore.getKey(alias, passwordTrust.toCharArray)
    val certT = trustKeyStore.getCertificate(alias)
    //寻找方法能够恢复cert？
    certT.getEncoded.equals(cert)
  }

  /**
   * 预加载系统密钥对和信任证书
   *
   * @param alias
   */
  def preLoadKey(alias: String): Unit = {
    val fis = new FileInputStream(new File(keyStorePath(alias)))
    val pwd = password(alias).toCharArray()
    if (keyStore.contains(alias)) keyStore(alias).load(fis, pwd)
    else {
      val keyS = KeyStore.getInstance(KeyStore.getDefaultType)
      keyS.load(fis, pwd)
      keyStore(alias) = keyS
    }

    val fisT = new FileInputStream(new File(trustKeyStorePath))
    val pwdT = passwordTrust.toCharArray()
    trustKeyStore.load(fisT, pwdT)
    loadTrustkeysPubAddrMap()
  }

  /**
   * 初始化信任证书中对短地址和证书的映射
   */
  def loadTrustkeysPubAddrMap(): Unit = {
    val enums = trustKeyStore.aliases()
    while (enums.hasMoreElements) {
      val alias = enums.nextElement()
      val cert = trustKeyStore.getCertificate(alias)
      trustkeysPubAddrMap.put(getBitcoinAddrByCert(cert), cert)
    }
  }

  /**
   * 获取本地证书，得到证书类和其序列化的字节序列
   *
   * @param certPath
   * @return 字节序列（通过base58进行转化）
   */
  def loadCertByPath(certPath: String): (Certificate, Array[Byte], String) = {
    val certF = CertificateFactory.getInstance("X.509")
    val fileInputStream = new FileInputStream(certPath)
    val x509Cert = certF.generateCertificate(fileInputStream)
    val arrayCert = SerializeUtils.serialise(x509Cert)
    (x509Cert, arrayCert, Base64.Encoder(arrayCert).toBase64)
  }

  /**
   * 添加证书到信任列表
   *
   * @param cert 字节数组
   * @param alias
   * @return
   */
  def loadTrustedCert(cert: Array[Byte], alias: String): Boolean = {
    val certTx = SerializeUtils.deserialise(cert).asInstanceOf[Certificate]
    getAliasByCert(certTx).getOrElse(None) match {
      case None =>
        trustKeyStore.setCertificateEntry(alias, certTx)
        trustkeysPubAddrMap.put(getBitcoinAddrByCert(certTx), certTx)
        val fileOutputStream = new FileOutputStream(trustKeyStorePath)
        trustKeyStore.store(fileOutputStream, passwordTrust.toCharArray)
        true
      case _ =>
        false
    }
  }

  /**
   * 添加证书到信任列表
   *
   * @param cert base64字符串
   * @param alias
   * @return
   */
  def loadTrustedCertBase64(cert: String, alias: String): Boolean = {
    val certTx = SerializeUtils.deserialise(Base64.Decoder(cert).toByteArray).asInstanceOf[Certificate]
    getAliasByCert(certTx).getOrElse(None) match {
      case None =>
        trustKeyStore.setCertificateEntry(alias, certTx)
        trustkeysPubAddrMap.put(getBitcoinAddrByCert(certTx), certTx)
        val fileOutputStream = new FileOutputStream(trustKeyStorePath)
        trustKeyStore.store(fileOutputStream, passwordTrust.toCharArray)
        true
      case _ =>
        false
    }
  }
  def main(args: Array[String]): Unit = {
    println(ByteString.copyFromUtf8(ECDSASign.getBitcoinAddrByCert(ECDSASign.getCertFromJKS(new File("jks/mykeystore_1.jks"), "123", "1"))).toStringUtf8)
    println(ECDSASign.getBitcoinAddrByCert(ECDSASign.getCertFromJKS(new File("jks/mykeystore_2.jks"), "123", "2")))
    println(ECDSASign.getBitcoinAddrByCert(ECDSASign.getCertFromJKS(new File("jks/mykeystore_3.jks"), "123", "3")))
    println(ECDSASign.getBitcoinAddrByCert(ECDSASign.getCertFromJKS(new File("jks/mykeystore_4.jks"), "123", "4")))
    println(ECDSASign.getBitcoinAddrByCert(ECDSASign.getCertFromJKS(new File("jks/mykeystore_5.jks"), "123", "5")))
  }

}

/**
 * 系统密钥相关类
 * @author shidianyue
 * @version	1.0
 * @since	1.0
 */
class ECDSASign extends SignFunc {
  /**
   * 签名
   *
   * @param privateKey
   * @param message
   * @return
   */
  def sign(privateKey: PrivateKey, message: Array[Byte]): Array[Byte] = {
    val s1 = Signature.getInstance("SHA1withECDSA");
    s1.initSign(privateKey);
    s1.update(message)
    s1.sign()
  }

  /**
   * 验证
   *
   * @param signature
   * @param message
   * @param publicKey
   * @return
   */
  def verify(signature: Array[Byte], message: Array[Byte], publicKey: PublicKey): Boolean = {
    val s2 = Signature.getInstance("SHA1withECDSA");
    s2.initVerify(publicKey);
    s2.update(message)
    s2.verify(signature);
  }

}