package com.example.pawtytime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

enum class PinType { EVENT, PRODUCT, SERVICE }

class MapScreen : Fragment(), OnMapReadyCallback {

    private data class MapItem(
        val id: String,
        val type: PinType,
        val position: LatLng,
        val title: String,
        val zip: String? = null,
        var marker: com.google.android.gms.maps.model.Marker? = null
    )

    private val allItems = mutableListOf<MapItem>()
    private var userHomeLatLng: LatLng? = null

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

        allItems.clear()
        loadEventPinsFromFirestore()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val zoomIn = view.findViewById<ImageButton>(R.id.btnZoomIn)
        val zoomOut = view.findViewById<ImageButton>(R.id.btnZoomOut)
        val filterBtn = view.findViewById<FloatingActionButton>(R.id.btnFilter)

        zoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        zoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        filterBtn.setOnClickListener {
            showFilterPopup(filterBtn)
        }
    }

    private fun showFilterPopup(anchor: View) {
            val dropdownView = layoutInflater.inflate(R.layout.filter_dropdown, null)
            val popupWindow = PopupWindow(
                dropdownView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.elevation = 10f
            popupWindow.showAsDropDown(anchor, -dropdownView.width / 2, 16)

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

            val cbEvent = dropdownView.findViewById<CheckBox>(R.id.cbTypeEvent)
            val cbProduct = dropdownView.findViewById<CheckBox>(R.id.cbTypeProduct)
            val cbService = dropdownView.findViewById<CheckBox>(R.id.cbTypeService)

            val cb10 = dropdownView.findViewById<CheckBox>(R.id.cbDist10)
            val cb25 = dropdownView.findViewById<CheckBox>(R.id.cbDist25)
            val cb50 = dropdownView.findViewById<CheckBox>(R.id.cbDist50)
            val cbCustomDist = dropdownView.findViewById<CheckBox>(R.id.cbDistCustom)
            val etCustomMiles = dropdownView.findViewById<EditText>(R.id.etCustomMiles)

            val cbSearchLocation = dropdownView.findViewById<CheckBox>(R.id.cbSearchLocation)
            val etZipCode = dropdownView.findViewById<EditText>(R.id.etZipCode)

            fun recomputeFilters() {
                val types = mutableSetOf<PinType>()
                if (cbEvent.isChecked) types += PinType.EVENT
                if (cbProduct.isChecked) types += PinType.PRODUCT
                if (cbService.isChecked) types += PinType.SERVICE

                val maxDist: Double? = when {
                    cb10.isChecked -> 10.0
                    cb25.isChecked -> 25.0
                    cb50.isChecked -> 50.0
                    cbCustomDist.isChecked ->
                        etCustomMiles.text.toString().trim().toDoubleOrNull()
                    else -> null
                }

                val zip: String? =
                    if (cbSearchLocation.isChecked)
                        etZipCode.text.toString().trim().takeIf { it.isNotEmpty() }
                    else null

                applyMapFilters(
                    activeTypes = types,
                    maxDistanceMiles = maxDist,
                    searchZip = zip)
            }

            val distanceChecks = listOf(cb10, cb25, cb50, cbCustomDist)

            fun setDistanceExclusive(selected: CheckBox) {
                distanceChecks.forEach { cb ->
                    if (cb != selected) cb.isChecked = false
                }
                etCustomMiles.isEnabled = cbCustomDist.isChecked
            }

            distanceChecks.forEach { cb ->
                cb.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) setDistanceExclusive(cb)
                    recomputeFilters()
                }
            }

            cbEvent.setOnCheckedChangeListener { _, _ -> recomputeFilters() }
            cbProduct.setOnCheckedChangeListener { _, _ -> recomputeFilters() }
            cbService.setOnCheckedChangeListener { _, _ -> recomputeFilters() }

            cbSearchLocation.setOnCheckedChangeListener { _, isChecked ->
                etZipCode.isEnabled = isChecked
                recomputeFilters()
            }

            etCustomMiles.addTextChangedListener { recomputeFilters() }
            etZipCode.addTextChangedListener { recomputeFilters() }
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

                userHomeLatLng = latLng
                googleMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 11f)
                )
            }
        } catch (_: Exception) { }
    }

    private fun bitmapFromVector(context: Context, resId: Int): BitmapDescriptor {
        val drawable = AppCompatResources.getDrawable(context, resId)!!

        val targetDp = 32
        val density = context.resources.displayMetrics.density
        val sizePx = (targetDp * density).toInt()

        val bitmap = Bitmap.createBitmap(
            sizePx,
            sizePx,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun distanceMiles(from: LatLng, to: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            from.latitude, from.longitude,
            to.latitude, to.longitude,
            results
        )
        val meters = results[0].toDouble()
        return meters / 1609.34
    }

    private fun centerOnZip(zip: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        try {
            val query = "$zip, USA"
            val results = geocoder.getFromLocationName(query, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                val latLng = LatLng(loc.latitude, loc.longitude)

                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 11f)
                )
            }
        } catch (_: Exception) {

        }
    }

    private fun applyMapFilters(
        activeTypes: Set<PinType>,
        maxDistanceMiles: Double?,
        searchZip: String?
    ) {
        val map = googleMap ?: return
        map.clear()

        val userCenter = userHomeLatLng

        allItems.forEach { item ->
            if (activeTypes.isNotEmpty() && item.type !in activeTypes) return@forEach

            if (maxDistanceMiles != null && userCenter != null) {
                val d = distanceMiles(userCenter, item.position)
                if (d > maxDistanceMiles) return@forEach
            }

            if (!searchZip.isNullOrBlank()) {
                val itemZip = item.zip?.trim()
                if (itemZip.isNullOrBlank() ||
                    !itemZip.equals(searchZip.trim(), ignoreCase = true)
                ) {
                    return@forEach
                }
            }

            val iconRes = when (item.type) {
                PinType.EVENT -> R.drawable.ic_pin_event
                PinType.PRODUCT -> R.drawable.ic_pin_product
                PinType.SERVICE -> R.drawable.ic_pin_service
            }

            val marker = map.addMarker(
                MarkerOptions()
                    .position(item.position)
                    .title(item.title)
                    .icon(bitmapFromVector(requireContext(), iconRes))
            )
            item.marker = marker
        }

        if (!searchZip.isNullOrBlank()) {
            centerOnZip(searchZip)
        }
    }

private fun loadEventPinsFromFirestore() {
    db.collection("events")
        .get()
        .addOnSuccessListener { snap ->
            allItems.clear()

            snap.documents.forEach { doc ->
                val dto = doc.toObject(EventDto::class.java) ?: return@forEach
                val ui = dto.toUi(doc.id)

                allItems.add(
                    MapItem(
                        id = ui.id,
                        type = PinType.EVENT,
                        position = LatLng(ui.lat, ui.lng),
                        title = ui.title,
                        zip = ui.zip
                    )
                )
            }

            applyMapFilters(
                activeTypes = emptySet(),
                maxDistanceMiles = null,
                searchZip = null
            )
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