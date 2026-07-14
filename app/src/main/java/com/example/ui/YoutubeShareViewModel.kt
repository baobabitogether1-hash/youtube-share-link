package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TimestampNote
import com.example.data.YoutubeShare
import com.example.data.YoutubeShareRepository
import com.example.data.YoutubeShareWithNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class YoutubeShareViewModel(private val repository: YoutubeShareRepository) : ViewModel() {

    val allShares: StateFlow<List<YoutubeShareWithNotes>> = repository.allSharesWithNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form States
    private val _youtubeUrl = MutableStateFlow("")
    val youtubeUrl = _youtubeUrl.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _personalizedMessage = MutableStateFlow("")
    val personalizedMessage = _personalizedMessage.asStateFlow()

    private val _editingNotes = MutableStateFlow<List<TimestampNote>>(emptyList())
    val editingNotes = _editingNotes.asStateFlow()

    // Single Note Input States
    private val _currentNoteTimeStr = MutableStateFlow("")
    val currentNoteTimeStr = _currentNoteTimeStr.asStateFlow()

    private val _currentNoteText = MutableStateFlow("")
    val currentNoteText = _currentNoteText.asStateFlow()

    // Form Status
    private val _isFormOpen = MutableStateFlow(false)
    val isFormOpen = _isFormOpen.asStateFlow()

    private val _editingShareId = MutableStateFlow<Int?>(null)
    val editingShareId = _editingShareId.asStateFlow()

    // View Details Status
    private val _selectedShare = MutableStateFlow<YoutubeShareWithNotes?>(null)
    val selectedShare = _selectedShare.asStateFlow()

    // Validation Status helper
    val isUrlValid: Boolean
        get() = extractYoutubeVideoId(_youtubeUrl.value) != null

    fun onYoutubeUrlChange(url: String) {
        _youtubeUrl.value = url
        val videoId = extractYoutubeVideoId(url)
        if (videoId != null && _title.value.isEmpty()) {
            _title.value = "YouTube Video ($videoId)"
        }
    }

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
    }

    fun onPersonalizedMessageChange(msg: String) {
        _personalizedMessage.value = msg
    }

    fun onCurrentNoteTimeChange(time: String) {
        _currentNoteTimeStr.value = time
    }

    fun onCurrentNoteTextChange(text: String) {
        _currentNoteText.value = text
    }

    fun addEditingNote(): Boolean {
        val timeStr = _currentNoteTimeStr.value
        val noteText = _currentNoteText.value.trim()
        if (noteText.isEmpty()) return false

        val seconds = parseDuration(timeStr) ?: return false

        val newNote = TimestampNote(
            shareId = _editingShareId.value ?: 0,
            timeSeconds = seconds,
            noteText = noteText
        )

        _editingNotes.value = (_editingNotes.value + newNote).sortedBy { it.timeSeconds }
        _currentNoteTimeStr.value = ""
        _currentNoteText.value = ""
        return true
    }

    fun removeEditingNote(note: TimestampNote) {
        _editingNotes.value = _editingNotes.value.filter { it != note }
    }

    fun openCreateForm(prefilledUrl: String? = null) {
        _editingShareId.value = null
        _youtubeUrl.value = prefilledUrl ?: ""
        _title.value = if (prefilledUrl != null) {
            val videoId = extractYoutubeVideoId(prefilledUrl)
            if (videoId != null) "YouTube Video ($videoId)" else ""
        } else ""
        _personalizedMessage.value = ""
        _editingNotes.value = emptyList()
        _currentNoteTimeStr.value = ""
        _currentNoteText.value = ""
        _isFormOpen.value = true
    }

    fun openEditForm(shareWithNotes: YoutubeShareWithNotes) {
        _editingShareId.value = shareWithNotes.share.id
        _youtubeUrl.value = shareWithNotes.share.youtubeUrl
        _title.value = shareWithNotes.share.title
        _personalizedMessage.value = shareWithNotes.share.personalizedMessage
        _editingNotes.value = shareWithNotes.notes.sortedBy { it.timeSeconds }
        _currentNoteTimeStr.value = ""
        _currentNoteText.value = ""
        _isFormOpen.value = true
    }

    fun closeForm() {
        _isFormOpen.value = false
    }

    fun selectShare(share: YoutubeShareWithNotes?) {
        _selectedShare.value = share
    }

    fun saveShare(onCompleted: () -> Unit = {}) {
        val url = _youtubeUrl.value.trim()
        if (url.isEmpty()) return

        val finalTitle = _title.value.trim().ifEmpty {
            val videoId = extractYoutubeVideoId(url)
            if (videoId != null) "YouTube Video ($videoId)" else "YouTube Video"
        }

        val share = YoutubeShare(
            id = _editingShareId.value ?: 0,
            youtubeUrl = url,
            title = finalTitle,
            personalizedMessage = _personalizedMessage.value
        )

        viewModelScope.launch {
            repository.save(share, _editingNotes.value)
            _isFormOpen.value = false
            onCompleted()
        }
    }

    fun deleteShare(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_selectedShare.value?.share?.id == id) {
                _selectedShare.value = null
            }
        }
    }

    // Helper functions
    companion object {
        fun extractYoutubeVideoId(url: String): String? {
            val cleanedUrl = url.trim()
            val regex = "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?\\S*v=|embed\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})".toRegex()
            return regex.find(cleanedUrl)?.groupValues?.getOrNull(1)
        }

        fun formatSeconds(seconds: Int): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) {
                String.format("%02d:%02d:%02d", h, m, s)
            } else {
                String.format("%02d:%02d", m, s)
            }
        }

        fun parseDuration(durationStr: String): Int? {
            val trimmed = durationStr.trim()
            if (trimmed.isEmpty()) return null

            // Try raw seconds
            val rawSeconds = trimmed.toIntOrNull()
            if (rawSeconds != null && rawSeconds >= 0) return rawSeconds

            // Try MM:SS or HH:MM:SS
            val parts = trimmed.split(":")
            if (parts.size in 2..3) {
                try {
                    var hrs = 0
                    var mins = 0
                    var secs = 0
                    if (parts.size == 3) {
                        hrs = parts[0].toInt()
                        mins = parts[1].toInt()
                        secs = parts[2].toInt()
                    } else {
                        mins = parts[0].toInt()
                        secs = parts[1].toInt()
                    }
                    if (mins in 0..59 && secs in 0..59 && hrs >= 0) {
                        return hrs * 3600 + mins * 60 + secs
                    }
                } catch (e: NumberFormatException) {
                    // ignore
                }
            }
            return null
        }

        fun compileShareText(share: YoutubeShare, notes: List<TimestampNote>): String {
            val sb = StringBuilder()
            if (share.title.isNotEmpty()) {
                sb.append("🎥 ").append(share.title).append("\n\n")
            }
            if (share.personalizedMessage.isNotEmpty()) {
                sb.append(share.personalizedMessage).append("\n\n")
            }

            sb.append("Watch video: ").append(share.youtubeUrl).append("\n\n")

            if (notes.isNotEmpty()) {
                sb.append("⏱️ Timestamped Notes:\n")
                val sortedNotes = notes.sortedBy { it.timeSeconds }
                val videoId = extractYoutubeVideoId(share.youtubeUrl)

                for (note in sortedNotes) {
                    val formattedTime = formatSeconds(note.timeSeconds)
                    sb.append("- ").append(formattedTime).append(": ").append(note.noteText)
                    if (videoId != null) {
                        sb.append(" (https://youtu.be/").append(videoId).append("?t=").append(note.timeSeconds).append(")")
                    }
                    sb.append("\n")
                }
            }
            return sb.toString().trim()
        }

        fun shareContent(context: Context, share: YoutubeShare, notes: List<TimestampNote>) {
            val compiledText = compileShareText(share, notes)
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, compiledText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Video Notes")
            context.startActivity(shareIntent)
        }
    }
}

class YoutubeShareViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoutubeShareViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = YoutubeShareRepository(database.youtubeShareDao())
            @Suppress("UNCHECKED_CAST")
            return YoutubeShareViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
