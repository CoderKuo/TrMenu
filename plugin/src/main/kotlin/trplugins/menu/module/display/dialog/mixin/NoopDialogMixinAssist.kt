package trplugins.menu.module.display.dialog.mixin

object NoopDialogMixinAssist : DialogMixinAssist {

    override fun enabled(): Boolean {
        return false
    }

    override fun diagnostics(): String {
        return "Mixin assist is disabled."
    }
}
