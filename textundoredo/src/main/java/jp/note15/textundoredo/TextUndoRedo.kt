package jp.note15.textundoredo

import android.text.Editable
import android.text.Selection
import android.text.Spanned
import android.text.Spanned.SPAN_COMPOSING
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
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
        get() = editHistory.position > 0 && !changeListener.isImeComposing

    fun undo() {
        d(TAG, "undo: canUndo=${canUndo}, isImeComposing=${changeListener.isImeComposing}")
        if (!canUndo || changeListener.isImeComposing) return
        applyOperation(true)
        d(TAG, "undo: completed")
    }

    val canRedo: Boolean
        get() = editHistory.position < editHistory.history.size && !changeListener.isImeComposing

    fun redo() {
        d(TAG, "redo: canRedo=${canRedo}, isImeComposing=${changeListener.isImeComposing}")
        if (!canRedo || changeListener.isImeComposing) return
        applyOperation(false)
        d(TAG, "redo: completed")
    }

    private fun applyOperation(isUndo: Boolean) {
        val edit = (if (isUndo) editHistory.previous else editHistory.next) ?: return
        val text = textView.editableText
        val changes = if (isUndo) edit.changes.reversed() else edit.changes

        isUndoOrRedo = true
        for (change in changes) {
            if (isUndo) {
                val end = change.start + change.newText.length
                text.replace(change.start, end, change.oldText)
            } else {
                val end = change.start + change.oldText.length
                text.replace(change.start, end, change.newText)
            }
        }
        isUndoOrRedo = false

        removeComposingSpans()

        val selectionChange = if (isUndo) edit.changes.firstOrNull() else edit.changes.lastOrNull()
        val selection = if (selectionChange != null) {
            if (isUndo) {
                selectionChange.start + selectionChange.oldText.length
            } else {
                selectionChange.start + selectionChange.newText.length
            }
        } else {
            text.length
        }
        Selection.setSelection(text, selection)

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
            d(TAG, "EditHistory: clear")
        }

        fun add(item: EditItem) {
            d(
                TAG,
                "EditHistory: add item with ${item.changes.size} changes. Current position: $position, history size: ${history.size}"
            )
            while (history.size > position) {
                history.removeLast()
            }
            history.add(item)
            position++

            if (maxHistorySize >= 0) {
                trimHistory()
            }
            d(TAG, "EditHistory: added. New position: $position, history size: ${history.size}")
        }

        fun updateMaxHistorySizeAndTrim(maxHistorySize: Int) {
            this.maxHistorySize = maxHistorySize
            if (this.maxHistorySize >= 0) {
                trimHistory()
            }
            d(TAG, "EditHistory: updateMaxHistorySizeAndTrim: $maxHistorySize")
        }

        private fun trimHistory() {
            while (history.size > maxHistorySize) {
                history.removeFirst()
                position--
            }

            if (position < 0) {
                position = 0
            }
            d(
                TAG,
                "EditHistory: trimHistory. New position: $position, history size: ${history.size}"
            )
        }

        val current: EditItem?
            get() = history.getOrNull(position - 1)

        val previous: EditItem?
            get() {
                if (position == 0) {
                    d(TAG, "EditHistory: previous - no previous item (position is 0)")
                    return null
                }
                position--
                val item = history.getOrNull(position)
                d(TAG, "EditHistory: previous - new position: $position, item: ${item != null}")
                return item
            }

        val next: EditItem?
            get() {
                if (position >= history.size) {
                    d(TAG, "EditHistory: next - no next item (position >= history size)")
                    return null
                }
                val item = history.getOrNull(position)
                if (item != null) {
                    position++
                }
                d(TAG, "EditHistory: next - new position: $position, item: ${item != null}")
                return item
            }
    }

    // New DiffOperation data class
    private data class DiffOperation(
        val start: Int, val oldText: CharSequence, val newText: CharSequence
    )

    // Modified EditItem to hold a list of DiffOperation
    private data class EditItem(
        val changes: List<DiffOperation>
    )

    internal enum class ActionType {
        INSERT, DELETE, PASTE, NOT_DEF
    }

    private inner class EditTextChangeListener : TextWatcher {
        private var beforeChange: CharSequence? = null
        private var lastActionType: ActionType? = ActionType.NOT_DEF
        private var lastActionTime: Long = 0

        var isImeComposing: Boolean = false
        private var imeInitialBeforeText: CharSequence? = null
        private val imeCompositionChanges: MutableList<DiffOperation> =
            mutableListOf() // To store changes during IME composition

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            d(
                TAG,
                "beforeTextChanged: s='${s}', start=${start}, count=${count}, after=${after}, isUndoOrRedo=${isUndoOrRedo}, isImeComposing=${isImeComposing}"
            )
            if (isUndoOrRedo) return
            beforeChange = s.subSequence(start, start + count)
            if (!isImeComposing) {
                imeInitialBeforeText = s.toString()
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            d(
                TAG,
                "onTextChanged: s='${s}', start=${start}, before=${before}, count=${count}, isUndoOrRedo=${isUndoOrRedo}, isImeComposing=${isImeComposing}"
            )
            if (isUndoOrRedo) return

            val hasComposingSpan = hasComposingSpan(s)

            val newText = s.subSequence(start, start + count)
            val oldText = beforeChange // This is the text that was replaced

            if (hasComposingSpan && !isImeComposing && before < count) { // Start new IME composition
                isImeComposing = true
                stateChangeListener?.invoke()
                imeCompositionChanges.clear() // Clear previous changes for a new composition
                d(TAG, "onTextChanged: IME composition started.")
            }

            if (isImeComposing) { // Always add to IME changes if composing
                imeCompositionChanges.add(DiffOperation(start, oldText ?: "", newText))
                d(
                    TAG,
                    "onTextChanged: Added IME composition change: start=$start, oldText='${oldText}', newText='${newText}'"
                )
            } else { // Normal typing
                addEdit(start, oldText, newText)
                d(TAG, "onTextChanged: Normal typing, added single edit.")
            }
        }

        override fun afterTextChanged(s: Editable?) {
            d(
                TAG,
                "afterTextChanged: s='${s}', isUndoOrRedo=${isUndoOrRedo}, isImeComposing=${isImeComposing}"
            )
            if (isUndoOrRedo || s == null) return

            val hasComposingSpan = hasComposingSpan(s)

            if (isImeComposing && !hasComposingSpan) {
                // IME composition ended
                d(
                    TAG,
                    "afterTextChanged: IME composition ended. Adding ${imeCompositionChanges.size} changes to history."
                )
                if (imeCompositionChanges.isNotEmpty()) {
                    editHistory.add(EditItem(imeCompositionChanges.toList()))
                    imeCompositionChanges.clear() // Clear after adding to history
                }
                isImeComposing = false
                stateChangeListener?.invoke()
                imeInitialBeforeText = null // No longer needed with diff items
            }
        }

        private fun hasComposingSpan(s: CharSequence): Boolean {
            return s is Spanned && s.getSpans(0, s.length, Any::class.java)
                .any { s.getSpanFlags(it) and SPAN_COMPOSING != 0 }
        }

        private fun getActionType(oldText: CharSequence?, newText: CharSequence?): ActionType {
            val oldLen = oldText?.length ?: 0
            val newLen = newText?.length ?: 0

            if (oldLen > newLen) return ActionType.DELETE
            if (newLen > oldLen) {
                return if (oldLen == 0) ActionType.INSERT else ActionType.PASTE
            }
            return ActionType.NOT_DEF
        }

        private fun addEdit(start: Int, oldText: CharSequence?, newText: CharSequence?) {
            d(TAG, "addEdit: start=$start, oldText='${oldText}', newText='${newText}'")
            val at = getActionType(oldText, newText)

            if (TextUtils.equals(oldText, newText)) {
                d(TAG, "addEdit: oldText equals newText, returning.")
                return
            }

            val newDiffOperation = DiffOperation(start, oldText ?: "", newText ?: "")

            // Batching logic for normal typing
            val currentEditItem = editHistory.current
            val batch = currentEditItem != null && getActionType(
                currentEditItem.changes.first().oldText, currentEditItem.changes.first().newText
            ) == at && at != ActionType.PASTE && // PASTE is typically a single, distinct action
                    System.currentTimeMillis() - lastActionTime < BATCH_TIME_THRESHOLD_MS

            if (!batch) {
                d(TAG, "addEdit: Not batching, adding new EditItem.")
                editHistory.add(EditItem(listOf(newDiffOperation)))
            } else {
                d(TAG, "addEdit: Batching with previous EditItem.")
                val newChanges = currentEditItem!!.changes.toMutableList()
                newChanges.add(newDiffOperation)
                editHistory.history[editHistory.position - 1] = EditItem(newChanges)
            }
            lastActionType = at
            lastActionTime = System.currentTimeMillis()
            stateChangeListener?.invoke()
        }
    }

    companion object {
        private const val BATCH_TIME_THRESHOLD_MS = 1000L
        private const val TAG = "TextUndoRedo"
        private fun d(tag: String, message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(tag, message)
            }
        }
    }
}