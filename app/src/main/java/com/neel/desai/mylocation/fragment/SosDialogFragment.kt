package com.neel.desai.mylocation.fragment

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.databinding.DialogSosBinding
import com.neel.desai.mylocation.repo.SosFactory
import com.neel.desai.mylocation.util.AddressParserJson
import com.neel.desai.mylocation.util.SharedPreference
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.SosViewModel
import org.json.JSONObject
import java.io.IOException
import java.util.*

class SosDialogFragment : DialogFragment(), OnMapReadyCallback {

    lateinit var sosViewModel: SosViewModel;
    lateinit var sosFactory: SosFactory;
    var location: Location? = null
    var _binding: DialogSosBinding? = null
    var supportMapFragment: SupportMapFragment? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = DialogSosBinding.inflate(inflater, container, false)
        val view = _binding!!.root
        SharedPreference.getInstance(activity!!.applicationContext)
        _binding!!.tvDriver.setText(SharedPreference.getDecryptedStringValue(SharedPreference.PREF_APP_KEY_USER_NAME))
        supportMapFragment = if (Build.VERSION.SDK_INT < 21) {
            activity?.supportFragmentManager?.findFragmentById(R.id.map_sos) as SupportMapFragment?
        } else {
            activity?.supportFragmentManager?.findFragmentById(R.id.map_sos) as SupportMapFragment?
        }
        supportMapFragment!!.getMapAsync(this)


        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sosFactory = SosFactory(activity!!.applicationContext)
        sosViewModel = ViewModelProviders.of(this, sosFactory).get(SosViewModel::class.java)
    }

    override fun onMapReady(googlemap: GoogleMap?) {
        location = Utility.getLastKnownLoaction(true, activity)

        if (location != null) {
            myTask(activity!!, location!!.latitude, location!!.longitude).execute()
            val posisiabsen = LatLng(location!!.latitude, location!!.longitude) ////your lat lng
            val bitemap_Marker = BitmapFactory.decodeResource(resources, R.drawable.pin)
            val bitmapdescriptor = BitmapDescriptorFactory.fromBitmap(bitemap_Marker)
            googlemap?.addMarker(
                MarkerOptions().position(posisiabsen)
                    .icon(bitmapdescriptor)
            )
            googlemap?.moveCamera(CameraUpdateFactory.newLatLng(posisiabsen))
            googlemap?.setMyLocationEnabled(false)
            googlemap?.animateCamera(CameraUpdateFactory.zoomTo(8f), 2000, null)
        }
    }


    override fun onDestroyView() {
        _binding = null
        assert(fragmentManager != null)
        val fragment = fragmentManager!!.findFragmentById(R.id.map_sos)
        val ft = Objects.requireNonNull(activity)!!.supportFragmentManager.beginTransaction()
        ft.remove(fragment!!)
        super.onDestroyView()
        ft.commit()

    }


    inner class myTask(var context: Context, var latitude: Double, var longitude: Double) : AsyncTask<Void?, Void?, String?>() {


        override fun onPostExecute(result: String?) {
            //do stuff
            _binding?.tvLocation?.setText(result)
        }

        override fun doInBackground(vararg params: Void?): String? {
            //do stuff
            var City: String? = ""
            var State: String? = ""
            val geocoder: Geocoder
            var addresses: List<Address>? = null
            geocoder = Geocoder(context, Locale.getDefault())
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1) // Here 1 represent max location result to returned, by documents it recommended 1 to 5
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
                    "N/A"
                } else {
                    "$City,$State"
                }
            } else {
                Log.i("SOSAddress", "From API")
                try {

                    val jsonObj: JSONObject = AddressParserJson.getJSONfromURL(
                        "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&sensor=true&key=" + context.resources.getString(R.string.google_api_adddress)
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
                        "N/A"
                    } else {
                        "$City,$State"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null;
        }

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


}