package com.example.pawtytime

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class EventsScreen : Fragment(R.layout.fragment_events_screen) {

    private val items = mutableListOf<EventUi>()

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rvEvents: RecyclerView
    private lateinit var adapter: EventsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvEvents = view.findViewById(R.id.rvEvents)
        rvEvents.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventsAdapter(items) { event ->
            val ctx = requireContext()
            val intent = android.content.Intent(ctx, EventDetailActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }

        rvEvents.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.btnCreateEvent)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(requireContext(), CreateEventActivity::class.java)
                )
            }

        loadEventsFromFirestore()
    }

    private fun loadEventsFromFirestore() {
        db.collection("events")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val dto = doc.toObject(EventDto::class.java)
                    dto?.toUi(doc.id)
                }

                items.clear()
                items.addAll(list)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("EventsScreen", "Failed to load events", e)
            }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}