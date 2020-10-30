package com.neel.desai.mylocation.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.activity.HomeActivity
import com.neel.desai.mylocation.database.DatabaseClient
import com.neel.desai.mylocation.databinding.DialogRestOffBinding
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.util.Constants
import com.neel.desai.mylocation.util.SharedPrefsUtils
import com.neel.desai.mylocation.util.Utility
import com.neel.desai.mylocation.viewmodel.RestoffViewModel
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RestOffDialogFragment : DialogFragment() {


    var cTimerSnoozRest: CountDownTimer? = null
    var isStartSnoozRest = false
    var Time: Long = 0
    var rest_off: Button? = null

    var brakeTime: Long = 0


    lateinit var restoffViewModel: RestoffViewModel
    var _binding: DialogRestOffBinding? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        _binding = DialogRestOffBinding.inflate(inflater, container, false)
        val view = _binding!!.root
        dialog!!.setCanceledOnTouchOutside(false)
        dialog!!.setOnKeyListener { dialog, keyCode, event -> // Prevent dialog close on back press button
            keyCode == KeyEvent.KEYCODE_BACK
        }
        val dataRestStart: MyLocationData? = DatabaseClient.getInstance(activity!!).getAppDatabase().taskDao().getLastRestStart()
        val formatter: DateFormat = SimpleDateFormat("dd MMM yyyy,HH:mm")

        if (dataRestStart != null) {
            try {
                Time = TimeUnit.SECONDS.toMillis(Utility.GetDate(Date(), formatter.parse(dataRestStart.time)))
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            if (Time >= Constants.REST_TIME) {
                _binding!!.tvTime.setText("00:00")
            } else {

                startTimersnoozRest(Constants.REST_TIME+60 * 1000 - Time)
            }
        } else {

            startTimersnoozRest(Constants.REST_TIME.toLong())
        }

        _binding!!.restOff.setOnClickListener { v: View? ->

            SharedPrefsUtils.setBooleanPreference(activity!!, Constants.IS_REST_REMINDER, false)

            HomeActivity.IsRestStart = false
            SharedPrefsUtils.setBooleanPreference(activity!!, Constants.INTENT_IS_REST_START_KEY, false)
            if (activity is HomeActivity && activity as HomeActivity? != null) {
                (activity as HomeActivity?)?.invalidateOptionsMenu()
            }
            val intent = Intent(Constants.BROADCAST_REST_ACTION)
            intent.putExtra(Constants.INTENT_IS_REST_START_KEY, HomeActivity.IsRestStart)
            LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent)
            cancelTimersnoozRest()
            dismiss()
        }


        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        restoffViewModel = ViewModelProviders.of(this).get(RestoffViewModel::class.java)
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

    //start timer function for User is Overspeed
    fun startTimersnoozRest(Time: Long) {
        cTimerSnoozRest = object : CountDownTimer(Time, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                brakeTime = millisUntilFinished
                _binding?.tvTime?.setText(Utility.GetTimewithoutlable(millisUntilFinished))
                if ((Constants.REST_TIME - Constants.REST_TIME * Constants.REST_COMPLETE_BEFORETIME_PERCENT / 100).equals(millisUntilFinished)) {
                    _binding?.tvTime?.setTextColor(activity!!.resources.getColor(R.color.colorRed))
                }
            }

            override fun onFinish() {
                brakeTime = 0
                _binding?.tvTime?.setTextColor(activity!!.resources.getColor(R.color.colorLabel))
                _binding?.tvTime?.setText("00:00")

                //Neel
            }
        }
        isStartSnoozRest = true
        (cTimerSnoozRest as CountDownTimer).start()
    }


    //cancel timer for User is IDLE
    fun cancelTimersnoozRest() {
        if (cTimerSnoozRest != null) {
            brakeTime = 0
            cTimerSnoozRest!!.cancel()
        }
    }

}