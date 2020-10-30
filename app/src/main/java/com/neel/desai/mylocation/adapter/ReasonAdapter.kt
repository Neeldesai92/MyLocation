package com.neel.desai.mylocation.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.model.ReasonData


open class ReasonAdapter(data: List<ReasonData>) : RecyclerView.Adapter<ReasonAdapter.MyViewHolder>() {
    private val dataSet: List<ReasonData>

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var chip: Chip

        init {
            chip = itemView.findViewById<View>(R.id.chip) as Chip
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReasonAdapter.MyViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.mychip, parent, false)
        return ReasonAdapter.MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReasonAdapter.MyViewHolder, listPosition: Int) {
        Log.e("Adapter-------", Gson().toJson(dataSet[listPosition]))
        holder.chip.setChecked(dataSet[listPosition].isSelected)
        holder.chip.setText(dataSet[listPosition].Reason)
        holder.chip.setTag(listPosition)
        holder.chip.setOnClickListener(View.OnClickListener { v ->
            dataSet[(v.tag as Int)].isSelected = !dataSet[(v.tag as Int)].isSelected
            for (i in dataSet.indices) {
                if (i != v.tag as Int) {
                    dataSet[i].isSelected = false
                }
            }
            notifyDataSetChanged()
        })
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

    init {
        dataSet = data
    }
}