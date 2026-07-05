package com.example.service

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AppDatabase
import com.example.data.ClipboardItem
import com.example.ui.keyboard.KeyboardLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MinimalistInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@MinimalistInputMethodService.lifecycle)
            )
        }
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        composeView.setContent {
            KeyboardLayout(
                isSystemKeyboard = true,
                onKeyClick = { text ->
                    currentInputConnection?.commitText(text, 1)
                },
                onBackspace = {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                },
                onAction = {
                    val conn = currentInputConnection
                    if (conn != null) {
                        val editorInfo = currentInputEditorInfo
                        val actionId = editorInfo?.actionId ?: 0
                        if (actionId != 0) {
                            conn.performEditorAction(actionId)
                        } else {
                            val actionCode = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: 0
                            if (actionCode != EditorInfo.IME_ACTION_NONE && actionCode != EditorInfo.IME_ACTION_UNSPECIFIED) {
                                conn.performEditorAction(actionCode)
                            } else {
                                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                            }
                        }
                    }
                },
                onSpace = {
                    currentInputConnection?.commitText(" ", 1)
                }
            )
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Capture primary clip if available to build endless history automatically!
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(this@MinimalistInputMethodService)
                    if (!db.clipboardDao().exists(text)) {
                        db.clipboardDao().insert(ClipboardItem(text = text))
                    }
                }
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
