package io.github.byter11.kindlecast

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


sealed class ConversionStatus {
    object Idle : ConversionStatus()
    object Loading : ConversionStatus()
    data class Success(val file: File) : ConversionStatus()
    data class Error(val message: String) : ConversionStatus()
}

class PythonLogger(val onLogReceived: (String) -> Unit) {
    fun write(message: Any?) {
        val finalMessage = when (message) {
            null -> ""
            is String -> message
            is ByteArray -> String(message, Charsets.UTF_8)
            is IntArray -> {
                val bytes = ByteArray(message.size) { i -> message[i].toByte() }
                String(bytes, Charsets.UTF_8)
            }
            is Array<*> -> {
                val sb = StringBuilder()
                message.forEach { item ->
                    when (item) {
                        is Number -> sb.append(item.toInt().toChar())
                        is ByteArray -> sb.append(String(item, Charsets.UTF_8))
                        else -> sb.append(item?.toString() ?: "")
                    }
                }
                sb.toString()
            }

            else -> message.toString()
        }

        if (finalMessage.isNotEmpty()) {
            onLogReceived(finalMessage)
        }
    }

    fun flush() {}
}

class Converter(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow<ConversionStatus>(ConversionStatus.Idle)
    val status = _status.asStateFlow()

    private val _logs = MutableStateFlow("")
    val logs = _logs.asStateFlow()

    init {
        if (!Python.isStarted()) {
            var platform = AndroidPlatform(getApplication())
            platform.redirectStdioToLogcat()
            Python.start(platform)
        }

        Python.getInstance().getModule("main").callAttr("setup")
    }

    fun convertUriToAzw3(context: Context, uri: Uri) {
        viewModelScope.launch {
            val filename = DocumentFile.fromSingleUri(context, uri)?.name ?: "ebook_${UUID.randomUUID().toString().substring(0,15)}.epub"
            _status.value = ConversionStatus.Loading
            _logs.value = "Starting conversion for $filename...\n"

            withContext(Dispatchers.IO) {
                try {
                    val logger = PythonLogger { newLog ->
                        _logs.update { it + newLog }
                    }

                    val py = Python.getInstance()
                    val sys = py.getModule("sys")
                    sys.put("stdout", logger)

                    // File IO Logic
                    val cacheDir = context.cacheDir
                    val epubFile = File(cacheDir, filename)
                    context.contentResolver.openInputStream(uri)
                        ?.use { it.copyTo(epubFile.outputStream()) }

                    val azw3File = File(cacheDir, "${epubFile.nameWithoutExtension.take(128)}.azw3")

                    // Call Python
                    val container = py.getModule("calibre.ebooks.oeb.polish.container")
                    container.callAttr("epub_to_azw3", epubFile.absolutePath, azw3File.absolutePath)

                    _status.value = ConversionStatus.Success(azw3File)
                    _logs.update { it + "Conversion successful" }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _logs.value += e.localizedMessage
                    _status.value =
                        ConversionStatus.Error(e.localizedMessage ?: "Conversion failed")
                }
            }
        }
    }

    fun reset() {
        _status.value = ConversionStatus.Idle
        _logs.value = ""
    }
}