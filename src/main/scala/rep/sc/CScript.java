package rep.sc;

import javax.script.ScriptEngine;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

class CScript{
	public static ScriptEngine getEngine(){
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
		String[] stringArray = new String[]{"-doe", "--global-per-engine"};
		ScriptEngine scriptEngine = factory.getScriptEngine(stringArray);
		return scriptEngine;
	}
}