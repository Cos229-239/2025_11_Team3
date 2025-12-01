package com.example.pawtytime

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EventDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val tvDetailTitle: TextView = findViewById(R.id.tvDetailTitle)
        val tvDetailEventId: TextView = findViewById(R.id.tvDetailEventId)

        val eventId = intent.getStringExtra("eventId") ?: "(unknown)"

        tvDetailTitle.text = "Event Details"
        tvDetailEventId.text = "Event ID: $eventId"
    }
}