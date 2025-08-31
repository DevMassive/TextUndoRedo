package jp.note15.textundoredo.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.note15.textundoredo.TextUndoRedo
import jp.note15.textundoredo.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var textUndoRedo: TextUndoRedo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        textUndoRedo = TextUndoRedo(binding.editText)

        binding.undoButton.setOnClickListener {
            textUndoRedo.undo()
        }

        binding.redoButton.setOnClickListener {
            textUndoRedo.redo()
        }

        textUndoRedo.stateChangeListener = {
            updateUndoRedoButtonStates()
        }

        // Initial state update
        updateUndoRedoButtonStates()
    }

    private fun updateUndoRedoButtonStates() {
        binding.undoButton.isEnabled = textUndoRedo.canUndo
        binding.redoButton.isEnabled = textUndoRedo.canRedo
    }
}