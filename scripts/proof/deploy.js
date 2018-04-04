function write(pn,pv){
	shim.setVal(pn,pv);
}
function read(pn){
	return shim.getVal(pn);
}

function put_proof(pn,pv){	
	//先检查该hash是否已经存在,如果已存在,抛异常
	var pv0 = read(pn);
//	if(pv0)
//		throw '['+pn+']已存在，当前值['+pv0+']';
	shim.setVal(pn,pv);
	print('putProof:'+pn+':'+pv);
}
function signup(cert,inf){
	return shim.signup(cert,inf);
}