package com.neel.desai.mylocation.adapter


import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.model.ReasonData


class NearByAdapter(data: List<ReasonData>, activity: Activity) : RecyclerView.Adapter<NearByAdapter.MyViewHolder>() {
    private val dataSet: List<ReasonData>
    var activity: Activity
    private var listener: OnItemClickListener? = null

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var chip: Chip

        init {
            chip = itemView.findViewById<View>(R.id.chip) as Chip
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.nearby_mychip, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, listPosition: Int) {
        Log.e("Adapter-------", Gson().toJson(dataSet[listPosition]))
        if (dataSet[listPosition].isSelected) {
            holder.chip.setTextColor(activity.resources.getColor(R.color.white))
        } else {
            holder.chip.setTextColor(activity.resources.getColor(R.color.colorBlue))
        }
        holder.chip.isChecked = dataSet[listPosition].isSelected
        holder.chip.setText(dataSet[listPosition].Reason)
        holder.chip.tag = listPosition
        holder.chip.setOnClickListener { v ->
            if (dataSet[(v.tag as Int)].isSelected) {
                dataSet[(v.tag as Int)].isSelected = false
            } else {
                dataSet[(v.tag as Int)]. isSelected = true
            }
            for (i in dataSet.indices) {
                if (i != v.tag as Int) {
                    dataSet[i].isSelected = false
                }
            }
            listener?.onItemClick(dataSet[(v.tag as Int)])
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun getselected(): ReasonData? {
        Log.i("Dataset", Gson().toJson(dataSet))
        for (i in dataSet.indices) {
            if (dataSet[i].isSelected) {
                return dataSet[i]
            }
        }
        return null
    }

    fun SetOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(item: ReasonData?)
    }

    init {
        dataSet = data
        this.activity = activity
    }
}