package com.yourapp.seetuorganizer.ui.months

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.adapter.MonthCustomAdapter
import com.yourapp.seetuorganizer.databinding.FragmentMonthsBinding
import com.yourapp.seetuorganizer.model.MonthModel

class MonthsFragment : Fragment() {
    private lateinit var binding : FragmentMonthsBinding
    private lateinit var database : DatabaseReference
    private val args : MonthsFragmentArgs by navArgs()
    private lateinit var balance : MutableLiveData<Int>
    private lateinit var mAdView : AdView

    private val adSize: AdSize
        get() {
            val display = requireActivity().windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density
            var adWidthPixels = outMetrics.widthPixels.toFloat()

            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireActivity(), adWidth)
        }

    private fun initializeAds(){
        // Calling Ads.
        MobileAds.initialize(requireActivity())

        mAdView = AdView(requireActivity())
        mAdView.adSize = adSize
        mAdView.adUnitId = "ca-app-pub-6773446513562001/1111234367"
        binding.monthsBannerAdViewContainer.addView(mAdView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_months, container, false)
    }

    private fun setTitle(){
        binding.userNameTextView.text = args.user.name
        //binding.userPendingAmount.text = args.user.pending.toString()
        binding.userContactNumber.text = args.user.phone
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMonthsBinding.bind(view)
        balance = MutableLiveData<Int>()
        balance.value = args.user.pending
        setTitle()

        initializeAds()

        balance.observe(requireActivity(), Observer {
            binding.userPendingAmount.text = "₹"+balance.value.toString()
        })
        database = Firebase.database.reference

        val shader = LinearGradient(0f, 0f, 0f, binding.userNameTextView.textSize, Color.RED, Color.BLUE,
            Shader.TileMode.CLAMP)
        binding.userNameTextView.paint.shader = shader

        val monthsReference = database.child("organizer").child(args.organizerPhoneNumber).
        child(args.seetuName).child(args.user.phone!!).child("months")

        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val monthList = mutableListOf<MonthModel>()
                for(i in snapshot.children){
                    val month = i.getValue<MonthModel>() as MonthModel
                    monthList.add(month)
                }

                val recyclerView = binding.monthsRecyclerView
                val linearLayoutManager = LinearLayoutManager(activity?.applicationContext)

                recyclerView.layoutManager = linearLayoutManager

                val monthsCustomAdapter = MonthCustomAdapter(activity?.applicationContext, monthList)
                { position -> onListItemClicked(monthList[position].month!!, monthList[position].pending!!, monthList[position]) }
                recyclerView.adapter = monthsCustomAdapter
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        monthsReference.addValueEventListener(listener)
    }

    private fun onListItemClicked(monthNumber : Int, pending : Int, month : MonthModel){
        //Toast.makeText(requireActivity(), "Item ${position+1} is clicked", Toast.LENGTH_SHORT).show()
        showEditMonthDialogOnButtonClicked(requireView(), monthNumber, pending, month)
    }

    private fun updateTotalPending(reference: DatabaseReference, amountReceived: Int){
        val listener = object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var pending = snapshot.value.toString().toInt()
                pending -= amountReceived
                snapshot.ref.setValue(pending)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        reference.child("pending").addListenerForSingleValueEvent(listener)
    }

    private fun updateMonthValue(reference: DatabaseReference, amountReceived : Int, monthNumber: Int){
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var pending = snapshot.value.toString().toInt()
                pending -= amountReceived
                snapshot.ref.setValue(pending)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        reference.child("months").child("month$monthNumber").child("pending").addListenerForSingleValueEvent(listener)
    }

//    private fun generateMessage(month : MonthModel, amountReceived: Int){
//        val orgInfoRef = database.child("organizerInfo").child(args.organizerPhoneNumber).child("name")
//        val listener = object : ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val name = snapshot.value.toString()
//                val message = "Organizer: $name\nUser: ${args.user.name}\nSeetu Name: ${args.seetuName}\n" +
//                        "Month: ${month.month}\nYelam: ${month.yelam}\nTo Pay: ${month.toPay}\nPending: ${month.pending}\nReceived: " +
//                        "$amountReceived\nCurrent Pending: ${month.pending?.minus(amountReceived)} "
//                sendSMS(message)
//            }
//            override fun onCancelled(error: DatabaseError) {
//            }
//        }
//        orgInfoRef.addListenerForSingleValueEvent(listener)
//    }
//
//    private fun sendSMS(message : String){
//        val number = args.user.phone
//        val SENT = "SMS_SENT"
//        val DELIVERED = "SMS_DELIVERED"
//        val sentPI = PendingIntent.getBroadcast(requireActivity(), 0, Intent(SENT), 0)
//        val deliveredPI = PendingIntent.getBroadcast(requireActivity(), 0, Intent(DELIVERED), 0)
//
//        val permissionCheck = ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.READ_PHONE_STATE)
//        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
//            val REQUEST_READ_PHONE_STATE = 123
//            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.READ_PHONE_STATE), REQUEST_READ_PHONE_STATE)
//        }
//        else{
//            val broadcastReceiverSent = object : BroadcastReceiver() {
//                override fun onReceive(arg0: Context?, arg1: Intent?) {
//                    when (resultCode) {
//                        Activity.RESULT_OK -> {
//                            val values = ContentValues()
//                            values.put("address", number)
//                            values.put("body", message)
//                            activity!!.contentResolver.insert(Uri.parse("content://sms/sent"), values)
//                            Toast.makeText(requireActivity(), "SMS sent", Toast.LENGTH_SHORT).show()
//                        }
//                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(requireActivity(), "Generic failure", Toast.LENGTH_SHORT).show()
//                        SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(requireActivity(), "No service", Toast.LENGTH_SHORT).show()
//                        SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(requireActivity(), "Null PDU", Toast.LENGTH_SHORT).show()
//                        SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(requireActivity(), "Radio off", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//            LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(broadcastReceiverSent, IntentFilter(SENT))
//
//            val broadcastReceiverDelivered = object : BroadcastReceiver(){
//                override fun onReceive(p0: Context?, p1: Intent?) {
//                    when(resultCode){
//                        Activity.RESULT_OK -> Toast.makeText(requireActivity(), "Sms delivered", Toast.LENGTH_SHORT).show()
//                        Activity.RESULT_CANCELED -> Toast.makeText(requireActivity(), "Sms not delivered", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//            LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(broadcastReceiverDelivered, IntentFilter(DELIVERED))
//
//            val smsManager = SmsManager.getDefault()
//            smsManager.sendTextMessage(number, args.organizerPhoneNumber, message, sentPI, deliveredPI)
//            //Toast.makeText(requireActivity(), "message sent", Toast.LENGTH_SHORT).show()
//        }
//
//
//    }

    private fun showEditMonthDialogOnButtonClicked(view : View?, monthNumber: Int, pending : Int, month : MonthModel){
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(getString(R.string.pending))

        val editMonthDialog = layoutInflater.inflate(R.layout.alert_dialog_edit_month, null)
        builder.setView(editMonthDialog)

        val amountReceivedEditText = editMonthDialog.findViewById<TextInputEditText>(R.id.monthAmountReceivedEditText)
        //amountReceivedEditText.hint = pending.toString()

        val organizerReference = database.child("organizer").child(args.organizerPhoneNumber).child(args.seetuName)
            .child(args.user.phone!!)
        val orgUserReference = database.child("organizerUserInfo").child(args.organizerPhoneNumber)
            .child(args.user.phone!!)
        val userReference = database.child("user").child(args.user.phone!!).child(args.organizerPhoneNumber)
            .child(args.seetuName)

        builder.setPositiveButton(getString(R.string.save), DialogInterface.OnClickListener { dialogInterface, i ->
            var amountReceived = amountReceivedEditText.text.toString().toInt()
            if(amountReceived > pending)
                amountReceived = pending

            //update livedata
            balance.value = balance.value?.minus(amountReceived)

            //Update Month pending
            updateMonthValue(organizerReference, amountReceived, monthNumber)
            updateMonthValue(userReference, amountReceived, monthNumber)

            //Update Total Pending
            updateTotalPending(organizerReference, amountReceived)
            updateTotalPending(orgUserReference, amountReceived)
            updateTotalPending(userReference, amountReceived)

            //generateMessage(month, amountReceived)
            Toast.makeText(requireActivity(), "Amount updated", Toast.LENGTH_SHORT).show()
        })

        builder.setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialogInterface, i ->
            dialogInterface.cancel()
        })

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        val watcher = object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    amountReceivedEditText.text.toString().isNotEmpty()
            }
        }
        amountReceivedEditText.addTextChangedListener(watcher)
    }

}