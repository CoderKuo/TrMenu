package trplugins.menu.module.internal.script.impl

import com.xiaoyiluck.meoweco.api.MeowEcoAPI
import taboolib.library.kether.ArgTypes
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestContext
import taboolib.module.kether.KetherParser
import taboolib.module.kether.scriptParser
import trplugins.menu.module.internal.script.kether.BaseAction
import java.util.concurrent.CompletableFuture

class KetherMeco(
    private val currencyId: String,
    private val operator: String,
    private val value: ParsedAction<*>,
) : BaseAction<Boolean>() {

    override fun process(context: QuestContext.Frame): CompletableFuture<Boolean> {
        val viewer = context.viewer()
        return context.newFrame(value).run<Any>().thenApply {
            val target = it.toString().toDoubleOrNull() ?: return@thenApply false
            val balance = MeowEcoAPI.get()?.getBalance(viewer.uniqueId, currencyId) ?: 0.0
            when (operator) {
                ">" -> balance > target
                ">=" -> balance >= target
                "<" -> balance < target
                "<=" -> balance <= target
                "==", "=" -> kotlin.math.abs(balance - target) < 0.001
                "!=" -> kotlin.math.abs(balance - target) >= 0.001
                else -> false
            }
        }
    }

    companion object {

        @KetherParser(["meco"], namespace = "trmenu", shared = true)
        fun parser() = scriptParser {
            val currencyId = it.nextToken()
            val operator = it.nextToken()
            val value = it.next(ArgTypes.ACTION)
            KetherMeco(currencyId, operator, value)
        }
    }
}

