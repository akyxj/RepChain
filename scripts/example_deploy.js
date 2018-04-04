function loadCert(cert){
	shim.loadCert(cert);
	print("cert:"+cert);
}
function write(pn,pv){
	shim.setVal(pn,pv);
	// print("setState:"+pn+":"+pv)
}
function set(pm){
	for(x in pm){
		write(x,pm[x]);
	}
}
function read(pn){
	return shim.getVal(pn);
}
function transfer(afrom,ato,amount){
	// print('tx_account:'+tx_account);
	if(afrom != tx_account)
		throw "["+tx_account+"]无权从["+afrom+"]转出资产"
	var rfrom = read(afrom);
	// print(rfrom + ":" + amount)
	if(rfrom<amount)
		throw "余额不足!"
	var rto = read(ato);
	write(afrom,rfrom-amount);
	write(ato,rto+amount);
	// print(ato+':'+read(ato))
}

function put_proof(pn,pv){	
	//先检查该hash是否已经存在,如果已存在,抛异常
	var pv0 = read(pn);
	if(pv0)
		throw '['+pn+']已存在，当前值['+pv0+']';
	shim.setVal(pn,pv);
	print('putProof:'+pn+':'+pv);
}
function signup(cert,inf){
	shim.check(tx_account,tx)
	return shim.signup(cert,inf);
}

function destroyCert(certAddr){
	shim.check(tx_account,tx)
	shim.destroyCert(certAddr);
}

function replaceCert(pemCert,certAddr){
	shim.check(tx_account,tx)
	return shim.replaceCert(pemCert,certAddr);
}