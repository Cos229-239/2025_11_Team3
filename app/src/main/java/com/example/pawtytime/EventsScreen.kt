package com.example.pawtytime

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import java.util.Calendar
import android.content.Intent

class EventsScreen : Fragment(R.layout.fragment_events_screen) {

    private lateinit var rvEvents: RecyclerView
    private lateinit var adapter: EventsAdapter

    private val items = mutableListOf<EventUi>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvEvents = view.findViewById(R.id.rvEvents)
        rvEvents.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventsAdapter(items)
        rvEvents.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.btnCreateEvent)
            .setOnClickListener {
                startActivity(
                    Intent(requireContext(), CreateEventActivity::class.java)
                )
            }

        showDemoEvents()
    }

    private fun showDemoEvents() {
        val now = Calendar.getInstance()

        val demo1Time = now.clone() as Calendar
        demo1Time.add(Calendar.DAY_OF_YEAR, 1)
        demo1Time.set(Calendar.HOUR_OF_DAY, 18)
        demo1Time.set(Calendar.MINUTE, 0)

        val demo2Time = now.clone() as Calendar
        demo2Time.add(Calendar.DAY_OF_YEAR, 3)
        demo2Time.set(Calendar.HOUR_OF_DAY, 11)
        demo2Time.set(Calendar.MINUTE, 30)

        val demoEvents = listOf(
            EventUi(
                id = "demo1",
                title = "Yappy Hour @ Tampa Bay Brewing Co",
                dateTime = Timestamp(demo1Time.time),
                addressLine = "1600 E 8th Ave",
                city = "Tampa",
                state = "FL",
                zip = "33605",
                imageUrl = null,
                goingCount = 5,
                interestedCount = 12
            ),
            EventUi(
                id = "demo2",
                title = "Pawtumn Festival",
                dateTime = Timestamp(demo2Time.time),
                addressLine = "123 Pawty Lane",
                city = "Tampa",
                state = "FL",
                zip = "33602",
                imageUrl = null,
                goingCount = 8,
                interestedCount = 20
            )
        )

        adapter.replaceAll(demoEvents)
    }
}