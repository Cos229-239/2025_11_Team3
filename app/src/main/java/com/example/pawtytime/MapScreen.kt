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

class MapScreen : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

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

        val tampa = LatLng(27.9506, -82.4572)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(tampa, 11f))

        map.addMarker(
            MarkerOptions()
                .position(LatLng(27.96, -82.45))
                .title("Pawtumn Festival")
        )

        map.addMarker(
            MarkerOptions()
                .position(LatLng(27.94, -82.46))
                .title("Local Pet Shop")
        )
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