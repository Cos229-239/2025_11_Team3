package com.example.pawtytime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText


class EventsScreen : Fragment(R.layout.fragment_events_screen) {

    private val items = mutableListOf<EventUi>()
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var rvEvents: RecyclerView
    private lateinit var adapter: EventsAdapter

    private lateinit var etSearchZip: TextInputEditText
    private lateinit var tvNoEvents: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvEvents = view.findViewById(R.id.rvEvents)
        tvNoEvents = view.findViewById(R.id.tvNoEvents)
        etSearchZip = view.findViewById(R.id.etSearchZip)

        rvEvents.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventsAdapter(items) { event ->
            val ctx = requireContext()
            val intent = android.content.Intent(ctx, EventDetailActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }

        rvEvents.adapter = adapter

        etSearchZip.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                val raw = etSearchZip.text?.toString()?.trim().orEmpty()
                val zipFilter = raw.ifBlank { null }
                loadEventsFromFirestore(zipFilter)
                true
            } else {
                false
            }
        }

        view.findViewById<FloatingActionButton>(R.id.btnCreateEvent)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(requireContext(), CreateEventActivity::class.java)
                )
            }

        loadEventsFromFirestore(null)
    }

    private fun updateEventsList(list: List<EventUi>) {
        items.clear()

        if (list.isEmpty()) {
            tvNoEvents.visibility = View.VISIBLE
            rvEvents.visibility = View.GONE
        } else {
            tvNoEvents.visibility = View.GONE
            rvEvents.visibility = View.VISIBLE
            items.addAll(list)
        }

        adapter.notifyDataSetChanged()
    }

    private fun loadEventsFromFirestore(zipFilter: String? = null) {
        db.collection("events")
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                val allEvents = snap.documents.mapNotNull { doc ->
                    val dto = doc.toObject(EventDto::class.java)
                    dto?.toUi(doc.id)
                }

                val filtered = if (zipFilter.isNullOrBlank()) {
                    allEvents
                } else {
                    allEvents.filter { it.zip == zipFilter }
                }

                updateEventsList(filtered)
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