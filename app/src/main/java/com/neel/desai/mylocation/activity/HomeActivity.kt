package com.neel.desai.mylocation.activity

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.neel.desai.mylocation.R
import com.neel.desai.mylocation.adapter.DashbordMenuAdapter
import com.neel.desai.mylocation.database.DatabaseClient
import com.neel.desai.mylocation.databinding.ActivityHomeBinding
import com.neel.desai.mylocation.databinding.NavDrawerHeaderBinding
import com.neel.desai.mylocation.fragment.*
import com.neel.desai.mylocation.model.DashbordMenu
import com.neel.desai.mylocation.model.MyLocationData
import com.neel.desai.mylocation.service.LocationService
import com.neel.desai.mylocation.util.*
import com.neel.desai.mylocation.viewmodel.HomeViewModel
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class HomeActivity : BaseActivity(), BaseActivity.OnBrodcastListener {
    lateinit var HomeviewModel: HomeViewModel
    lateinit var HomeBinding: ActivityHomeBinding
    lateinit var navigationViewHeaderBinding: NavDrawerHeaderBinding
    private var Dashbordmenuadapter: DashbordMenuAdapter? = null
    private var Dashbordmenudata: MutableList<DashbordMenu>? = mutableListOf()
    var IsRestStart = false
    private var Alertdialog: androidx.appcompat.app.AlertDialog? = null
    var isService = false
    private var data: List<MyLocationData>? = listOf()
    var timer: Timer? = null
    var locationData: MyLocationData? = null
    var locationDataNew: MyLocationData? = null
    var toggle: ActionBarDrawerToggle? = null
    var pervDialog: DialogFragment? = null

    companion object {
        var IsRestStart = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnBrodcastListener(this)
        HomeviewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)

        HomeBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        HomeBinding.homeModel = HomeviewModel
        HomeBinding.lifecycleOwner = this

        val headerView: View = HomeBinding.navView.getHeaderView(0)
        navigationViewHeaderBinding = NavDrawerHeaderBinding.bind(headerView)
        SharedPreference.getInstance(applicationContext)
        navigationViewHeaderBinding.tvUserName.text =
            SharedPreference.getDecryptedStringValue(SharedPreference.PREF_APP_KEY_USER_NAME)

        setRepeatingAsyncTask()
        ManageNotification(intent)
        if (Utility.isMyServiceRunning(LocationService::class.java, applicationContext)) {
            isService = true
            navigationViewHeaderBinding.trip.setChecked(true)
        } else {
            isService = false
            navigationViewHeaderBinding.trip.setChecked(false)
            SharedPrefsUtils.setBooleanPreference(
                applicationContext,
                Constants.IS_REST_REMINDER,
                false
            )
            SharedPrefsUtils.setBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                false
            )
            invalidateOptionsMenu()
        }

        if (SharedPrefsUtils.getBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                false
            )
        ) {
            //Show rest off diloag
            /* val restOffDialogFragment = RestOffDialogFragment()
             val bundle = Bundle()
             bundle.putSerializable(
                 Constants.INTENT_LOCATION_KEY,
                 intent.getSerializableExtra(Constants.INTENT_LOCATION_KEY) as MyLocationData
             )
             restOffDialogFragment.setArguments(bundle)
             showDialog(restOffDialogFragment)*/
        }


        navigationViewHeaderBinding.trip.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                val builder =
                    androidx.appcompat.app.AlertDialog.Builder(this)
                builder.setTitle("Alert" as CharSequence)
                builder.setMessage("Are you Sure You want to stop trip ?" as CharSequence)
                builder.setCancelable(false)
                builder.setPositiveButton("YES" as CharSequence) { _, arg1 ->
                    cancelTimer()
                    stopService(
                        Intent(
                            applicationContext,
                            LocationService::class.java
                        )
                    )

                    HomeBinding.appbar.tvCurrentvalue.setText("0")
                    SharedPrefsUtils.setBooleanPreference(
                        applicationContext,
                        Constants.INTENT_ISUSERSTOPT_KEY,
                        true
                    )
                    if (SharedPrefsUtils.getBooleanPreference(
                            applicationContext,
                            Constants.INTENT_IS_REST_START_KEY,
                            false
                        )
                    ) {
                        IsRestStart = false
                        SharedPrefsUtils.setBooleanPreference(
                            applicationContext,
                            Constants.INTENT_IS_REST_START_KEY,
                            IsRestStart
                        )
                        val intent = Intent(Constants.BROADCAST_REST_ACTION)
                        intent.putExtra(Constants.INTENT_IS_REST_START_KEY, IsRestStart)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    }
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
                    Alertdialog!!.dismiss()
                }
                builder.setNegativeButton(
                    "NO" as CharSequence
                ) { arg0, arg1 ->
                    isService = true
                    navigationViewHeaderBinding.trip.setChecked(true)
                    Alertdialog!!.dismiss()
                }
                Alertdialog = builder.create()
                if (!Alertdialog!!.isShowing()) {
                    Alertdialog!!.show()
                }
            } else {
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_OUT_NOTIFFY, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN, false)
                SharedPrefsUtils.setBooleanPreference(applicationContext, Constants.IS_GEOFANCE_IN_NOTIFFY, false)
                if (!Utility.isMyServiceRunning(LocationService::class.java, applicationContext)) {
                    Utility.setOverspeedData(applicationContext, "0.0", "0.0", "0")
                    setRepeatingAsyncTask()
                    SharedPrefsUtils.setDoublePreference(
                        applicationContext,
                        Constants.INTENT_DISTANCE_DIFFT_KEY,
                        0.0
                    )
                    HomeBinding.appbar.tvRemainvalue.setText("0")
                    SharedPrefsUtils.setDoublePreference(
                        applicationContext,
                        Constants.INTENT_DISTANCE_REMAING_KEY,
                        0.0
                    )
                    AddData()

                    /* data = */


                    data = DatabaseClient.getInstance(applicationContext).getAppDatabase()
                        .taskDao().GetAll()

                    if (data != null)
                        DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()
                            .deleteAll(data)
                    /* LiveAlldata.observeForever { result ->
                            data= result as MutableList<MyLocationData>?

                             if (data != null)
                                 DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()
                                     .deleteAll(data)
                         LiveAlldata.removeObserver { this }

                        }*/

                    /*  data = DatabaseClient.getInstance(applicationContext).getAppDatabase()
                              .taskDao().all()!!.value*/
                    SharedPrefsUtils.setStringPreference(
                        applicationContext,
                        Constants.INTENTDIFF_DATA_KEY,
                        ""
                    )
                    if (Build.VERSION.SDK_INT < 26) {

                        startService(Intent(applicationContext, LocationService::class.java))

                    } else {
                        startForegroundService(
                            Intent(
                                applicationContext,
                                LocationService::class.java
                            )
                        )
                    }
                    Toast.makeText(
                        applicationContext,
                        "Trip is Started drive safely.",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }


        }


        toggle = ActionBarDrawerToggle(
            this,
            HomeBinding.drawerLayout,
            HomeBinding.appbar.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        HomeBinding.drawerLayout.addDrawerListener(toggle!!)
        toggle!!.syncState()



        HomeBinding.appbar.toolbar.title = "DriveXpress"
        setSupportActionBar(HomeBinding.appbar.toolbar)
        dashbordDynamicSize()
        AddModuleList()
        AddData()

        HomeBinding.appbar.left.setOnClickListener { v: View? ->
            if (HomeBinding.appbar.layoutThree.visibility == View.VISIBLE) {
                HomeBinding.appbar.layoutThree.visibility = View.GONE
                HomeBinding.appbar.layoutOne.visibility = View.GONE
                HomeBinding.appbar.layoutTwo.visibility = View.VISIBLE
            } else if (HomeBinding.appbar.layoutTwo.visibility == View.VISIBLE) {
                HomeBinding.appbar.layoutThree.visibility = View.GONE
                HomeBinding.appbar.layoutOne.visibility = View.VISIBLE
                HomeBinding.appbar.layoutTwo.visibility = View.GONE
            }
        }

        HomeBinding.appbar.right.setOnClickListener { v: View? ->

            if (HomeBinding.appbar.layoutOne.visibility == View.VISIBLE) {
                HomeBinding.appbar.layoutThree.visibility = View.GONE
                HomeBinding.appbar.layoutOne.visibility = View.GONE
                HomeBinding.appbar.layoutTwo.visibility = View.VISIBLE
            } else if (HomeBinding.appbar.layoutTwo.visibility == View.VISIBLE) {
                HomeBinding.appbar.layoutThree.visibility = View.VISIBLE
                HomeBinding.appbar.layoutOne.visibility = View.GONE
                HomeBinding.appbar.layoutTwo.visibility = View.GONE
            }

        }


        HomeBinding.appbar.ivSos.setOnClickListener { v: View? ->
            HomeBinding.appbar.ivSos.setEnabled(false)
            showDialog(SosDialogFragment())
        }


        /* DatabaseClient.getInstance(applicationContext).getAppDatabase()
              .taskDao().getLastRecordNew().observeForever { result ->
                  if (result != null) {
                      if (result.activity.equals("LocationChnage", ignoreCase = true)) {
                          locationData =
                              intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY) as MyLocationData
                          val stringBuilder = java.lang.StringBuilder()
                          stringBuilder.append(locationData!!.isaccuracy)
                          stringBuilder.append(Gson().toJson(locationData))

                          if (locationData!!.isaccuracy === 1) {
                              HomeBinding.appbar.tvCurrentvalue.setText(locationData!!.speed)
                          }
                          CalculateDistance()
                      } else if (result.activity.equals("ServiceStop", ignoreCase = true)) {
                          if (!SharedPrefsUtils.getBooleanPreference(
                                  applicationContext,
                                  Constants.INTENT_ISUSERSTOPT_KEY,
                                  false
                              )
                          ) {
                              Log.e("TTT", "Start Again")
                              if (Build.VERSION.SDK_INT < 26) {
                                  startService(
                                      Intent(
                                          applicationContext,
                                          LocationService::class.java
                                      )
                                  )
                              } else {
                                  startForegroundService(
                                      Intent(
                                          applicationContext,
                                          LocationService::class.java
                                      )
                                  )
                              }

                          }
                      } else if (result.activity.equals("RestReminder", ignoreCase = true)) {
                          Log.e("REST_REMINDER_BRODCAST", "REST_REMINDER_BRODCAST")
                          invalidateOptionsMenu()
                          *//* val restDialogFragment = RestDialogFragment()
                     val bundle = Bundle()
                     bundle.putSerializable(
                         Constants.INTENT_LOCATION_KEY,
                         intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY)
                     )
                     restDialogFragment.setArguments(bundle)
                     showDialog(restDialogFragment)*//*
                    } else if (result.activity.equals("OverSpeed", ignoreCase = true)) {
                        *//*  Log.e(
                          "OVER_SPEED",
                          "OVER_SPEED" + "  " + intent!!.getStringExtra(Constants.DISTANCE_KEY)
                      )
                      val speedDialogFragment = SpeedDialogFragment()
                      val bundle = Bundle()
                      bundle.putSerializable(
                          Constants.DISTANCE_KEY,
                          intent!!.getStringExtra(Constants.DISTANCE_KEY)
                      )
                      speedDialogFragment.setArguments(bundle)
                      showDialog(speedDialogFragment)*//*
                    } else if (result.activity.equals("IdleReminder", ignoreCase = true)) {
                        *//* Log.e("IDLE_REMINDER_BRODCAST", "IDLE_REMINDER_BRODCAST")
                     val idleDialogFragment = IdleDialogFragment()
                     val bundle = Bundle()
                     bundle.putSerializable(
                         Constants.INTENT_LOCATION_KEY,
                         intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY)
                     )
                     idleDialogFragment.setArguments(bundle)
                     showDialog(idleDialogFragment)*//*
                    }
                }
            }*/

    }


    override fun onBackPressed() {


        if (HomeBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            HomeBinding.drawerLayout.closeDrawers()

        } else {
            showAlertForLogout(true)
        }

    }


    /**
     * LogOut/Exit dialog
     */
    private fun showAlertForLogout(Isforlogout: Boolean) {
        var msg = ""
        msg = if (Isforlogout) {
            "Are you sure you want to Logout?"
        } else {
            "Are you sure you want to exit?"
        }
        var Title = ""
        Title = if (Isforlogout) {
            "Confirm Logout"
        } else {
            "Confirm Exit"
        }
        val alertDialogBuilder = AlertDialog.Builder(
            this@HomeActivity
        )
        alertDialogBuilder.setTitle(Title)
        alertDialogBuilder
            .setMessage(msg)
            .setCancelable(true)
            .setPositiveButton(
                getString(R.string.action_yes)
            ) { dialog, id ->
                SharedPreference.getInstance(applicationContext)
                SharedPreference.clearSP(applicationContext)
                if (Isforlogout) {
                    finish()
                } else {
                    finish()
                }


            }
            .setNegativeButton(
                getString(R.string.action_cancel)
            ) { dialog, id -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    fun dashbordDynamicSize() {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val temp =
            Math.sqrt(xInches * xInches + yInches * yInches.toDouble())
        val diagonalInches = Math.round(temp * 2) / 2.0f.toDouble()
        if (diagonalInches >= 4 && diagonalInches < 4.5) {
            // icon size 60

            /*RelativeLayout.LayoutParams paramsSos = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            paramsSos.setMargins(0, 0, CommonMethods.dpToPx(10), CommonMethods.dpToPx(5));
            fl_Sos.setLayoutParams(paramsSos);*/
            val lp =
                HomeBinding.appbar.flSos.getLayoutParams() as RelativeLayout.LayoutParams
            lp.setMargins(0, 0, CommonMethods.dpToPx(10), CommonMethods.dpToPx(5))
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            HomeBinding.appbar.flSos.setLayoutParams(lp)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, CommonMethods.dpToPx(5), 0, 0)
            HomeBinding.appbar.rlDashbordInfo.setLayoutParams(params)
            val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params1.setMargins(
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(10),
                CommonMethods.dpToPx(50),
                0
            )
            HomeBinding.appbar.rlDashbordExtraInfo.setLayoutParams(params1)
        } else if (diagonalInches >= 4.5 && diagonalInches < 5) {
            // icon size 70
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, CommonMethods.dpToPx(5), 0, 0)
            HomeBinding.appbar.rlDashbordInfo.setLayoutParams(params)
            val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params1.setMargins(
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(10),
                CommonMethods.dpToPx(50),
                0
            )
            HomeBinding.appbar.rlDashbordExtraInfo.setLayoutParams(params1)
        } else if (diagonalInches >= 5 && diagonalInches < 5.5) {
            // icon size 85
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, CommonMethods.dpToPx(10), 0, 0)
            HomeBinding.appbar.rlDashbordInfo.setLayoutParams(params)
            val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params1.setMargins(
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(20),
                CommonMethods.dpToPx(50),
                0
            )
            HomeBinding.appbar.rlDashbordExtraInfo.setLayoutParams(params1)
        } else if (diagonalInches >= 5.5 && diagonalInches < 6) {
// icon size 75
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, CommonMethods.dpToPx(5), 0, 0)
            HomeBinding.appbar.rlDashbordInfo.setLayoutParams(params)
            val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params1.setMargins(
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(15),
                CommonMethods.dpToPx(50),
                0
            )
            HomeBinding.appbar.rlDashbordExtraInfo.setLayoutParams(params1)
        } else if (diagonalInches >= 6 && diagonalInches < 6.5) {
            // icon size 75
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, CommonMethods.dpToPx(40), 0, 0)
            HomeBinding.appbar.rlDashbordInfo.setLayoutParams(params)
            val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params1.setMargins(
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(40),
                CommonMethods.dpToPx(50),
                0
            )
            HomeBinding.appbar.rlDashbordExtraInfo.setLayoutParams(params1)
        } else {
            // icon size 75
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, CommonMethods.dpToPx(50), 0, 0)
            HomeBinding.appbar.rlDashbordInfo.setLayoutParams(params)
            val params1 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params1.setMargins(
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(50),
                CommonMethods.dpToPx(50),
                0
            )
            HomeBinding.appbar.rlDashbordExtraInfo.setLayoutParams(params1)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ManageNotification(intent!!)
    }


    fun AddData() {

        HomeBinding.appbar.tvRemainvalue.text = "0"
        HomeBinding.appbar.tvRemainmsg.text = "REMAINING\nBREAK-IN"
        HomeBinding.appbar.tvCompletevalue.text = "0"
        HomeBinding.appbar.tvCompletemsg.text = "TOTAL\nDRIVEN"
        HomeBinding.appbar.tvFulevalue.text = "0"
        HomeBinding.appbar.tvFulemsg.text = "REMAINING\nFULE"
        HomeBinding.appbar.tvAvgvalue.text = "0"
        HomeBinding.appbar.tvAvgmsg.text = "AVRAGE\nSPEED"
        HomeBinding.appbar.tvCurrentvalue.text = "0"
        HomeBinding.appbar.tvCurrentmsg.text = "CURRENT\nSPEED"
        HomeBinding.appbar.tvDrivevalue.text = "00:00"
        HomeBinding.appbar.tvdrivemsg.text = "TOTAL DRIVE\nTIME"
        HomeBinding.appbar.tvIdlevalue.text = "00:00"
        HomeBinding.appbar.tvidlemsg.text = "TOTAL IDLE\nTIME"
        HomeBinding.appbar.tvRestvalue.text = "00:00"
        HomeBinding.appbar.tvRestmsg.text = "TOTAL REST\nTIME"
    }


    fun AddModuleList() {

        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_operations),
                resources.getDrawable(R.drawable.operations)
            )
        )


        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_tracking),
                resources.getDrawable(R.drawable.tracking)
            )
        )
        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_entertain),
                resources.getDrawable(R.drawable.entertain)
            )
        )
        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_info),
                resources.getDrawable(R.drawable.info)
            )
        )
        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_emergency),
                resources.getDrawable(R.drawable.emergency)
            )
        )
        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_nearby),
                resources.getDrawable(R.drawable.nearby)
            )
        )
        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_fuel),
                resources.getDrawable(R.drawable.dashbord_fuel)
            )
        )
        Dashbordmenudata!!.add(
            DashbordMenu(
                getString(R.string.label_toll),
                resources.getDrawable(R.drawable.toll)
            )
        )



        HomeBinding.appbar.recyclerViewMenu.setHasFixedSize(true)
        HomeBinding.appbar.recyclerViewMenu.setLayoutManager(GridLayoutManager(this, 2))

        HomeBinding.appbar.recyclerViewMenu.addItemDecoration(
            DividerItemDecoration(
                HomeBinding.appbar.recyclerViewMenu.getContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        HomeBinding.appbar.recyclerViewMenu.addItemDecoration(
            DividerItemDecoration(
                HomeBinding.appbar.recyclerViewMenu.getContext(),
                DividerItemDecoration.HORIZONTAL
            )
        )


        Dashbordmenuadapter =
            DashbordMenuAdapter(Dashbordmenudata!!, this)
        HomeBinding.appbar.recyclerViewMenu.setAdapter(Dashbordmenuadapter)


        ItemClickSupport.addTo(HomeBinding.appbar.recyclerViewMenu)
            .setOnItemClickListener(object : ItemClickSupport.OnItemClickListener {
                override fun onItemClicked(
                    recyclerView: RecyclerView?,
                    position: Int,
                    v: View?
                ) {
                    // do it
                    if (position == 1) {
                        startActivity(Intent(this@HomeActivity, TrackingActivity::class.java))

                    } else if (position == 5) {

                        startActivity(Intent(this@HomeActivity, NearBy::class.java))
                        Toast.makeText(applicationContext, "Coming soon", Toast.LENGTH_SHORT).show()
                    } else if (position == 6) {
                        Toast.makeText(applicationContext, "Coming soon", Toast.LENGTH_SHORT).show()
                    } else if (position == 7) {
                        Toast.makeText(applicationContext, "Coming soon", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Coming soon", Toast.LENGTH_SHORT).show()
                    }
                }
            })

    }


    fun SetData() {
        val precisionspeed = DecimalFormat("0")
        val avgspeed: Float =
            DatabaseClient.getInstance(applicationContext).getAppDatabase()
                .taskDao().getSpeedavg()
        HomeBinding.appbar.tvAvgvalue.setText(precisionspeed.format(avgspeed.toDouble()))

        val TotalDriveTime = DatabaseClient.getInstance(applicationContext).getAppDatabase()
            .taskDao().getDataTravel("1")

        if (TotalDriveTime != null && TotalDriveTime.size > 0) {
            var timeoverspeed: Long = 0
            val formatter: DateFormat = SimpleDateFormat("dd MMM yyyy,HH:mm")
            val size = TotalDriveTime.size
            var i = 0
            while (i < size) {
                var temptime: Long = 0
                var j = i + 1
                while (i < size) {
                    if (j < TotalDriveTime.size) {
                        try {
                            temptime = Utility.GetDate(
                                formatter.parse(
                                    TotalDriveTime[j].time
                                ) as Date,
                                formatter.parse(TotalDriveTime[i].time) as Date
                            )
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                    } else {
                        i = j - 1
                        break
                    }
                    j++
                }
                timeoverspeed += temptime
                i++
            }

            HomeBinding.appbar.tvDrivevalue.setText(
                Utility.GetTimewithoutlable(
                    TimeUnit.SECONDS.toMillis(
                        timeoverspeed
                    )
                )
            )
        }


        val RestData = DatabaseClient.getInstance(applicationContext).getAppDatabase()
            .taskDao().getRestData("1")

        if (RestData != null && RestData.size > 0) {
            var timeoverspeed: Long = 0
            val formatter: DateFormat = SimpleDateFormat("dd MMM yyyy,HH:mm")
            val size = RestData.size
            var i = 0
            while (i < size) {
                var temptime: Long = 0
                var j = i + 1
                while (i < size) {
                    if (j < RestData.size && RestData[i].activity
                            .equals(Constants.REST_START) && RestData[j].activity
                            .equals(Constants.REST_END)
                    ) {
                        try {
                            temptime = Utility.GetDate(
                                formatter.parse(
                                    RestData[j].time
                                ) as Date,
                                formatter.parse(RestData[i].time) as Date
                            )
                        } catch (e: ParseException) {
                            e.printStackTrace()
                        }
                    } else {
                        i = j - 1
                        break
                    }
                    j++
                }
                timeoverspeed += temptime
                i++
            }

            HomeBinding.appbar.tvRestvalue.setText(
                Utility.GetTimewithoutlable(
                    TimeUnit.SECONDS.toMillis(
                        timeoverspeed
                    )
                )
            )
        }

        val dataIdle =
            DatabaseClient.getInstance(applicationContext).getAppDatabase().taskDao()
                .getSearch(Constants.IDLE_REMINDER)

        if (dataIdle != null) {
            HomeBinding.appbar.tvIdlevalue.setText(Utility.GetTimewithoutlable((dataIdle.size * Constants.IDLE_TIME).toLong()))
        }
    }


    private fun setRepeatingAsyncTask() {
        val handler = Handler()
        if (timer != null) {
            timer!!.cancel()
        }
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    try {
                        SetData()
                    } catch (e: Exception) {
                        val stringBuilder = StringBuilder()
                        stringBuilder.append(e.message)
                        stringBuilder.append("")

                    }
                }
            }
        }, 0, 30000)
    }

    fun cancelTimer() {

        if (timer != null) {
            timer?.cancel()
        }
    }


    override fun onReceive(intent: Intent?, Type: String?) {
        if (Type.equals("LocationChnage", ignoreCase = true)) {
            locationData =
                intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY) as MyLocationData
            val stringBuilder = java.lang.StringBuilder()
            stringBuilder.append(locationData!!.isaccuracy)
            stringBuilder.append(Gson().toJson(locationData))

            if (locationData!!.isaccuracy.equals(1) ) {
                HomeBinding.appbar.tvCurrentvalue.setText(locationData!!.speed)
            }
            CalculateDistance()
        } else if (Type.equals("ServiceStop", ignoreCase = true)) {
            if (!SharedPrefsUtils.getBooleanPreference(
                    applicationContext,
                    Constants.INTENT_ISUSERSTOPT_KEY,
                    false
                )
            ) {

                if (Build.VERSION.SDK_INT < 26) {
                    startService(Intent(applicationContext, LocationService::class.java))
                } else {
                    startForegroundService(
                        Intent(
                            applicationContext,
                            LocationService::class.java
                        )
                    )
                }

            }
        } else if (Type.equals("RestReminder", ignoreCase = true)) {

            invalidateOptionsMenu()
            val restDialogFragment = RestDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                Constants.INTENT_LOCATION_KEY,
                intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY)
            )
            restDialogFragment.setArguments(bundle)
            showDialog(restDialogFragment)
        } else if (Type.equals("OverSpeed", ignoreCase = true)) {

            val speedDialogFragment = SpeedDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                Constants.DISTANCE_KEY,
                intent!!.getStringExtra(Constants.DISTANCE_KEY)
            )
            speedDialogFragment.setArguments(bundle)
            showDialog(speedDialogFragment)
        } else if (Type.equals("IdleReminder", ignoreCase = true)) {

            val idleDialogFragment = IdleDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                Constants.INTENT_LOCATION_KEY,
                intent!!.getSerializableExtra(Constants.INTENT_LOCATION_KEY)
            )
            idleDialogFragment.setArguments(bundle)
            showDialog(idleDialogFragment)
        }
    }


    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
        askIgnoreOptimization()
        if (!(getSystemService(Context.LOCATION_SERVICE) as LocationManager).isProviderEnabled("gps")) {
            Utility.turnGPSOn(this)
        }

        /*  Glide.with(this@HomeActivity).load(
              "http://openweathermap.org/img/w/" + weather.getWeather().get(0).getIcon()
                  .toString() + ".png"
          ).error(R.drawable.ic_cloud).into( HomeBinding.appbar.ivWetherIcon)
          HomeBinding.appbar.ivWetherIcon.setColorFilter(resources.getColor(R.color.white))
            *//*if (!SharedPrefsUtils.getStringPreference(applicationContext, Constants.INTENT_WEATHER_KEY).equals("")) {
              val weather: CurrentWeather = Gson().fromJson(
                  SharedPrefsUtils.getStringPreference(
                      applicationContext,
                      Constants.INTENT_WEATHER_KEY
                  ), CurrentWeather::class.java
              )
              val precision = DecimalFormat("0")

             // tv_wether.setText(precision.format(weather.getMain().getTempMax()).toString() + "'C")
          }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1000) {
            if (resultCode == -1) {
                data?.getStringExtra("result")
            }
            if (resultCode == 0) {
                Utility.turnGPSOn(this)
            }
        }

    }


    //here we  Calculate Distacne for remaining Brake-in Travel Distance and Last Brake Distance
    fun CalculateDistance() {
        val precision = DecimalFormat("0.00")
        if (!SharedPrefsUtils.getBooleanPreference(
                applicationContext,
                Constants.INTENT_IS_REST_START_KEY,
                false
            )
            && SharedPrefsUtils.getDoublePreference(
                applicationContext,
                Constants.INTENT_DISTANCE_REMAING_KEY,
                0.0
            ) > 0
        ) {
            HomeBinding.appbar.tvRemainvalue.setText(
                precision.format(
                    (Constants.REST_AFTER_IN_KM -
                            SharedPrefsUtils.getDoublePreference(
                                applicationContext,
                                Constants.INTENT_DISTANCE_REMAING_KEY,
                                0.0
                            )) / 1000
                )
            )
        } else {
            HomeBinding.appbar.tvRemainvalue.setText("0")
            SharedPrefsUtils.setDoublePreference(
                applicationContext,
                Constants.INTENT_DISTANCE_REMAING_KEY,
                0.0
            )
        }
        val LastRestEnd: MyLocationData? = DatabaseClient.getInstance(this).getAppDatabase()
            .taskDao().getLastRestEnd()
        if (LastRestEnd != null) {

            val data = DatabaseClient.getInstance(this).getAppDatabase()
                .taskDao().getAllAfterRestEnd(LastRestEnd.id)


            if (data != null && data!!.size > 0) {
                var distanceoevrspeed = 0.0
                val size: Int = data!!.size
                for (i in 0 until size) {
                    if (i + 1 < size) {
                        distanceoevrspeed += Utility.getDistancBetweenTwoPoints(
                            data!!.get(i).latitude.toDouble(),
                            data!!.get(i).longitude.toDouble(),
                            data!!.get(i + 1).latitude.toDouble(),
                            data!!.get(i + 1).longitude.toDouble()
                        )
                    }
                }
                HomeBinding.appbar.tvLastBrakeValue.setText(precision.format(distanceoevrspeed / 1000))
            }
        } else {
            HomeBinding.appbar.tvLastBrakeValue.setText("0")
        }
        if (SharedPrefsUtils.getDoublePreference(
                applicationContext,
                Constants.INTENT_DISTANCE_DIFFT_KEY,
                0.0
            ) == 0.0
        ) {

            var TotaleDistance = DatabaseClient.getInstance(applicationContext).getAppDatabase()
                .taskDao().getDataTravel("1")

            if (TotaleDistance != null && TotaleDistance.size > 0) {
                var distance = 0.0
                val size = TotaleDistance.size
                for (i in 0 until size) {
                    if (i + 1 < size) {
                        distance += Utility.getDistancBetweenTwoPoints(
                            TotaleDistance[i].latitude.toDouble(),
                            TotaleDistance[i].longitude.toDouble(),
                            TotaleDistance[i + 1].latitude.toDouble(),
                            TotaleDistance[i + 1].longitude.toDouble()
                        )
                    }
                }
                HomeBinding.appbar.tvCompletevalue.setText(precision.format(distance / 1000))
                HomeBinding.appbar.tvCompletemsg.setText("TOTAL \n DRIVEN")
            }
        } else {
            HomeBinding.appbar.tvCompletevalue.setText(
                precision.format(SharedPrefsUtils.getDoublePreference(applicationContext, Constants.INTENT_DISTANCE_DIFFT_KEY, 0.0) / 1000)
            )
            HomeBinding.appbar.tvCompletemsg.setText("TOTAL \n DRIVEN")
        }
    }


    fun showDialog(msg: DialogFragment) {
        pervDialog?.dismiss()
        if (pervDialog != null && pervDialog?.getFragmentManager() != null && (pervDialog is RestDialogFragment || pervDialog is SosDialogFragment)) {
            val fragment: Fragment? =
                pervDialog?.getFragmentManager()!!.findFragmentById(R.id.map_rest)
            val fragment1: Fragment? =
                pervDialog!!.getFragmentManager()!!.findFragmentById(R.id.map_sos)
            if (fragment1 != null) {
                val ft =
                    Objects.requireNonNull(this).supportFragmentManager
                        .beginTransaction()
                ft.remove(fragment1)
                ft.commit()
            }
            if (fragment != null) {
                val ft =
                    Objects.requireNonNull(this).supportFragmentManager
                        .beginTransaction()
                ft.remove(fragment)
                ft.commit()
            }
        }
        pervDialog = msg


        //closeYourDialogFragment();
        //  MapDialogFragment dialogFragment = new MapDialogFragment();
        val ft =
            supportFragmentManager.beginTransaction()
        val prev =
            supportFragmentManager.findFragmentByTag(Constants.DIALOG_FRGAMET_KEY)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        msg.show(supportFragmentManager, Constants.DIALOG_FRGAMET_KEY)
        HomeBinding.appbar.ivSos.setEnabled(true)
    }

    fun ManageNotification(intent: Intent) {
        if (intent.getBooleanExtra(Constants.OPEN_MAP_KEY, false)) {
            val location =
                intent.getParcelableExtra<Parcelable>(Constants.INTENT_LOCATION_KEY) as Location
            if (location != null) {
                val uri = String.format(
                    Locale.ENGLISH,
                    "geo:%f,%f",
                    location.latitude,
                    location.longitude
                )
                val intent_map = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                startActivity(intent_map)
            }
        } else if (intent.getBooleanExtra(Constants.IS_IDLE_REMINDER, false)) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                Constants.IDLE_NOTIFICATION_ID
            )
            val idleDialogFragment = IdleDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                Constants.INTENT_LOCATION_KEY,
                intent.getSerializableExtra(Constants.INTENT_LOCATION_KEY)
            )
            idleDialogFragment.setArguments(bundle)
            showDialog(idleDialogFragment)
        } else if (intent.getBooleanExtra(Constants.IS_FOR_REST, false)) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                Constants.REST_NOTIFICATION_ID
            )
            val restDialogFragment = RestDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                Constants.INTENT_LOCATION_KEY,
                intent.getSerializableExtra(Constants.INTENT_LOCATION_KEY)
            )
            restDialogFragment.setArguments(bundle)
            showDialog(restDialogFragment)
        } else if (intent.getBooleanExtra(Constants.ISFOR_OVERSPEED, false)) {
            val speedDialogFragment = SpeedDialogFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                Constants.DISTANCE_KEY,
                intent.getStringExtra(Constants.DISTANCE_KEY)
            )
            speedDialogFragment.setArguments(bundle)
            showDialog(speedDialogFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        val item = menu.findItem(R.id.rest)
        item.isVisible = SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.IS_REST_REMINDER, false)
        if (SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_IS_REST_START_KEY, false)) {
            item.title = "Rest off"
        } else {
            item.title = "Take Rest"
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.rest -> {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(Constants.REST_NOTIFICATION_ID)
                if (SharedPrefsUtils.getBooleanPreference(applicationContext, Constants.INTENT_IS_REST_START_KEY, false)) {
                    val restOffDialogFragment = RestOffDialogFragment()
                    val bundle = Bundle()
                    restOffDialogFragment.setArguments(bundle)
                    restOffDialogFragment.show(supportFragmentManager, "dialog")
                } else {
                    val restDialogFragment = RestDialogFragment()
                    val bundle = Bundle()
                    bundle.putSerializable(Constants.INTENT_LOCATION_KEY, Gson().fromJson(SharedPrefsUtils.getStringPreference(applicationContext, Constants.IS_REST_REMINDER_DATA), MyLocationData::class.java))
                    restDialogFragment.arguments = bundle
                    showDialog(restDialogFragment)
                }
            }

            R.id.rest -> {

            }


        }

        // This is required to make the drawer toggle work
        return if (toggle!!.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)

    }


    private fun askIgnoreOptimization() {
        if (Build.VERSION.SDK_INT >= 23) {
            val intent = Intent()
            val packageName = packageName
            if (!(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                val stringBuilder = java.lang.StringBuilder()
                stringBuilder.append("package:")
                stringBuilder.append(packageName)
                intent.data = Uri.parse(stringBuilder.toString())
                startActivity(intent)
            }
        }
    }

}