package com.neel.desai.mylocation.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.jaygoo.widget.OnRangeChangedListener
import com.jaygoo.widget.RangeSeekBar
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.adapter.NearByAdapter
import com.neel.desai.mylocation.adapter.PlaceAdapter
import com.neel.desai.mylocation.databinding.ActivityNearByBinding
import com.neel.desai.mylocation.fragment.IdleDialogFragment
import com.neel.desai.mylocation.fragment.RestDialogFragment
import com.neel.desai.mylocation.fragment.SosDialogFragment
import com.neel.desai.mylocation.fragment.SpeedDialogFragment
import com.neel.desai.mylocation.maputil.PlacesTask
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.model.NearByData
import com.neel.desai.mylocation.model.ReasonData
import com.neel.desai.mylocation.service.LocationService
import com.neel.desai.mylocation.util.CommonMethods
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.SharedPrefsUtils
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.NearByViewModel
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import java.util.*

class NearBy : BaseActivity(), BaseActivity.OnBrodcastListener, OnMapReadyCallback, OnMenuItemClickListener<PowerMenuItem> {

    lateinit var nearByViewModel: NearByViewModel
    lateinit var bindding: ActivityNearByBinding

    var pervDialog: DialogFragment? = null
    private var mMap: GoogleMap? = null
    var locationData: MyLocationData? = null
    var currentloction: LatLng? = null
    var mMarker: Marker? = null
    var zoomlevel = 15f
    var bitemap_Marker: Bitmap? = null
    var bitmapdescriptor: BitmapDescriptor? = null
    var mMarkerPlaceLink = HashMap<String, String>()

    var radius = 5000
    var powerMenu: PowerMenu? = null
    var behavior: BottomSheetBehavior<*>? = null
    var finalAdapter: NearByAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nearByViewModel = ViewModelProviders.of(this).get(NearByViewModel::class.java)
        bindding = DataBindingUtil.setContentView(this, R.layout.activity_near_by);
        bindding.data = nearByViewModel
        bindding.lifecycleOwner = this

        behavior = BottomSheetBehavior.from<View>(bindding.bottomSheet)


        (behavior as BottomSheetBehavior<View>).state = BottomSheetBehavior.STATE_COLLAPSED

        bindding.myRecyclerView.setHasFixedSize(true)
        bindding.myRecyclerView.setLayoutManager(LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false))
        bindding.myRecyclerView.setItemAnimator(DefaultItemAnimator())
        val strings: ArrayList<ReasonData> = Constants.getnearBy(applicationContext)
        finalAdapter = NearByAdapter(strings, this@NearBy)
        bindding.myRecyclerView.adapter = finalAdapter


        bindding.list.setOnClickListener(View.OnClickListener {
            (behavior as BottomSheetBehavior<View>).setState(BottomSheetBehavior.STATE_EXPANDED)
        })

        finalAdapter!!.SetOnItemClickListener(object : NearByAdapter.OnItemClickListener {
            override fun onItemClick(item: ReasonData?) {

                if (item!!.isSelected && !item.Reason.equals(getString(R.string.label_driver_friends))) {
                    var Item_key = ""
                    if (item.Reason.equals(getString(R.string.label_food))) {
                        Item_key = "restaurant"
                    } else if (item.Reason.equals(getString(R.string.label_fuel_nearby))) {
                        Item_key = "gas_station"
                    } else if (item.Reason.equals(getString(R.string.label_hospital))) {
                        Item_key = "hospital"
                    } else if (item.Reason.equals(getString(R.string.label_truck_service))) {
                        Item_key = "truck service"
                    }
                    val location: Location
                    val sb1: StringBuilder
                    var stringBuilder: StringBuilder
                    location = Utility.getLastKnownLoaction(true, applicationContext)
                    if (location != null) {
                        if (Utility.isNetworkAvailable(applicationContext)) {
                            if (Item_key != "truck service") {
                                sb1 = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
                                stringBuilder = StringBuilder()
                                stringBuilder.append("location=")
                                stringBuilder.append(location.latitude)
                                stringBuilder.append(",")
                                stringBuilder.append(location.longitude)
                                sb1.append(stringBuilder.toString())
                                sb1.append("&radius=$radius")
                                sb1.append("&types=$Item_key")
                                sb1.append("&sensor=true")
                                stringBuilder = StringBuilder()
                                stringBuilder.append("&key=")
                                stringBuilder.append(resources.getString(R.string.google_api_adddress))
                                sb1.append(stringBuilder.toString())
                            } else {
                                sb1 = StringBuilder("https://maps.googleapis.com/maps/api/place/textsearch/json?")
                                stringBuilder = StringBuilder()
                                stringBuilder.append("location=")
                                stringBuilder.append(location.latitude)
                                stringBuilder.append(",")
                                stringBuilder.append(location.longitude)
                                sb1.append(stringBuilder.toString())
                                sb1.append("&radius=$radius")
                                sb1.append("&query=truckservice dealers")
                                sb1.append("&sensor=true")
                                stringBuilder = StringBuilder()
                                stringBuilder.append("&key=")
                                stringBuilder.append(resources.getString(R.string.google_api_adddress))
                                sb1.append(stringBuilder.toString())
                            }
                            PlacesTask(item.Reason!!, mMap!!, this@NearBy, mMarkerPlaceLink).execute(*arrayOf(sb1.toString()))
                        } else {
                            Utility.alertDialogShow(this@NearBy, resources.getString(R.string.title_connection_alert), resources.getString(R.string.msg_try_with_active_network), false)
                        }
                    }
                } else if (item.isSelected && item.Reason.equals(getString(R.string.label_driver_friends))) {
                } else {
                    bindding.list.setVisibility(View.GONE)
                    mMap!!.clear()
                    onMapReady(mMap!!)
                }
            }


        })

        setOnBrodcastListener(this)
        (supportFragmentManager.findFragmentById(R.id.map_near_by) as SupportMapFragment?)!!.getMapAsync(this)

        bindding.toolbar.setTitle(R.string.title_nearBy)
        setSupportActionBar(bindding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        bindding.ivSos.setOnClickListener(View.OnClickListener {

            showDialog(SosDialogFragment())
        })

        bindding.ivCurrent.setOnClickListener(View.OnClickListener {
            val location = Utility.getLastKnownLoaction(true, applicationContext)
            if (location != null) {
                val currentLatLng = LatLng(
                    location.latitude,
                    location.longitude
                )
                SetZoomLevel(15f)
                val update = CameraUpdateFactory.newLatLngZoom(
                    currentLatLng,
                    zoomlevel
                )
                bindding.seekBar1.setValue(zoomlevel)
                mMap!!.moveCamera(update)
            }
        })
        dashbordDynamicSize()
        bindding.seekBar1.setOnRangeChangedListener(object : OnRangeChangedListener {
            override fun onRangeChanged(view: RangeSeekBar, leftValue: Float, rightValue: Float, isFromUser: Boolean) {
                SetZoomLevel(leftValue)
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(leftValue), 100, null)
            }

            override fun onStartTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {}
            override fun onStopTrackingTouch(view: RangeSeekBar, isLeft: Boolean) {}
        })


        powerMenu = PowerMenu.Builder(this)
            .addItem(PowerMenuItem("2 KM", false))
            .addItem(PowerMenuItem("5 KM", true))
            .addItem(PowerMenuItem("10 KM", false))
            .setAnimation(MenuAnimation.SHOWUP_TOP_LEFT) // Animation start point (TOP | LEFT)
            .setMenuRadius(10f)
            .setMenuShadow(10f)
            .setTextColor(resources.getColor(R.color.colorLabel))
            .setSelectedTextColor(Color.WHITE)
            .setMenuColor(Color.WHITE)
            .setSelectedMenuColor(resources.getColor(R.color.colorAccent))
            .setOnMenuItemClickListener(this)
            .build()
    }


    private fun SetZoomLevel(leftValue: Float) {
        zoomlevel = leftValue
    }

    fun dashbordDynamicSize() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val temp = Math.sqrt(xInches * xInches + yInches * yInches.toDouble())
        val diagonalInches = Math.round(temp * 2) / 2.0f.toDouble()
        if (diagonalInches >= 4 && diagonalInches < 4.5) {
            // icon size 60

            /*RelativeLayout.LayoutParams paramsSos = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            paramsSos.setMargins(0, 0, CommonMethods.dpToPx(10), CommonMethods.dpToPx(5));
            fl_Sos.setLayoutParams(paramsSos);*/
            val lp = bindding.flSos.getLayoutParams() as RelativeLayout.LayoutParams
            lp.setMargins(0, 0, CommonMethods.dpToPx(10), CommonMethods.dpToPx(5))
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            bindding.flSos.setLayoutParams(lp)
            val lp1 = bindding.ivCurrent.getLayoutParams() as RelativeLayout.LayoutParams
            lp1.setMargins(0, 0, CommonMethods.dpToPx(0), CommonMethods.dpToPx(0))
            bindding.ivCurrent.setLayoutParams(lp1)
            val lp2 = bindding.ivSos.getLayoutParams() as RelativeLayout.LayoutParams
            lp2.addRule(RelativeLayout.BELOW, R.id.iv_current)
            bindding.ivSos.setLayoutParams(lp2)
        } else if (diagonalInches >= 4.5 && diagonalInches < 5) {
            // icon size 70
        } else if (diagonalInches >= 5 && diagonalInches < 5.5) {
            // icon size 85
        } else if (diagonalInches >= 5.5 && diagonalInches < 6) {
// icon size 75
        } else if (diagonalInches >= 6 && diagonalInches < 6.5) {
            // icon size 75
        } else {
            // icon size 75
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun showSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun showDialog(msg: DialogFragment) {
        if (pervDialog != null) {
            pervDialog!!.dismiss()
        }
        if (pervDialog != null && pervDialog!!.fragmentManager != null && (pervDialog is RestDialogFragment || pervDialog is SosDialogFragment)) {
            val fragment = pervDialog!!.fragmentManager!!.findFragmentById(R.id.map_rest)
            val fragment1 = pervDialog!!.fragmentManager!!.findFragmentById(R.id.map_sos)
            if (fragment1 != null) {
                val ft = Objects.requireNonNull(this).supportFragmentManager.beginTransaction()
                ft.remove(fragment1)
                ft.commit()
            }
            if (fragment != null) {
                val ft = Objects.requireNonNull(this).supportFragmentManager.beginTransaction()
                ft.remove(fragment)
                ft.commit()
            }
        }
        pervDialog = msg


        //closeYourDialogFragment();
        //  MapDialogFragment dialogFragment = new MapDialogFragment();
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(Constants.DIALOG_FRGAMET_KEY)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        msg.show(supportFragmentManager, Constants.DIALOG_FRGAMET_KEY)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nearby_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        } else if (item.itemId == R.id.radius) {
            val menuItemView: View = findViewById(R.id.radius)
            powerMenu!!.showAsAnchorLeftBottom(menuItemView)
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onBackPressed() {
        if (behavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            behavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            finish()
        }
        // close this activity and return to preview activity (if there is any)
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


    override fun onReceive(intent: Intent?, Type: String?) {
        if (Type.equals("LocationChnage", ignoreCase = true)) {
            locationData = intent?.getSerializableExtra(Constants.INTENT_LOCATION_KEY) as MyLocationData
            val stringBuilder = java.lang.StringBuilder()
            stringBuilder.append(locationData!!.isaccuracy)
            stringBuilder.append(Gson().toJson(locationData))
            Log.i("Isaccuracy", stringBuilder.toString())
            currentloction = LatLng(locationData!!.latitude.toDouble(), locationData!!.longitude.toDouble())
            mMap!!.isTrafficEnabled = true
            mMap!!.isIndoorEnabled = true
            mMap!!.isBuildingsEnabled = true
            mMap!!.uiSettings.isCompassEnabled = false
            mMap!!.uiSettings.isMapToolbarEnabled = false
            mMap!!.mapType = 1
            mMap!!.uiSettings.isZoomControlsEnabled = false
            mMap!!.uiSettings.setAllGesturesEnabled(true)
            mMap!!.uiSettings.isZoomGesturesEnabled = true
            mMap!!.isMyLocationEnabled = false
            mMap!!.uiSettings.isMyLocationButtonEnabled = true
        } else if (Type.equals("ServiceStop", ignoreCase = true)) {
            if (!SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_ISUSERSTOPT_KEY, false)) {
                Log.e("TTT", "Start Again")
                if (Build.VERSION.SDK_INT < 26) {
                    startService(Intent(applicationContext, LocationService::class.java))
                } else {
                    startForegroundService(Intent(applicationContext, LocationService::class.java))
                }

            }
        } else if (Type.equals("RestReminder", ignoreCase = true)) {
            Log.e("REST_REMINDER_BRODCAST", "REST_REMINDER_BRODCAST")
            invalidateOptionsMenu()
            val restDialogFragment = RestDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.INTENT_LOCATION_KEY, intent?.getSerializableExtra(Constants.INTENT_LOCATION_KEY))
            restDialogFragment.arguments = bundle
            showDialog(restDialogFragment)
        } else if (Type.equals("OverSpeed", ignoreCase = true)) {
            Log.e("OVER_SPEED", "OVER_SPEED" + "  " + intent?.getStringExtra(Constants.DISTANCE_KEY))
            val speedDialogFragment = SpeedDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.DISTANCE_KEY, intent?.getStringExtra(Constants.DISTANCE_KEY))
            speedDialogFragment.setArguments(bundle)
            showDialog(speedDialogFragment)
        } else if (Type.equals("IdleReminder", ignoreCase = true)) {
            Log.e("IDLE_REMINDER_BRODCAST", "IDLE_REMINDER_BRODCAST")
            val idleDialogFragment = IdleDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.INTENT_LOCATION_KEY, intent?.getSerializableExtra(Constants.INTENT_LOCATION_KEY))
            idleDialogFragment.setArguments(bundle)
            showDialog(idleDialogFragment)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        zoomlevel = 15f
        bindding.seekBar1.setValue(zoomlevel)
        //   seekBar1.setMax((int) mMap.getMaxZoomLevel());
        mMap!!.setTrafficEnabled(true)
        mMap!!.setIndoorEnabled(true)
        mMap!!.setBuildingsEnabled(true)
        mMap!!.setMyLocationEnabled(false)
        mMap!!.getUiSettings().isMyLocationButtonEnabled = true
        mMap!!.getUiSettings().isCompassEnabled = false
        mMap!!.getUiSettings().isMapToolbarEnabled = false
        mMap!!.setMapType(1)
        mMap!!.getUiSettings().setAllGesturesEnabled(true)
        mMap!!.getUiSettings().isZoomGesturesEnabled = true
        val location = Utility.getLastKnownLoaction(true, applicationContext)
        if (location != null) {
            currentloction = LatLng(location.latitude, location.longitude)
            mMap!!.clear()
            mMarker = mMap!!.addMarker(MarkerOptions().position(currentloction!!).icon(bitmapdescriptor))
            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(currentloction))
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
        }
        mMap!!.setOnInfoWindowClickListener(OnInfoWindowClickListener { arg0 ->
            val location = Utility.getLastKnownLoaction(true, applicationContext)
            if (location != null) {
                val stringBuilder = java.lang.StringBuilder()
                stringBuilder.append("geo:")
                stringBuilder.append(location.latitude)
                stringBuilder.append(",")
                stringBuilder.append(location.longitude)
                stringBuilder.append("?q=")
                stringBuilder.append(mMarkerPlaceLink[arg0.id])
                val mapIntent = Intent("android.intent.action.VIEW", Uri.parse(stringBuilder.toString()))
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        })
    }


    fun setListData(listData: List<NearByData?>?) {
        Log.e("NearByData", Gson().toJson(listData))
        if (listData != null && listData.size > 0) {
            SetZoomLevel(15f)
            bindding.seekBar1.setValue(zoomlevel)



            Collections.sort(listData, Comparator { o1, o2 ->
                java.lang.Double.compare(o1!!.Distance, o2!!.Distance) // error
            })
            bindding.list.setVisibility(View.VISIBLE)
            val recyclerView = findViewById(R.id.my_recycler_view_bottom) as RecyclerView
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = GridLayoutManager(this, 1)
            recyclerView.itemAnimator = DefaultItemAnimator()
            val placeAdapter = PlaceAdapter(this@NearBy, listData as List<NearByData>, false)
            recyclerView.adapter = placeAdapter
        } else {
            bindding.list.setVisibility(View.GONE)
        }
    }

    override fun onItemClick(position: Int, item: PowerMenuItem?) {

        if (item!!.getTitle() == "2 KM") {
            radius = 2000
        } else if (item.getTitle() == "5 KM") {
            radius = 5000
        } else if (item.getTitle() == "10 KM") {
            radius = 10000
        }

        if (finalAdapter != null) {
            val reasonData = finalAdapter!!.getselected()
            if (reasonData != null) {
                if (reasonData.isSelected && !reasonData.Reason.equals("DRIVER FRIENDS")) {
                    var Item_key = ""
                    if (reasonData.Reason.equals("FOOD")) {
                        Item_key = "restaurant"
                    } else if (reasonData.Reason.equals("FUEL")) {
                        Item_key = "gas_station"
                    } else if (reasonData.Reason.equals("HOSPITAL")) {
                        Item_key = "hospital"
                    }
                    val location: Location?
                    val sb1: java.lang.StringBuilder
                    var stringBuilder: java.lang.StringBuilder
                    location = Utility.getLastKnownLoaction(true, applicationContext)
                    if (location != null) {
                        if (Utility.isNetworkAvailable(applicationContext)) {
                            if (Item_key != "truck service") {
                                sb1 = java.lang.StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
                                stringBuilder = java.lang.StringBuilder()
                                stringBuilder.append("location=")
                                stringBuilder.append(location.latitude)
                                stringBuilder.append(",")
                                stringBuilder.append(location.longitude)
                                sb1.append(stringBuilder.toString())
                                sb1.append("&radius=$radius")
                                sb1.append("&types=$Item_key")
                                sb1.append("&sensor=true")
                                stringBuilder = java.lang.StringBuilder()
                                stringBuilder.append("&key=")
                                stringBuilder.append(resources.getString(R.string.google_api_adddress))
                                sb1.append(stringBuilder.toString())
                            } else {
                                sb1 = java.lang.StringBuilder("https://maps.googleapis.com/maps/api/place/textsearch/json?")
                                stringBuilder = java.lang.StringBuilder()
                                stringBuilder.append("location=")
                                stringBuilder.append(location.latitude)
                                stringBuilder.append(",")
                                stringBuilder.append(location.longitude)
                                sb1.append(stringBuilder.toString())
                                sb1.append("&radius=$radius")
                                sb1.append("&query=truckservice dealers")
                                sb1.append("&sensor=true")
                                stringBuilder = java.lang.StringBuilder()
                                stringBuilder.append("&key=")
                                stringBuilder.append(resources.getString(R.string.google_api_adddress))
                                sb1.append(stringBuilder.toString())
                            }
                            PlacesTask(reasonData.Reason!!, mMap!!, this@NearBy, mMarkerPlaceLink).execute(*arrayOf(sb1.toString()))
                        } else {
                            Utility.alertDialogShow(this@NearBy, resources.getString(R.string.title_connection_alert), resources.getString(R.string.msg_try_with_active_network), false)
                        }
                    }
                } else if (reasonData.isSelected && reasonData.Reason.equals("DRIVER FRIENDS")) {
                } else {
                    bindding.list.setVisibility(View.GONE)
                    mMap!!.clear()
                    onMapReady(mMap!!)
                }
            }
        }
        powerMenu!!.selectedPosition = position // change selected item

        powerMenu!!.dismiss()
    }


}
