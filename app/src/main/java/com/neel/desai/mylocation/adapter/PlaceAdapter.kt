package com.neel.desai.mylocation.adapter


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.model.NearByData
import java.text.DecimalFormat


class PlaceAdapter(activity: Activity, data: List<NearByData>, IsBiker: Boolean) : RecyclerView.Adapter<PlaceAdapter.MyViewHolder>() {
    private val dataSet: List<NearByData>
    var activity: Activity
    var IsBiker: Boolean

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var img: ImageView
        var tv_name: TextView
        var tv_ratings: TextView
        var myRatingBar: RatingBar
        var tv_distance: TextView
        var img_call: ImageView
        var tv_address: TextView
        var tv_open: TextView

        var img_direction: ImageView

        init {
            img = itemView.findViewById<View>(R.id.img) as ImageView
            img_direction = itemView.findViewById<View>(R.id.img_direction) as ImageView
            tv_name = itemView.findViewById<View>(R.id.tv_name) as TextView
            tv_ratings = itemView.findViewById<View>(R.id.tv_ratings) as TextView
            myRatingBar = itemView.findViewById<View>(R.id.myRatingBar) as RatingBar
            tv_distance = itemView.findViewById<View>(R.id.tv_distance) as TextView
            img_call = itemView.findViewById<View>(R.id.img_call) as ImageView
            tv_address = itemView.findViewById<View>(R.id.tv_address) as TextView
            tv_open = itemView.findViewById<View>(R.id.tv_open) as TextView

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlaceAdapter.MyViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.raw_place_layout, parent, false)
        return PlaceAdapter.MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceAdapter.MyViewHolder, listPosition: Int) {
        if (!IsBiker) {

            Glide.with(activity).load(dataSet[listPosition].Url).error(R.drawable.img_placeholder).into(holder.img)
            if (dataSet[listPosition].Rating != null) {
                holder.tv_ratings.setText(dataSet[listPosition].Rating.toString() + "")
                holder.tv_ratings.setVisibility(View.VISIBLE)
            } else {
                holder.tv_ratings.setVisibility(View.GONE)
            }
            if (dataSet[listPosition].Rating != null) {
                holder.myRatingBar.setRating((dataSet[listPosition].Rating.toString() + "").toFloat())
                holder.myRatingBar.setVisibility(View.VISIBLE)
            } else {
                holder.myRatingBar.setRating(0.0.toFloat())
                holder.myRatingBar.setVisibility(View.GONE)
            }
            if (!dataSet[listPosition].IsOpen) {
                holder.tv_open.setText("Open")
                holder.tv_open.setTextColor(activity.resources.getColor(R.color.colorScannedQty))
            } else {
                holder.tv_open.setText("Closed")
                holder.tv_open.setTextColor(activity.resources.getColor(R.color.colorRed))
            }
        } else {
            holder.myRatingBar.setVisibility(View.GONE)
            holder.tv_open.setVisibility(View.GONE)
            holder.tv_ratings.setVisibility(View.GONE)
        }
        holder.tv_name.setText(dataSet[listPosition].Name)
        if (dataSet[listPosition].Address != null) {
            holder.tv_address.setVisibility(View.VISIBLE)
            holder.tv_address.setText(dataSet[listPosition].Address.toString() + "")
        } else {
            holder.tv_address.setVisibility(View.GONE)
        }
        val precision = DecimalFormat("0.00")
        holder.tv_distance.setText(precision.format(dataSet[listPosition].Distance / 1000).toString() + " KM")
        if (dataSet[listPosition].Contactno.equals("N/A")) {
            holder.img_call.setVisibility(View.GONE)
        } else {
            holder.img_call.setVisibility(View.VISIBLE)
        }
        holder.img_direction.setTag(listPosition)
        holder.img_direction.setOnClickListener(View.OnClickListener { v ->
            if (dataSet[(v.tag as Int)].Latitude !== 0.0 && dataSet[(v.tag as Int)].Longitude !== 0.0) {
                val stringBuilder = StringBuilder()
                stringBuilder.append("geo:")
                stringBuilder.append(dataSet[(v.tag as Int)].Latitude)
                stringBuilder.append(",")
                stringBuilder.append(dataSet[(v.tag as Int)].Longitude)
                stringBuilder.append("?q=")
                stringBuilder.append(dataSet[(v.tag as Int)].Name)
                val mapIntent = Intent("android.intent.action.VIEW", Uri.parse(stringBuilder.toString()))
                mapIntent.setPackage("com.google.android.apps.maps")
                activity.startActivity(mapIntent)
            }
        })
        holder.img_call.setTag(listPosition)
        holder.img_call.setOnClickListener(View.OnClickListener { v ->
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CALL_PHONE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.CALL_PHONE),
                    20121
                )

                // MY_PERMISSIONS_REQUEST_CALL_PHONE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            } else {
                //You already have permission
                try {
                    if (dataSet[(v.tag as Int)].Contactno != null && dataSet[(v.tag as Int)].Contactno != null) {
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + dataSet[(v.tag as Int)].Contactno))
                        activity.startActivity(intent)
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    init {
        dataSet = data
        this.activity = activity
        this.IsBiker = IsBiker
    }
}