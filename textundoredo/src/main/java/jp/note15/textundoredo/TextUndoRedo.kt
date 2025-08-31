package jp.note15.textundoredo

import android.text.Editable
import android.text.Selection
import android.text.Spanned
import android.text.Spanned.SPAN_COMPOSING
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.TextView
import java.util.LinkedList

class TextUndoRedo(private val textView: TextView) {
    var stateChangeListener: (() -> Unit)? = null

    private var isUndoOrRedo = false

    private val editHistory: EditHistory = EditHistory()

    private val changeListener: EditTextChangeListener

    init {
        changeListener = EditTextChangeListener()
        textView.addTextChangedListener(changeListener)
    }

    fun disconnect() {
        textView.removeTextChangedListener(changeListener)
    }

    fun setMaxHistorySize(maxHistorySize: Int) {
        editHistory.updateMaxHistorySizeAndTrim(maxHistorySize)
    }

    fun clearHistory() {
        editHistory.clear()
        stateChangeListener?.invoke()
    }

    val canUndo: Boolean
        get() = editHistory.position > 0

    fun undo() {
        val edit = editHistory.previous ?: return

        val text = textView.editableText
        val start = edit.start
        val end = start + (edit.after?.length ?: 0)

        isUndoOrRedo = true
        text.replace(start, end, edit.before)
        isUndoOrRedo = false

        removeComposingSpans()

        Selection.setSelection(text, start + (edit.before?.length ?: 0))
        stateChangeListener?.invoke()
    }

    val canRedo: Boolean
        get() = editHistory.position < editHistory.history.size

    fun redo() {
        val edit = editHistory.next ?: return

        val text = textView.editableText
        val start = edit.start
        val end = start + (edit.before?.length ?: 0)

        isUndoOrRedo = true
        text.replace(start, end, edit.after)
        isUndoOrRedo = false

        removeComposingSpans()

        Selection.setSelection(text, start + (edit.after?.length ?: 0))
        stateChangeListener?.invoke()
    }

    private fun removeComposingSpans() {
        textView.clearComposingText()
    }

    private class EditHistory {
        var position = 0
        var maxHistorySize = -1

        val history: LinkedList<EditItem> = LinkedList()

        fun clear() {
            position = 0
            history.clear()
        }

        fun add(item: EditItem) {
            while (history.size > position) {
                history.removeLast()
            }
            history.add(item)
            position++

            if (maxHistorySize >= 0) {
                trimHistory()
            }
        }

        fun updateMaxHistorySizeAndTrim(maxHistorySize: Int) {
            this.maxHistorySize = maxHistorySize
            if (this.maxHistorySize >= 0) {
                trimHistory()
            }
        }

        private fun trimHistory() {
            while (history.size > maxHistorySize) {
                history.removeFirst()
                position--
            }

            if (position < 0) {
                position = 0
            }
        }

        val current: EditItem?
            get() = history.getOrNull(position - 1)

        val previous: EditItem?
            get() {
                if (position == 0) return null
                position--
                return history.getOrNull(position)
            }

        val next: EditItem?
            get() {
                if (position >= history.size) return null
                val item = history.getOrNull(position)
                if (item != null) {
                    position++
                }
                return item
            }
    }

    private data class EditItem(
        var start: Int, var before: CharSequence?, var after: CharSequence?
    )

    internal enum class ActionType {
        INSERT, DELETE, PASTE, NOT_DEF
    }

    private inner class EditTextChangeListener : TextWatcher {
        private var beforeChange: CharSequence? = null
        private var lastActionType: ActionType? = ActionType.NOT_DEF
        private var lastActionTime: Long = 0

        private var isImeComposing: Boolean = false
        private var imeInitialBeforeText: CharSequence? = null

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (isUndoOrRedo) return
            beforeChange = s.subSequence(start, start + count)
            if (!isImeComposing) {
                imeInitialBeforeText = s.toString()
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (isUndoOrRedo) return

            val hasComposingSpan = s is Spanned && s.getSpans(0, s.length, Any::class.java)
                .any { s.getSpanFlags(it) and SPAN_COMPOSING != 0 }

            if (hasComposingSpan) {
                if (!isImeComposing) {
                    isImeComposing = true
                }
            } else if (!isImeComposing) {
                addEdit(start, beforeChange, s.subSequence(start, start + count))
            }
        }

        override fun afterTextChanged(s: Editable?) {
            if (isUndoOrRedo || s == null) return

            val hasComposingSpan = s.getSpans(0, s.length, Any::class.java)
                .any { s.getSpanFlags(it) and SPAN_COMPOSING != 0 }

            if (isImeComposing && !hasComposingSpan) {
                val (prefix, before, after) = diff(imeInitialBeforeText, s)
                if (before.isNotEmpty() || after.isNotEmpty()) {
                    addEdit(prefix, before, after)
                }
                isImeComposing = false
                imeInitialBeforeText = null
            }
        }

        private fun diff(
            initial: CharSequence?,
            final: CharSequence
        ): Triple<Int, CharSequence, CharSequence> {
            val s1 = initial ?: ""
            val s2 = final
            val commonPrefix = commonPrefixLength(s1, s2)
            val commonSuffix = commonSuffixLength(s1, s2, commonPrefix)
            val before = s1.subSequence(commonPrefix, s1.length - commonSuffix)
            val after = s2.subSequence(commonPrefix, s2.length - commonSuffix)
            return Triple(commonPrefix, before, after)
        }

        private fun commonPrefixLength(s1: CharSequence, s2: CharSequence): Int {
            var i = 0
            while (i < s1.length && i < s2.length && s1[i] == s2[i]) {
                i++
            }
            return i
        }

        private fun commonSuffixLength(s1: CharSequence, s2: CharSequence, prefix: Int): Int {
            var i = 0
            while (i < s1.length - prefix && i < s2.length - prefix && s1[s1.length - 1 - i] == s2[s2.length - 1 - i]) {
                i++
            }
            return i
        }

        private fun getActionType(before: CharSequence?, after: CharSequence?): ActionType {
            if (!TextUtils.isEmpty(before) && TextUtils.isEmpty(after)) return ActionType.DELETE
            if (TextUtils.isEmpty(before) && !TextUtils.isEmpty(after)) return ActionType.INSERT
            if (!TextUtils.isEmpty(before) && !TextUtils.isEmpty(after)) return ActionType.PASTE
            return ActionType.NOT_DEF
        }

        private fun addEdit(start: Int, before: CharSequence?, after: CharSequence?) {
            val at = getActionType(before, after)
            val editItem = editHistory.current

            if (TextUtils.equals(before, after)) return

            val batch =
                editItem != null && at == lastActionType && at != ActionType.PASTE && System.currentTimeMillis() - lastActionTime < BATCH_TIME_THRESHOLD_MS

            if (!batch) {
                editHistory.add(EditItem(start, before, after))
            } else {
                if (at == ActionType.DELETE) {
                    editItem.start = start
                    editItem.before = TextUtils.concat(before, editItem.before)
                } else { // INSERT
                    val relativeStart = start - editItem.start
                    val currentAfter = editItem.after?.toString() ?: ""
                    if (relativeStart >= 0 && relativeStart <= currentAfter.length) {
                        editItem.after =
                            StringBuilder(currentAfter).insert(relativeStart, after).toString()
                    } else {
                        editItem.after = TextUtils.concat(currentAfter, after)
                    }
                }
            }
            lastActionType = at
            lastActionTime = System.currentTimeMillis()
            stateChangeListener?.invoke()
        }
    }

    companion object {
        private const val BATCH_TIME_THRESHOLD_MS = 1000L
    }
}
