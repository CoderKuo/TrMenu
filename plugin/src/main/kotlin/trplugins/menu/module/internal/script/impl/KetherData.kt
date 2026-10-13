package trplugins.menu.module.internal.script.impl

import taboolib.library.kether.ArgTypes
import taboolib.library.kether.LocalizedException
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestContext
import taboolib.module.kether.KetherParser
import taboolib.module.kether.scriptParser
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.script.kether.BaseAction
import trplugins.menu.module.internal.script.kether.EditType
import trplugins.menu.module.internal.script.kether.EditType.*
import java.util.concurrent.CompletableFuture

/**
 * @author Arasple
 * @date 2021/1/29 10:17
 */
class KetherData(val type: EditType, private val source: ParsedAction<*>, private val apply: ParsedAction<*>?) :
    BaseAction<Any>() {

    override fun process(context: QuestContext.Frame): CompletableFuture<Any> {
        val viewer = context.viewer()

        return context.newFrame(source).run<String>().thenApply { key ->
            when (type) {
                DEL -> {
                    Metadata.getData(viewer)[key].also {
                        Metadata.setData(viewer, Metadata.DataType.DATA, key, null)
                    }
                }

                SET -> {
                    apply?.let { it1 ->
                        context.newFrame(it1).run<String>().thenApply { apply ->
                            Metadata.setData(viewer, Metadata.DataType.DATA, key, apply)
                        }
                    }
                }

                GET -> Metadata.getData(viewer)[key]
                HAS -> Metadata.getData(viewer).data.containsKey(key)
            }

        }

    }

    companion object {

        @KetherParser(["data"], namespace = "trmenu", shared = true)
        fun parser() = scriptParser {
            val type = when (it.nextToken().lowercase()) {
                "del" -> DEL
                "set" -> SET
                "get" -> GET
                else -> throw LocalizedException.of("unknown type")
            }
            val key = it.next(ArgTypes.ACTION)
            it.mark()
            KetherData(
                type, key,
                try {
                    it.expect("to")
                    it.next(ArgTypes.ACTION)
                } catch (ignored: Exception) {
                    it.reset()
                    null
                }
            )
        }

    }

}