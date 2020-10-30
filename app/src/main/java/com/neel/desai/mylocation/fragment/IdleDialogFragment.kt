package com.neel.desai.mylocation.fragment

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.maps.SupportMapFragment
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.adapter.ReasonAdapter
import com.neel.desai.mylocation.database.DatabaseClient
import com.neel.desai.mylocation.databinding.DialogIdleBinding
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.model.ReasonData
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.IdelViewModel
import java.util.*

class IdleDialogFragment : DialogFragment() {

    lateinit var idelViewModel: IdelViewModel;


    var mMapFragment: SupportMapFragment? = null
    var alertDialog: AlertDialog? = null
    var _binding: DialogIdleBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = DialogIdleBinding.inflate(inflater, container, false)
        val view = _binding!!.root

        val myLocationData: MyLocationData? = arguments!!.getSerializable(Constants.INTENT_LOCATION_KEY) as MyLocationData?
        _binding!!.btnAddReason.setOnClickListener(View.OnClickListener { showDialogIdle(activity, "Add Idle Reason", myLocationData) })
        _binding!!.tvTime.setText(Utility.GetTimewithoutlable(Constants.IDLE_TIME.toLong()))

        dialog!!.setCanceledOnTouchOutside(false)
        dialog!!.setOnKeyListener { dialog, keyCode, event -> // Prevent dialog close on back press button
            keyCode == KeyEvent.KEYCODE_BACK
        }


        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        idelViewModel = ViewModelProviders.of(this).get(IdelViewModel::class.java)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dialog.window!!.attributes)
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            dialog.window!!.attributes = lp
        }
    }

    override fun onDestroyView() {
        _binding = null
        DismissIdle()
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialog)
    }


    fun showDialogIdle(activity: Activity?, msg: String?, myLocationData: MyLocationData?) {
        val inflater = layoutInflater
        val subView = inflater.inflate(R.layout.dialog_idle_reson, null)
        val strings: ArrayList<ReasonData> = Constants.idleReason
        val recyclerView = subView.findViewById<View>(R.id.my_recycler_view) as RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.itemAnimator = DefaultItemAnimator()
        val finalAdapter = ReasonAdapter(strings)
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
            btnPositive.setOnClickListener(View.OnClickListener {
                if (finalAdapter.getselected() != null) {

                    myLocationData?.reason = finalAdapter?.getselected()?.Reason!!
                    if (myLocationData != null) {
                        DatabaseClient.getInstance(activity).getAppDatabase().taskDao().update(myLocationData)
                    }
                    DismissIdle()
                    dismiss()

                } else {
                    Toast.makeText(getActivity(), "Please Select  Reason", Toast.LENGTH_LONG).show()
                }
            })
        }
        alertDialog!!.setCancelable(true)
        alertDialog!!.setCanceledOnTouchOutside(true)
        alertDialog!!.show()

    }

    fun DismissIdle() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
    }


}