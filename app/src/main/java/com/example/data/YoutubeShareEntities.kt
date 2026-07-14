package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "youtube_shares")
data class YoutubeShare(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val youtubeUrl: String,
    val title: String,
    val personalizedMessage: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "timestamp_notes",
    foreignKeys = [
        ForeignKey(
            entity = YoutubeShare::class,
            parentColumns = ["id"],
            childColumns = ["shareId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shareId"])]
)
data class TimestampNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shareId: Int,
    val timeSeconds: Int,
    val noteText: String
)

data class YoutubeShareWithNotes(
    @Embedded val share: YoutubeShare,
    @Relation(
        parentColumn = "id",
        entityColumn = "shareId"
    )
    val notes: List<TimestampNote>
)
