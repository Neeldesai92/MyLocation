package com.neel.desai.mylocation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neel.desai.mylocation.databinding.RawDahbordMenuBinding
import com.neel.desai.mylocation.model.DashbordMenu

class DashbordMenuAdapter(private val menu: List<DashbordMenu>, var context: Context) :
    RecyclerView.Adapter<DashbordMenuAdapter.Myholder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Myholder {
        val itemBinding =
            RawDahbordMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Myholder(itemBinding)
    }

    override fun getItemCount(): Int = menu.size
    override fun onBindViewHolder(holder: Myholder, position: Int) {
        holder.itemBinding.item = menu[position]
    }

    inner class Myholder(val itemBinding: RawDahbordMenuBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

    }
}

