package jp.note15.textundoredo

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog


@RunWith(RobolectricTestRunner::class)
class TextUndoRedoTest {

    private lateinit var editText: EditText
    private lateinit var textUndoRedo: TextUndoRedo
    private lateinit var ic: InputConnection

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        val context = ApplicationProvider.getApplicationContext<Context>()
        editText = EditText(context)
        textUndoRedo = TextUndoRedo(editText)

        val info = EditorInfo()
        ic = editText.onCreateInputConnection(info)
    }

    @Test
    fun testEmptyToAThenUndoRedo() {
        // Initial state: empty
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // 1. Change from empty to "a"
        val editable = editText.editableText
        editable.append("a")

        assertEquals("a", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // 2. Undo the change
        textUndoRedo.undo()
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // 3. Redo the change
        textUndoRedo.redo()
        assertEquals("a", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }

    @Test
    fun testImeInputAiThenUndoRedo() {
        // Initial state: empty
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Simulate IME input: type "あい" which converts to "愛"
        ic.setComposingText("あ", 1)
        ic.setComposingText("あい", 2)
        // 「変換」ボタンによる変換
        ic.setComposingText("アイ", 2)
        ic.setComposingText("愛", 1)
        ic.commitText("愛", 1)

        assertEquals("愛", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Undo the change
        textUndoRedo.undo()
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // Redo the change
        textUndoRedo.redo()
        assertEquals("愛", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }

    @Test
    fun testImeInputAiUsingCandidateThenUndoRedo() {
        // Initial state: empty
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Simulate IME input: type "あい" which converts to "愛"
        ic.setComposingText("あ", 1)
        ic.setComposingText("あい", 2)
        // 変換候補から選択
        ic.commitText("愛", 1)

        assertEquals("愛", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Undo the change
        textUndoRedo.undo()
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // Redo the change
        textUndoRedo.redo()
        assertEquals("愛", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }

    @Test
    fun testWithPrefixImeInputAiUsingCandidateThenUndoRedo() {
        // Initial state: これが
        editText.setText("これが")
        textUndoRedo.clearHistory()
        val length = editText.text.length

        assertEquals("これが", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Simulate IME input: type "あい" which converts to "愛"
        ic.setSelection(length, length)
        ic.setComposingText("あ", length + 1)
        ic.setComposingText("あい", length + 2)
        // 変換候補から選択
        ic.commitText("愛", length + 1)

        assertEquals("これが愛", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Undo the change
        textUndoRedo.undo()
        assertEquals("これが", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // Redo the change
        textUndoRedo.redo()
        assertEquals("これが愛", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }

    @Test
    fun testImeInputBThenButUsingCandidateThenUndoRedo() {
        // Initial state: empty
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Simulate IME input: type "b"
        ic.setComposingText("b", 1)
        assertEquals("b", editText.text.toString())

        ic.finishComposingText()
        ic.setSelection(1, 1)
        ic.commitText("ut", 3)

        assertEquals("but", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Undo the change
        textUndoRedo.undo()
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // Redo the change
        textUndoRedo.redo()
        assertEquals("but", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }

    @Test
    fun testBatchThenUndoRedo() {
        // Initial state: empty
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        val editable = editText.editableText
        editable.append("13")
        editable.insert(1, "2")

        assertEquals("123", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Undo the change
        textUndoRedo.undo()
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // Redo the change
        textUndoRedo.redo()
        assertEquals("123", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }

    @Test
    fun testImeInputAndDeletionsUndoRedo() {
        // Initial state: empty
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)

        // Simulate typing "a" and then committing " lot" (consolidated as "a lot")
        ic.setComposingText("a", 1)
        assertEquals("a", editText.text.toString())
        ic.finishComposingText() // Commit "a"
        assertEquals("a", editText.text.toString()) // Text should be "a" after committing "a"
        ic.commitText(
            " lot",
            1
        ) // This will insert " lot" at the current cursor position (which is 1)
        assertEquals("a lot", editText.text.toString())

        // Delete 't' from "a lot" -> "a lo"
        editText.editableText.delete(4, 5) // Delete 't'
        assertEquals("a lo", editText.text.toString())

        // Simulate IME input replacing "lo" with "l" -> "a l"
        ic.setSelection(2, 4) // Select "lo"
        ic.setComposingText("l", 1) // Replace "lo" with "l"
        assertEquals("a l", editText.text.toString())

        // Simulate IME commit empty, effectively deleting "l" -> "a "
        ic.commitText("", 1) // Commit empty text, effectively deleting the composing text "l"
        assertEquals("a ", editText.text.toString())

        // Delete ' ' from "a " -> "a"
        editText.editableText.delete(1, 2)
        assertEquals("a", editText.text.toString())

        // Delete 'a' from "a" -> ""
        editText.editableText.delete(0, 1)
        assertEquals("", editText.text.toString())

        // Now, perform undo operations and verify the expected state
        // Undo 1: Should restore "a lot"
        textUndoRedo.undo()
        assertEquals("a lot", editText.text.toString())

        // Undo 2: Should restore ""
        textUndoRedo.undo()
        assertEquals("", editText.text.toString())
        assertEquals(false, textUndoRedo.canUndo)
        assertEquals(true, textUndoRedo.canRedo)

        // Redo 1: Should restore "a lot"
        textUndoRedo.redo()
        assertEquals("a lot", editText.text.toString())

        // Redo 2: Should restore ""
        textUndoRedo.redo()
        assertEquals("", editText.text.toString())
        assertEquals(true, textUndoRedo.canUndo)
        assertEquals(false, textUndoRedo.canRedo)
    }
}
