package com.yourapp.seetuorganizer.ui.seetu

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.adapter.UserCustomAdapter
import com.yourapp.seetuorganizer.databinding.FragmentSeetuBinding
import com.yourapp.seetuorganizer.model.MonthModel
import com.yourapp.seetuorganizer.model.UserModel

const val AD_UNIT_ID = "ca-app-pub-6773446513562001/9639433409"
const val TAG = "MainActivity"
class SeetuFragment : Fragment() {
    private lateinit var binding: FragmentSeetuBinding
    private lateinit var database : DatabaseReference
    private val args : SeetuFragmentArgs by navArgs()

    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_seetu, container, false)
    }

    private fun setTitle(){
        binding.seetuTitleTextView.text = args.seetu.name
        binding.seetuAmount.text = "₹"+ args.seetu.amount.toString()+" "+ getString(R.string.chit)
        binding.seetuMonths.text = args.seetu.months.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSeetuBinding.bind(view)

        setTitle()

        database = Firebase.database.reference

        val shader = LinearGradient(0f, 0f, 0f, binding.seetuTitleTextView.textSize, Color.RED, Color.BLUE,
            Shader.TileMode.CLAMP)
        binding.seetuTitleTextView.paint.shader = shader

        if (!mAdIsLoading && mInterstitialAd == null) {
            mAdIsLoading = true
            loadAd()
        }

        val seetuReference = database.child("organizer").child(args.organizerPhoneNumber).child(args.seetu.name!!)
        val orgUserReference = database.child("organizerUserInfo").child(args.organizerPhoneNumber)
        val userReference = database.child("user")

        val usersPhoneList = mutableListOf<String>()

        val listener = object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<UserModel>()
                usersPhoneList.clear()
                for(i in snapshot.children){
                    val name = i.child("name").value.toString()
                    val locality = i.child("locality").value.toString()
                    val phone = i.child("phone").value.toString()
                    val pending = i.child("pending").value.toString()

                    userList.add(UserModel(locality, name, pending.toInt(), phone))
                    usersPhoneList.add(phone)
                }

                val recyclerView = binding.seetuRecyclerView
                val gridLayoutManager = GridLayoutManager(activity?.applicationContext, 2)

                recyclerView.layoutManager = gridLayoutManager

                val userCustomAdapter = UserCustomAdapter(userList)
                { position -> onListItemClicked(userList[position]) }
                recyclerView.adapter = userCustomAdapter

            }
            override fun onCancelled(error: DatabaseError) {
            }
        }

        seetuReference.addValueEventListener(listener)

        binding.seetuMonthAddButton.setOnClickListener{
            showAddMonthDialogOnButtonClicked(it, seetuReference, orgUserReference, userReference, usersPhoneList)
        }

        binding.seetuUserAddButton.setOnClickListener {
            showAddUserDialogOnButtonClicked(it, seetuReference, orgUserReference, userReference, usersPhoneList.size)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            showInterstitial()
            findNavController().popBackStack()
        }
    }

    private fun onListItemClicked(user : UserModel){
        //Toast.makeText(requireActivity(), "Item ${position+1} is clicked", Toast.LENGTH_SHORT).show()
        val action = SeetuFragmentDirections.actionSeetuFragmentToMonthsFragment(args.organizerPhoneNumber, user, args.seetu.name!!)
        view?.findNavController()?.navigate(action)
    }

    private fun updatePending(reference: DatabaseReference, amount : Int){
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var pending = snapshot.value.toString().toInt()
                pending += amount
                snapshot.ref.setValue(pending)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        reference.addListenerForSingleValueEvent(listener)
    }

    private fun addMonthIfNotExists( seetuReference : DatabaseReference, orgUserReference : DatabaseReference, userReference: DatabaseReference,
                                     userPhone : String, monthModel: MonthModel){
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                    Toast.makeText(requireActivity(), "Month Already exists!", Toast.LENGTH_LONG).show()
                else{
                    //Organizer Reference
                    seetuReference.child(userPhone).child("months").child("month${monthModel.month}").setValue(monthModel)
                    updatePending(seetuReference.child(userPhone).child("pending"), monthModel.toPay!!)

                    //User Reference
                    userReference.child(userPhone).child(args.organizerPhoneNumber)
                        .child(args.seetu.name!!).child("months").child("month${monthModel.month}").setValue(monthModel)
                    updatePending(userReference.child(userPhone).child(args.organizerPhoneNumber)
                        .child(args.seetu.name!!).child("pending"), monthModel.toPay!!)

                    //Organizer User Reference
                    updatePending(orgUserReference.child(userPhone).child("pending"), monthModel.toPay!!)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        seetuReference.child(userPhone).child("months").child("month"+monthModel.month.toString())
            .addListenerForSingleValueEvent(listener)
    }

    private fun showAddMonthDialogOnButtonClicked(view : View?, seetuReference : DatabaseReference, orgUserReference : DatabaseReference,
                                                  userReference: DatabaseReference, usersPhoneList : List<String>){

        if(usersPhoneList.isNotEmpty()){
            if(isAdded){
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle(getString(R.string.new_month))
                val addMonthDialog = layoutInflater.inflate(R.layout.alert_dialog_add_month, null)
                builder.setView(addMonthDialog)

                val monthET = addMonthDialog.findViewById<TextInputEditText>(R.id.monthNumberEditText)
                val yelamET = addMonthDialog.findViewById<TextInputEditText>(R.id.monthYelamEditText)
                val toPayET = addMonthDialog.findViewById<TextInputEditText>(R.id.monthToPayEditText)

                builder.setPositiveButton(getString(R.string.save), DialogInterface.OnClickListener { dialogInterface, i ->
                    val month = monthET.text.toString().toInt()
                    val yelam = yelamET.text.toString().toInt()
                    val toPay = toPayET.text.toString().toInt()
                    val monthModel = MonthModel(month, toPay, toPay, yelam)

                    for (phoneNumber in usersPhoneList)
                        addMonthIfNotExists(seetuReference, orgUserReference, userReference, phoneNumber, monthModel)

                    Toast.makeText(requireActivity(), "New Month added successfully", Toast.LENGTH_SHORT).show()
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
                        dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = !(monthET.text.toString().isEmpty() ||
                                yelamET.text.toString().isEmpty() ||
                                toPayET.text.toString().isEmpty() )
                    }
                }
                monthET.addTextChangedListener(watcher)
                yelamET.addTextChangedListener(watcher)
                toPayET.addTextChangedListener(watcher)
            }
        }
        else
            Toast.makeText(requireActivity(), "Add at least 1 user", Toast.LENGTH_SHORT).show()

    }

    private fun checkForFirstTimeUser(orgUserReference: DatabaseReference, user : UserModel){
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists())
                    snapshot.ref.setValue(user)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        orgUserReference.child(user.phone!!).addListenerForSingleValueEvent(listener)
    }
    private fun addUserIfNotExists(seetuReference : DatabaseReference, orgUserReference : DatabaseReference, userReference: DatabaseReference
    , user : UserModel){
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                    Toast.makeText(requireActivity(), "User Already exists!", Toast.LENGTH_LONG).show()
                else{
                    //Organizer reference
                    snapshot.ref.setValue(user)
                    //User Reference
                    val userRef = userReference.child(user.phone!!).child(args.organizerPhoneNumber).child(args.seetu.name!!)
                    userRef.child("name").setValue(args.seetu.name!!)
                    userRef.child("pending").setValue(0)
                    Toast.makeText(requireActivity(), "User added successfully", Toast.LENGTH_SHORT).show()

                    checkForFirstTimeUser(orgUserReference, user)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        seetuReference.child(user.phone!!).addListenerForSingleValueEvent(listener)
    }

    private fun showAddUserDialogOnButtonClicked(view : View?, seetuReference : DatabaseReference, orgUserReference : DatabaseReference,
                                                 userReference: DatabaseReference, userCount : Int){
        if(userCount < args.seetu.months!!){
            if(isAdded){
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle(getString(R.string.new_user))

                val addUserDialog = layoutInflater.inflate(R.layout.alert_dialog_add_user, null)
                builder.setView(addUserDialog)

                val nameEt = addUserDialog.findViewById<TextInputEditText>(R.id.userNameEditText)
                val localityET = addUserDialog.findViewById<TextInputEditText>(R.id.userLocalityEditText)
                val phoneET = addUserDialog.findViewById<TextInputEditText>(R.id.userPhoneEditText)
                val phoneTextInputLayout = addUserDialog.findViewById<TextInputLayout>(R.id.outlinedUserPhoneTextField)

                builder.setPositiveButton(getString(R.string.save), DialogInterface.OnClickListener { dialogInterface, i ->

                    val name = nameEt.text.toString().capitalize()
                    val locality = localityET.text.toString().capitalize()
                    val phone = phoneET.text.toString()

                    if(phone.length < 10)
                        //addUserDialog.findViewById<TextInputLayout>(R.id.outlinedUserPhoneTextField).error = "Enter Valid Phone Number"
                        Toast.makeText(requireContext(), "Enter Valid Phone Number ", Toast.LENGTH_SHORT).show()
                    else{
                        val user = UserModel(locality, name, 0, "+91$phone")
                        addUserIfNotExists(seetuReference, orgUserReference, userReference, user)
                    }
                })

                builder.setNegativeButton(getString(R.string.cancel)) { dialogInterface, i ->
                    dialogInterface.cancel()
                }

                val dialog = builder.create()
                dialog.show()

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                val watcher = object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) {
                        dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = !(nameEt.text.toString().isEmpty() ||
                                localityET.text.toString().isEmpty() ||
                                phoneET.text.toString().isEmpty() || (phoneET.text.toString().length < 10) )
                    }
                }
                val phoneFormatWatcher = object : PhoneNumberFormattingTextWatcher() {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if(phoneET.text.toString().length in 1..9)
                            phoneTextInputLayout.error = "Enter Valid Phone Number"
                        else{
                            phoneTextInputLayout.error = null
                            phoneTextInputLayout.isErrorEnabled = false
                        }


                    }
                }
                nameEt.addTextChangedListener(watcher)
                localityET.addTextChangedListener(watcher)
                phoneET.addTextChangedListener(watcher)
                phoneET.addTextChangedListener(phoneFormatWatcher)

            }
        }
        else
            Toast.makeText(requireActivity(), "Maximum number of users created", Toast.LENGTH_SHORT).show()
    }

    private fun loadAd() {
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            requireActivity(), AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError?.message)
                    mInterstitialAd = null
                    mAdIsLoading = false
                    val error = "domain: ${adError.domain}, code: ${adError.code}, " +
                            "message: ${adError.message}"
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mAdIsLoading = false

                }
            }
        )
    }

    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    mInterstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {
                }
            }
            mInterstitialAd?.show(requireActivity())
        }
    }

}
