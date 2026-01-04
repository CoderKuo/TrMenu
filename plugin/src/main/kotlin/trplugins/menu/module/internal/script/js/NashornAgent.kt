package trplugins.menu.module.internal.script.js

import com.google.common.collect.Maps
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common5.compileJS
import trplugins.menu.util.EvalResult
import javax.script.CompiledScript
import javax.script.SimpleScriptContext

@RuntimeDependencies(
    RuntimeDependency(
        "!org.ow2.asm:asm:9.7.1",
        test = "!jdk.nashorn.api.scripting.NashornScriptEngineFactory"
    ),
    RuntimeDependency(
        "!org.ow2.asm:asm-commons:9.7.1",
        test = "!jdk.nashorn.api.scripting.NashornScriptEngineFactory"
    ),
    RuntimeDependency(
        "!org.ow2.asm:asm-tree:9.7.1",
        test = "!jdk.nashorn.api.scripting.NashornScriptEngineFactory"
    ),
    RuntimeDependency(
        "!org.ow2.asm:asm-util:9.7.1",
        test = "!jdk.nashorn.api.scripting.NashornScriptEngineFactory"
    ),
    RuntimeDependency(
        "!org.ow2.asm:asm-analysis:9.7.1",
        test = "!jdk.nashorn.api.scripting.NashornScriptEngineFactory"
    )
)

object NashornAgent {
    private val compiledScripts = Maps.newConcurrentMap<String, CompiledScript>();
    fun preCompile(script: String): CompiledScript {
        return compiledScripts.computeIfAbsent(script) {
            script.compileJS()
        }
    }
    fun eval(context:SimpleScriptContext, script: String, cacheScript: Boolean = true): EvalResult {
        val compiledScript =
            if (cacheScript) preCompile(script)
            else script.compileJS()

        return EvalResult(compiledScript?.eval(context))
    }
}