package com.neel.desai.mylocation.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.databinding.DialogSpeedBinding
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.SpeedViewModel
import java.text.DecimalFormat

class SpeedDialogFragment : DialogFragment() {

    private val data: List<MyLocationData>? = null
    var location: Location? = null

    lateinit var speedViewModel: SpeedViewModel;
    var _binding: DialogSpeedBinding? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = DialogSpeedBinding.inflate(inflater, container, false)
        val view = _binding!!.root
        val precision = DecimalFormat("0.00")

        _binding!!.tvDistance.setText(precision.format(arguments!!.getString(Constants.DISTANCE_KEY)!!.toDouble() / 1000) + " kms")
        _binding!!.tvTime.setText(Utility.GetTimeMMSSwithoutlable(Constants.OVER_SPEED_TIME.toLong()).toString() + " sec")

        _binding!!.close.setOnClickListener(View.OnClickListener { dismiss() })
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        speedViewModel = ViewModelProviders.of(this).get(SpeedViewModel::class.java)
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
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CustomDialog)
    }


}