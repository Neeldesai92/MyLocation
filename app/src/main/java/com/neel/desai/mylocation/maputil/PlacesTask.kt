package com.neel.desai.mylocation.maputil


import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.activity.NearBy
import com.neel.desai.mylocation.model.NearByData
import com.neel.desai.mylocation.model.mapmodel.MapData
import com.neel.desai.mylocation.util.Utility
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class PlacesTask(var type: String, var mMap: GoogleMap, var activity: Activity, var mMarkerPlaceLink: HashMap<String, String>) : AsyncTask<String?, Int?, String>() {
    var data = ""
    var bitemap_Marker: Bitmap
    var bitmapdescriptor: BitmapDescriptor
    protected override fun doInBackground(vararg url: String?): String {
        try {
            activity.runOnUiThread { Utility.showDialog(activity) }
            Log.e("URL", url[0])
            data = downloadUrl(url[0])
        } catch (e: Exception) {
            Log.d("Background Task", e.toString())
        }
        return data
    }

    override fun onPostExecute(result: String) {
        activity.runOnUiThread { Utility.Dismiss() }
        if (data == "") {
            Utility.alertDialogShow(activity, activity.resources.getString(R.string.error), activity.resources.getString(R.string.msg_error), false)
        } else {
            val mapData: MapData = Gson().fromJson(result, MapData::class.java)
            Log.e("MapData", Gson().toJson(mapData))
            var nearByData: MutableList<NearByData?>? = null
            if (mapData != null && mapData.results != null && mapData.results.size > 0) {
                mMap.clear()
                nearByData = ArrayList<NearByData?>()
                val size: Int = mapData.results.size
                val location: Location = Utility.getLastKnownLoaction(true, activity)
                for (i in 0 until size) {

                    var name: String? = mapData.results.get(i).name
                    var vicinity: String? = mapData.results.get(i).vicinity
                    var markerOptions = MarkerOptions()
                    markerOptions.title("$name : $vicinity" )
                    markerOptions.position(LatLng(mapData.results.get(i).geometry?.location?.lat!!, mapData.results.get(i).geometry?.location?.lng!!))


                    when (type) {
                        "HOSPITAL" -> {
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(activity.resources, R.drawable.hospital)))
                        }
                        "FUEL" -> {
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(activity.resources, R.drawable.fuel)))
                        }
                        "FOOD" -> {
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(activity.resources, R.drawable.food)))
                        }
                        "TRUCK SERVICE" -> {
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(activity.resources, R.drawable.ic_marker)))
                        }
                        "DRIVER FRIENDS" -> {
                        }
                    }
                    val m = mMap.addMarker(markerOptions)
                    val hashMap = mMarkerPlaceLink
                    val id = m.id
                    val stringBuilder2 = StringBuilder()
                    stringBuilder2.append(name)
                    stringBuilder2.append(" ")
                    stringBuilder2.append(vicinity)
                    hashMap[id] = stringBuilder2.toString()
                    var url = ""
                    if (mapData.results.get(i).photos != null) {
                        url = if ("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + mapData.results.get(i).photos != null) mapData.results.get(i).photos!!.get(0).photoReference.toString()
                        else "" + "&key=" + activity.resources.getString(R.string.google_api_adddress) + ""
                    }
                    if (location != null) {
                        if (mapData.results.get(i).photos != null) {
                            url = if ("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + mapData.results.get(i).photos != null)
                                mapData.results.get(i).photos!!.get(0).photoReference.toString()
                            else
                                "" + "&key=" + activity.resources.getString(R.string.google_api_adddress) + ""
                        }
                        nearByData.add(NearByData(mapData.results.get(i).geometry!!.location?.lat!!, mapData.results.get(i).geometry!!.location?.lng!!, mapData.results.get(i).name, mapData.results.get(i).rating, mapData.results.get(i).vicinity, if (mapData.results.get(i).openingHours != null) true else false, "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + url + "&key=" + activity.resources.getString(R.string.google_api_adddress), "N/A", Utility.getDistancBetweenTwoPoints(location.latitude, location.longitude, mapData.results.get(i).geometry!!.location?.lat!!, mapData.results.get(i).geometry!!.location?.lng!!)))
                    } else {
                        nearByData.add(NearByData(mapData.results.get(i).geometry!!.location?.lng!!, mapData.results.get(i).geometry!!.location?.lng!!, mapData.results.get(i).name, mapData.results.get(i).rating, mapData.results.get(i).vicinity, if (mapData.results.get(i).openingHours != null) true else false, "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + url + "&key=" + activity.resources.getString(R.string.google_api_adddress), "N/A", 0.0))
                    }
                }
            }
            val location: Location = Utility.getLastKnownLoaction(true, activity)
            if (location != null) {
                val currentloction = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(currentloction).icon(bitmapdescriptor))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentloction))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
            }
            if (activity as NearBy != null && activity as NearBy is NearBy) {
                (activity as NearBy).setListData(nearByData)
            }
        }
        //  new ParserTask(type, mMap, activity, mMarkerPlaceLink).execute(new String[]{result});
    }

    @Throws(IOException::class)
    private fun downloadUrl(strUrl: String?): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            urlConnection = URL(strUrl).openConnection() as HttpURLConnection
            urlConnection!!.connect()
            iStream = urlConnection.inputStream
            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()
            var line: String? = ""
            while (true) {
                val readLine = br.readLine()
                line = readLine
                if (readLine == null) {
                    break
                }
                sb.append(line)
            }
            data = sb.toString()
            br.close()
        } catch (e: Exception) {
            Log.d("Exception", e.toString())
            data = e.toString()
        } catch (th: Throwable) {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        iStream!!.close()
        urlConnection!!.disconnect()
        return data
    }

    init {
        bitemap_Marker = BitmapFactory.decodeResource(activity.resources, R.drawable.pin)
        bitmapdescriptor = BitmapDescriptorFactory.fromBitmap(bitemap_Marker)
    }
}