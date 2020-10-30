package com.neel.desai.mylocation.fragment

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.activity.HomeActivity
import com.neel.desai.mylocation.adapter.ReasonAdapter
import com.neel.desai.mylocation.database.DatabaseClient
import com.neel.desai.mylocation.databinding.DialogRestBinding
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.model.ReasonData
import com.neel.desai.mylocation.util.AddressParserJson
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.SharedPrefsUtils
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.RestViewModel
import org.json.JSONObject
import java.io.IOException
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RestDialogFragment : DialogFragment(), OnMapReadyCallback {

    lateinit var restViewModel: RestViewModel

    private var data: List<MyLocationData>? = null


    var supportMapFragment: SupportMapFragment? = null
    var alertDialog: AlertDialog? = null

    var mMap: GoogleMap? = null
    lateinit var Bindding: DialogRestBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        Bindding = DialogRestBinding.inflate(inflater, container, false)
        val view = Bindding!!.root


        supportMapFragment = if (Build.VERSION.SDK_INT < 21) {
            activity?.supportFragmentManager?.findFragmentById(R.id.map_rest) as SupportMapFragment?
        } else {
            activity?.supportFragmentManager?.findFragmentById(R.id.map_rest) as SupportMapFragment?
        }
        supportMapFragment!!.getMapAsync(this)


        dialog!!.setCanceledOnTouchOutside(false)
        dialog!!.setOnKeyListener { dialog, keyCode, event -> // Prevent dialog close on back press button
            keyCode == KeyEvent.KEYCODE_BACK
        }





        val myLocationData = arguments!!.getSerializable(Constants.INTENT_LOCATION_KEY) as MyLocationData?


        val LastRestEnd: MyLocationData? = DatabaseClient.getInstance(activity!!).getAppDatabase()
            .taskDao().getLastRestEnd()

        if (LastRestEnd != null) {
            data = DatabaseClient.getInstance(activity!!).getAppDatabase()
                .taskDao().getAllAfterRestEnd(LastRestEnd.id)
            Collections.reverse(data)
            if (data != null && data?.size!! > 0) {
                var distanceLastRest = 0.0
                var timeLastRest: Long = 0
                val size: Int = data?.size!!
                val formatter: DateFormat = SimpleDateFormat("dd MMM yyyy,HH:mm")
                for (i in 0 until size) {
                    if (i + 1 < size) {
                        try {
                            timeLastRest += Utility.GetDate(
                                formatter.parse(data!!.get(i).time),
                                formatter.parse(data!!.get(i + 1).time)
                            )

                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                        distanceLastRest += Utility.getDistancBetweenTwoPoints(data?.get(i)?.latitude?.toDouble()!!, data!!.get(i).longitude.toDouble(), data!!.get(i + 1).latitude.toDouble(), data!!.get(i + 1).longitude.toDouble())
                    }
                }
                val precision = DecimalFormat("0.00")
                Bindding.tvDistance.setText(precision.format(distanceLastRest / 1000) + " kms")
                Bindding.tvTime.setText(Utility.GetTimewithoutlable(TimeUnit.SECONDS.toMillis(timeLastRest)).toString() + " hrs")
            }
        } else {
            data = DatabaseClient.getInstance(activity!!).getAppDatabase()
                .taskDao().getaccuracy("1")
            Collections.reverse(data)
            if (data != null && data?.size!! > 0) {
                var distancelast = 0.0
                var timeLast: Long = 0
                val size: Int = data?.size!!
                val formatter: DateFormat = SimpleDateFormat("dd MMM yyyy,HH:mm")
                for (i in 0 until size) {
                    if (i + 1 < size) {
                        try {
                            timeLast += Utility.GetDate(
                                formatter.parse(data!!.get(i).time),
                                formatter.parse(data!!.get(i + 1).time)
                            )

                        } catch (e: ParseException) {

                            e.printStackTrace()
                        }
                        distancelast += Utility.getDistancBetweenTwoPoints(data!!.get(i).latitude.toDouble(), data!!.get(i).longitude.toDouble(), data!!.get(i + 1).latitude.toDouble(), data!!.get(i + 1).longitude.toDouble())
                    }
                }
                val precision = DecimalFormat("0.00")
                Bindding.tvDistance.setText(precision.format(distancelast / 1000) + " kms")
                Bindding.tvTime.setText(Utility.GetTimewithoutlable(TimeUnit.SECONDS.toMillis(timeLast)).toString() + " hrs")
            }
        }
        Bindding.cancel.setOnClickListener { v: View? ->
            showDialogRest(activity, "Rest Cancel Reason", myLocationData)
        }
        Bindding.Rest.setOnClickListener { v: View? ->


            HomeActivity.IsRestStart = true
            SharedPrefsUtils.setBooleanPreference(activity!!, Constants.INTENT_IS_REST_START_KEY, true)
            if (activity is HomeActivity && activity as HomeActivity? != null) {
                (activity as HomeActivity?)!!.invalidateOptionsMenu()
            }
            val intent1 = Intent(Constants.BROADCAST_REST_ACTION)
            intent1.putExtra(Constants.INTENT_IS_REST_START_KEY, HomeActivity.IsRestStart)
            LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent1)
            (activity!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(Constants.REST_NOTIFICATION_ID)

            dismiss()


            val restOffDialogFragment = RestOffDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(Constants.INTENT_LOCATION_KEY, myLocationData)
            restOffDialogFragment.arguments = bundle
            restOffDialogFragment.show(activity!!.supportFragmentManager, "dialog")
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        restViewModel = ViewModelProviders.of(this).get(RestViewModel::class.java)
    }

    override fun onMapReady(p0: GoogleMap?) {


        mMap = p0
        drawPath(mMap!!)
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog!!)

        if (activity is HomeActivity && activity as HomeActivity? != null) (activity as HomeActivity?)?.SetData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CustomDialog)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false)
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            dialog.window!!.attributes = lp
        }
    }


    override fun onDestroy() {
        if (null != supportMapFragment)
            activity!!.supportFragmentManager.beginTransaction()
                .remove(supportMapFragment!!)
                .commitAllowingStateLoss()
        super.onDestroy()
    }


    private fun drawPath(map: GoogleMap) {
        val LastRestEnd: MyLocationData? = DatabaseClient.getInstance(activity!!).getAppDatabase()
            .taskDao().getLastRestEnd()
        if (LastRestEnd != null) {
            data = DatabaseClient.getInstance(activity!!).getAppDatabase()
                .taskDao().getAllAfterRestEnd(LastRestEnd.id)
        } else {
            data = DatabaseClient.getInstance(activity!!).getAppDatabase()
                .taskDao().getaccuracy("1")
        }
        val size: Int = data?.size!!

        if (data != null && size > 0) {
            //   Collections.reverse(data);
            val options = PolylineOptions().width(10f)
            if (data!!.get(0).address != null) {

                val LocationMarkerstart = mMap!!.addMarker(
                    MarkerOptions().position(LatLng(data?.get(0)?.latitude?.toDouble()!!, data?.get(0)?.longitude?.toDouble()!!))
                        .icon(BitmapDescriptorFactory.fromBitmap(Utility.getMarkerIconWithLabel(activity, data!!.get(0).address, 0f, R.drawable.ic_truck))).title(data!!.get(data!!.size - 1).speed))
            } else {

                myTask(activity, data!!.get(0), mMap!!, true).execute()
            }
            if (data?.get(size - 1)?.address != null) {

                val LocationMarkerend = mMap!!.addMarker(
                    MarkerOptions().position(LatLng(data!!.get(size - 1).latitude.toDouble(), data?.get(size - 1)?.longitude?.toDouble()!!))
                        .icon(BitmapDescriptorFactory.fromBitmap(Utility.getMarkerIconWithLabel(activity, data?.get(size - 1)?.address, 0f, R.drawable.ic_marker))).title(data?.get(0)!!.speed)
                )
            } else {

                myTask(activity, data?.get(size - 1)!!, mMap!!, false).execute()
            }
            for (i in 0 until size) {

                if (data?.get(i)?.status.equals(Constants.RUNNING)) {
                    options.color(ContextCompat.getColor(activity!!, R.color.polylineColor))
                    options.add(LatLng(data?.get(i)?.latitude?.toDouble()!!, data?.get(i)?.longitude?.toDouble()!!))
                    map.addPolyline(options)
                }

            }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(data?.get(0)?.latitude?.toDouble()!!, data?.get(0)?.longitude?.toDouble()!!), 10.0f))
        }
    }


    fun showDialogRest(activity: Activity?, msg: String?, myLocationData: MyLocationData?) {
        val inflater = layoutInflater
        val subView: View = inflater.inflate(R.layout.dialog_idle_reson, null)
        val strings: ArrayList<ReasonData> = Constants.restReason
        val recyclerView = subView.findViewById<View>(R.id.my_recycler_view) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.itemAnimator = DefaultItemAnimator()
        val finalAdapter: ReasonAdapter = ReasonAdapter(strings)
        recyclerView.adapter = finalAdapter
        val alertDialogBuilder = AlertDialog.Builder(
            activity!!,
            R.style.MyAlertDialogStyle
        )

        alertDialogBuilder.setPositiveButton(getString(R.string.action_ok), null)
        alertDialogBuilder.setView(subView)
        alertDialog = alertDialogBuilder.create()
        alertDialog!!.setOnShowListener {
            val btnPositive = alertDialog!!.getButton(AlertDialog.BUTTON_POSITIVE)
            btnPositive.setOnClickListener {
                if (finalAdapter.getselected() != null) {
                    val intent = Intent(Constants.BROADCAST_REST_ACTION_CANCEl)
                    intent.putExtra(Constants.IS_REST_CANCEL, true)
                    LocalBroadcastManager.getInstance(getActivity()!!).sendBroadcast(intent)
                    SharedPrefsUtils.setBooleanPreference(activity, Constants.IS_REST_REMINDER, false)
                    if (getActivity() is HomeActivity && getActivity() as HomeActivity? != null) {
                        (getActivity() as HomeActivity?)!!.invalidateOptionsMenu()
                    }
                    DismissRest()
                    dismiss()
                } else {
                    Toast.makeText(getActivity(), "Please Select  Reason", Toast.LENGTH_LONG).show()
                }
            }
        }
        alertDialog!!.setCancelable(true)
        alertDialog!!.setCanceledOnTouchOutside(true)
        alertDialog!!.show()

    }

    fun DismissRest() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
    }


    inner class myTask(var context: Context?, var myLocationData: MyLocationData, var mMap: GoogleMap, var isStart: Boolean) : AsyncTask<Void?, Void?, String?>() {


        override fun onPostExecute(result: String?) {
            //do stuff
            if (isStart) {
                val LocationMarkerstart = mMap.addMarker(
                    MarkerOptions().position(LatLng(myLocationData.longitude.toDouble(), myLocationData.longitude.toDouble()))
                        .icon(BitmapDescriptorFactory.fromBitmap(Utility.getMarkerIconWithLabel(context, result, 0f, R.drawable.ic_truck))).title(data?.get(data?.size?.minus(1)!!)?.speed)
                )
            } else {
                val LocationMarkerend = mMap.addMarker(
                    MarkerOptions().position(LatLng(myLocationData.latitude.toDouble(), myLocationData.longitude.toDouble()))
                        .icon(BitmapDescriptorFactory.fromBitmap(Utility.getMarkerIconWithLabel(context, result, 0f, R.drawable.ic_marker))).title(data?.get(0)?.speed)
                )
            }
        }

        override fun doInBackground(vararg params: Void?): String? {
            TODO("Not yet implemented")
            //do stuff
            var City: String? = ""
            var State: String? = ""
            val geocoder: Geocoder
            var addresses: List<Address>? = null
            geocoder = Geocoder(context, Locale.getDefault())
            try {
                addresses = geocoder.getFromLocation(myLocationData.latitude.toDouble(), myLocationData.latitude.toDouble(), 1) // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (addresses != null && addresses.size > 0 && addresses[0].locality != null && addresses[0].adminArea != null) {
                Log.i("SOSAddress", "From Local")
                Log.i("SOSAddress", addresses[0].locality)
                Log.i("SOSAddress", addresses[0].adminArea)
                City = addresses[0].locality
                State = addresses[0].adminArea
                return if (City == "" && State == "") {
                    myLocationData.address = "N/A"
                    DatabaseClient.getInstance(context!!).getAppDatabase().taskDao().update(myLocationData)
                    "N/A"
                } else {
                    myLocationData.address = "$City,$State"
                    DatabaseClient.getInstance(context!!).getAppDatabase().taskDao().update(myLocationData)
                    "$City,$State"
                }
            } else {
                Log.i("SOSAddress", "From API")
                try {
                    Log.i(
                        "SOSAddressURL", "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + myLocationData.latitude.toDouble() + ","
                                + myLocationData.latitude.toDouble() + "&sensor=true&key=" + context!!.resources.getString(R.string.google_api_adddress)
                    )
                    val jsonObj: JSONObject = AddressParserJson.getJSONfromURL(
                        "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + myLocationData.latitude.toDouble() + ","
                                + myLocationData.latitude.toDouble() + "&sensor=true&key=" + context!!.resources.getString(R.string.google_api_adddress)
                    )
                    val Status = jsonObj.getString("status")
                    if (Status.equals("OK", ignoreCase = true)) {
                        val Results = jsonObj.getJSONArray("results")
                        val zero = Results.getJSONObject(0)
                        val address_components = zero.getJSONArray("address_components")
                        for (i in 0 until address_components.length()) {
                            val zero2 = address_components.getJSONObject(i)
                            val long_name = zero2.getString("long_name")
                            val mtypes = zero2.getJSONArray("types")
                            val Type = mtypes.getString(0)
                            if (TextUtils.isEmpty(long_name) == false || long_name != null || long_name?.length!! > 0 || long_name !== "") {
                                if (Type.equals("administrative_area_level_2", ignoreCase = true)) {
                                    // Address2 = Address2 + long_name + ", ";
                                    City = long_name
                                    Log.i("SOSAddress", City)
                                } else if (Type.equals("administrative_area_level_1", ignoreCase = true)) {
                                    State = long_name
                                    Log.i("SOSAddress", long_name)
                                }
                            }
                        }
                    }
                    return if (City == "" && State == "") {
                        myLocationData.address = "N/A"
                        DatabaseClient.getInstance(context!!).getAppDatabase().taskDao().update(myLocationData)
                        "N/A"
                    } else {
                        myLocationData.address = "$City,$State"
                        DatabaseClient.getInstance(context!!).getAppDatabase().taskDao().update(myLocationData)
                        "$City,$State"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}