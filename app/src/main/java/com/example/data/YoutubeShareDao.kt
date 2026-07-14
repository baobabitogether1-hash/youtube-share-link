package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface YoutubeShareDao {
    @Transaction
    @Query("SELECT * FROM youtube_shares ORDER BY createdAt DESC")
    fun getAllSharesWithNotes(): Flow<List<YoutubeShareWithNotes>>

    @Transaction
    @Query("SELECT * FROM youtube_shares WHERE id = :id")
    fun getShareWithNotesById(id: Int): Flow<YoutubeShareWithNotes?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShare(share: YoutubeShare): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<TimestampNote>)

    @Query("DELETE FROM timestamp_notes WHERE shareId = :shareId")
    suspend fun deleteNotesForShare(shareId: Int)

    @Query("DELETE FROM youtube_shares WHERE id = :id")
    suspend fun deleteShareById(id: Int)

    @Transaction
    suspend fun saveShareWithNotes(share: YoutubeShare, notes: List<TimestampNote>): Long {
        val shareId = if (share.id == 0) {
            insertShare(share)
        } else {
            insertShare(share)
            share.id.toLong()
        }
        
        deleteNotesForShare(shareId.toInt())
        val notesToInsert = notes.map { it.copy(shareId = shareId.toInt()) }
        insertNotes(notesToInsert)
        return shareId
    }
}
