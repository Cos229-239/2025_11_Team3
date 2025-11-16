package com.example.pawtytime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.location.Geocoder
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class MapScreen : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_map_screen, container, false)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        loadUserLocationAndCenterMap()

        addMapMarker(
            lat = 27.96,
            lng = -82.45,
            title = "Pawtumn Festival",
            iconRes = R.drawable.ic_pin_event
        )

        addMapMarker(
            lat = 27.94,
            lng = -82.46,
            title = "Local Pet Shop",
            iconRes = R.drawable.ic_pin_product
        )

        addMapMarker(
            lat = 27.92,
            lng = -82.44,
            title = "Mobile Grooming",
            iconRes = R.drawable.ic_pin_service
        )
    }

    private fun addMapMarker(
        lat: Double,
        lng: Double,
        title: String,
        iconRes: Int
    ) {
        val pos = LatLng(lat, lng)

        googleMap?.addMarker(
            MarkerOptions()
                .position(pos)
                .title(title)
                .icon(
                    BitmapHelper.vectorToBitmap(
                        requireContext(),
                        iconRes,
                        sizeDp = 32f
                    )
                )
                .anchor(0.5f, 1f)
        )
    }

    private fun loadUserLocationAndCenterMap() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val locationString = doc.getString("location")

                if (!locationString.isNullOrBlank()) {
                    geocodeAndCenter(locationString)
                }
            }
    }

    private fun geocodeAndCenter(locationString: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        try {
            val results = geocoder.getFromLocationName(locationString, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                val latLng = LatLng(loc.latitude, loc.longitude)

                googleMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 11f)
                )
            }
        } catch (_: Exception) { }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val zoomIn = view.findViewById<ImageButton>(R.id.btnZoomIn)
        val zoomOut = view.findViewById<ImageButton>(R.id.btnZoomOut)
        val filterBtn = view.findViewById<FloatingActionButton>(R.id.btnFilter)

        zoomIn.setOnClickListener {
            googleMap?.animateCamera(
                CameraUpdateFactory.zoomIn()
            )
        }

        zoomOut.setOnClickListener {
            googleMap?.animateCamera(
                CameraUpdateFactory.zoomOut()
            )
        }

        filterBtn.setOnClickListener {
            val dropdownView = layoutInflater.inflate(R.layout.filter_dropdown, null)
            val popupWindow = PopupWindow(
                dropdownView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.elevation = 10f


            popupWindow.showAsDropDown(filterBtn, -dropdownView.width / 2, 16)


            val typeRow = dropdownView.findViewById<LinearLayout>(R.id.type_options)
            val typeHidden = dropdownView.findViewById<LinearLayout>(R.id.hidden_type)
            val typeIcon = dropdownView.findViewById<ImageView>(R.id.type_icon)

            typeRow.setOnClickListener {
                val showing = typeHidden.visibility == View.VISIBLE
                typeHidden.visibility = if (showing) View.GONE else View.VISIBLE
                typeIcon.setImageResource(if (showing) R.drawable.add else R.drawable.minus)
            }


            val distRow = dropdownView.findViewById<LinearLayout>(R.id.distance_options)
            val distHidden = dropdownView.findViewById<LinearLayout>(R.id.hidden_distance)
            val distIcon = dropdownView.findViewById<ImageView>(R.id.distance_icon)

            distRow.setOnClickListener {
                val showing = distHidden.visibility == View.VISIBLE
                distHidden.visibility = if (showing) View.GONE else View.VISIBLE
                distIcon.setImageResource(if (showing) R.drawable.add else R.drawable.minus)
            }

            val searchRow = dropdownView.findViewById<LinearLayout>(R.id.search_options)
            val searchHidden = dropdownView.findViewById<LinearLayout>(R.id.hidden_search)
            val searchIcon = dropdownView.findViewById<ImageView>(R.id.search_icon)

            searchRow.setOnClickListener {
                val showing = searchHidden.visibility == View.VISIBLE
                searchHidden.visibility = if (showing) View.GONE else View.VISIBLE
                searchIcon.setImageResource(if (showing) R.drawable.add else R.drawable.minus)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}