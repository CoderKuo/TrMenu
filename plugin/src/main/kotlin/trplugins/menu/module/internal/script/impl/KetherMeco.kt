package trplugins.menu.module.internal.script.impl

import taboolib.library.kether.ArgTypes
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestContext
import taboolib.module.kether.KetherParser
import taboolib.module.kether.scriptParser
import trplugins.menu.module.internal.hook.HookPlugin
import trplugins.menu.module.internal.script.kether.BaseAction
import java.util.concurrent.CompletableFuture

/**
 * @author Arasple
 * @date 2024/3/7
 */
class KetherMeco(val currencyId: String, val operator: String, val value: ParsedAction<*>) : BaseAction<Boolean>() {

    override fun process(context: QuestContext.Frame): CompletableFuture<Boolean> {
        return context.newFrame(value).run<Any>().thenApply {
            val amount = it.toString().toDoubleOrNull() ?: 0.0
            val player = context.viewer()
            val balance = HookPlugin.getMeowEco().getBalance(player, currencyId)

            when (operator) {
                "==" -> balance == amount
                "!=" -> balance != amount
                ">" -> balance > amount
                "<" -> balance < amount
                ">=" -> balance >= amount
                "<=" -> balance <= amount
                else -> false
            }
        }
    }

    companion object {

        /**
         * meco points >= 100
         */
        @KetherParser(["meco"], namespace = "trmenu", shared = true)
        fun parser() = scriptParser {
            val currencyId = it.nextToken()
            val operator = it.nextToken()
            val value = it.next(ArgTypes.ACTION)
            KetherMeco(currencyId, operator, value)
        }

    }

}
