package com.example.data

import kotlinx.coroutines.flow.Flow

class YoutubeShareRepository(private val dao: YoutubeShareDao) {
    val allSharesWithNotes: Flow<List<YoutubeShareWithNotes>> = dao.getAllSharesWithNotes()

    fun getShareWithNotesById(id: Int): Flow<YoutubeShareWithNotes?> {
        return dao.getShareWithNotesById(id)
    }

    suspend fun save(share: YoutubeShare, notes: List<TimestampNote>): Long {
        return dao.saveShareWithNotes(share, notes)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteShareById(id)
    }
}
