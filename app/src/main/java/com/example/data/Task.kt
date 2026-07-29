package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RepeatMode(val title: String) {
    NONE("بدون تکرار"),
    WEEKLY("تکرار هفتگی"),
    MONTHLY("تکرار ماهانه"),
    YEARLY("تکرار سالانه")
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val targetDate: Long,
    val isCompleted: Boolean = false,
    val hasAlarm: Boolean = false,
    val repeatMode: String = RepeatMode.NONE.name
)
