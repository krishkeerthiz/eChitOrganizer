package com.yourapp.seetuorganizer.ui.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.adapter.SeetuCustomAdapter
import com.yourapp.seetuorganizer.databinding.FragmentHomeBinding
import com.yourapp.seetuorganizer.model.SeetuModel
import com.yourapp.seetuorganizer.ui.signIn.SignInActivity

class HomeFragment : Fragment() {
    private lateinit var binding : FragmentHomeBinding
    private lateinit var database : DatabaseReference
    private lateinit var auth : FirebaseAuth
    private lateinit var phoneNumber : String
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
        mAdView.adUnitId = "ca-app-pub-6773446513562001/7676642717"
        binding.homeBannerAdViewContainer.addView(mAdView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private fun checkForFirstTimeOrganizer(phone : String){
        val organizerInfoRef = database.child("organizerInfo").child(phone)
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()){
                    val action = HomeFragmentDirections.actionHomeFragmentToInfoFragment(phone)
                    view?.findNavController()?.navigate(action)
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        organizerInfoRef.addListenerForSingleValueEvent(listener)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        auth = Firebase.auth

        if(auth.currentUser == null){
            startActivity(Intent(requireActivity(), SignInActivity::class.java))
            requireActivity().finish()
            return
        }

        initializeAds()

        database = Firebase.database.reference
        phoneNumber = auth.currentUser!!.phoneNumber.toString()

        val shader = LinearGradient(0f, 0f, 0f, binding.homeHeadingTextView.textSize, Color.RED, Color.BLUE,
            Shader.TileMode.CLAMP)
        binding.homeHeadingTextView.paint.shader = shader

        val organizerReference = database.child("organizer").child(phoneNumber)
        val seetuReference = database.child("seets").child(phoneNumber)

        checkForFirstTimeOrganizer(phoneNumber)

        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val seetuList = mutableListOf<SeetuModel>()

                for(i in snapshot.children){
                    //val key = i.key.toString()
                    val seetu = i.getValue<SeetuModel>() as SeetuModel
                    seetuList.add(seetu)
                }

                val recyclerView = binding.homeRecyclerView
                val linearLayoutManager = LinearLayoutManager(activity?.applicationContext)
                linearLayoutManager.reverseLayout = true
                linearLayoutManager.stackFromEnd = true
                recyclerView.layoutManager = linearLayoutManager

                val seetuCustomAdapter = SeetuCustomAdapter(activity?.applicationContext, seetuList,
                { position -> onListItemClicked(seetuList[position]) },
                    {position -> onDeleteButtonClicked(seetuList[position].name!!)})
                recyclerView.adapter = seetuCustomAdapter
            }

            override fun onCancelled(error: DatabaseError){
            }
        }

        seetuReference.addValueEventListener(listener)

        binding.floatingActionButtonHome.setOnClickListener{
            showAddSeetuDialogOnButtonClicked(it, seetuReference)
        }

        binding.floatingActionButtonHomeUsers.setOnClickListener{
            val action = HomeFragmentDirections.actionHomeFragmentToAllUsersFragment(phoneNumber)
            view.findNavController().navigate(action)
        }
    }

    private fun onListItemClicked(seetu : SeetuModel){
        //Toast.makeText(requireActivity(), seetu.name , Toast.LENGTH_SHORT).show()
        val action = HomeFragmentDirections.actionHomeFragmentToSeetuFragment(phoneNumber, seetu)
        view?.findNavController()?.navigate(action)
    }

    private fun deleteSeetu(userList : MutableList<String>, seetuName: String, organizerRef : DatabaseReference){
        organizerRef.removeValue()
        val userRef = database.child("user")
        for(user in userList)
            userRef.child(user).child(phoneNumber).child(seetuName).removeValue()
    }

    private fun onDeleteButtonClicked(seetuName : String){
        val seetuRef = database.child("seets").child(phoneNumber).child(seetuName)
        val organizerRef = database.child("organizer").child(phoneNumber).child(seetuName)
        val listener = object : ValueEventListener{
            val userList = mutableListOf<String>()
            override fun onDataChange(snapshot: DataSnapshot) {
                for(i in snapshot.children){
                    val userPhone = i.key.toString()
                    userList.add(userPhone)
                }
                deleteSeetu(userList, seetuName, snapshot.ref)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        organizerRef.addListenerForSingleValueEvent(listener)
        seetuRef.removeValue()
        Toast.makeText(requireActivity(), "$seetuName is deleted", Toast.LENGTH_SHORT).show()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.edit_bank_details -> {
                showEditBankDetailsDialogOnButtonClicked(requireView())
                true
            }
            R.id.sign_out ->{
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addSeetuIfNotExists(seetuRef : DatabaseReference, seetuName : String, seetu : SeetuModel){
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                    Toast.makeText(requireActivity(), "Seetu Already exists!", Toast.LENGTH_LONG).show()
                else {
                    snapshot.ref.setValue(seetu)
                    Toast.makeText(requireActivity(), "Seetu added successfully", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        seetuRef.child(seetuName).addListenerForSingleValueEvent(listener)
    }

    private fun showAddSeetuDialogOnButtonClicked(view : View?, seetuReference: DatabaseReference){
        if(isAdded){
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle(getString(R.string.new_chit))

            val addSeetuDialog = layoutInflater.inflate(R.layout.alert_dialog_add_seetu, null)
            builder.setView(addSeetuDialog)

            val seetuNameET = addSeetuDialog.findViewById<TextInputEditText>(R.id.seetuNameEditText)
            val seetuAmountET = addSeetuDialog.findViewById<TextInputEditText>(R.id.seetuAmountEditText)
            val seetuBeetET = addSeetuDialog.findViewById<TextInputEditText>(R.id.seetuBeetEditText)
            val seetuMonthsET = addSeetuDialog.findViewById<TextInputEditText>(R.id.seetuMonthsEditText)
            val seetuYearET = addSeetuDialog.findViewById<TextInputEditText>(R.id.seetuYearEditText)

            builder.setPositiveButton(getString(R.string.save), DialogInterface.OnClickListener { dialogInterface, i ->

                val seetuName = seetuNameET.text.toString().capitalize()
                val seetuAmount = seetuAmountET.text.toString().toInt()
                val seetuBeet = seetuBeetET.text.toString().toInt()
                val seetuMonths = seetuMonthsET.text.toString().toInt()
                val seetuYear = seetuYearET.text.toString().toInt()

                val seetu = SeetuModel(seetuAmount, seetuBeet, seetuMonths, seetuName, seetuYear)

                addSeetuIfNotExists(seetuReference, seetuName, seetu)
            })

            builder.setNegativeButton(getString(R.string.cancel)) { dialogInterface, i ->
                dialogInterface.cancel()
            }

            val dialog = builder.create()
            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            val watcher : TextWatcher = object : TextWatcher{
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = !(seetuNameET.text.toString().isEmpty() ||
                            seetuAmountET.text.toString().isEmpty() ||
                            seetuBeetET.text.toString().isEmpty() ||
                            seetuMonthsET.text.toString().isEmpty() ||
                            seetuYearET.text.toString().isEmpty() )
                }
            }
            seetuNameET.addTextChangedListener(watcher)
            seetuAmountET.addTextChangedListener(watcher)
            seetuBeetET.addTextChangedListener(watcher)
            seetuMonthsET.addTextChangedListener(watcher)
            seetuYearET.addTextChangedListener(watcher)
        }
    }

    private fun updateBankDetails(bankName : String, branch : String, accNum : String, ifsc : String){
        val organizerInfoReference = database.child("organizerInfo").child(phoneNumber)
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(bankName != "")
                    snapshot.child("bankName").ref.setValue(bankName)
                if(branch != "")
                    snapshot.child("branch").ref.setValue(branch)
                if(accNum != "")
                    snapshot.child("accountNumber").ref.setValue(accNum)
                if(ifsc != "")
                    snapshot.child("ifsc").ref.setValue(ifsc)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        organizerInfoReference.addListenerForSingleValueEvent(listener)
    }

    private fun showEditBankDetailsDialogOnButtonClicked(view : View?){
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(getString(R.string.edit_bank_details))

        val editBankDetailsDialog = layoutInflater.inflate(R.layout.alert_dialog_edit_bank_details, null)
        builder.setView(editBankDetailsDialog)

        builder.setPositiveButton(getString(R.string.save)) { dialogInterface, i ->
            val bankName = editBankDetailsDialog.findViewById<TextInputEditText>(R.id.EditBankNameEditText).text.toString().capitalize()
            val branch = editBankDetailsDialog.findViewById<TextInputEditText>(R.id.editBranchEditText).text.toString().capitalize()
            val accountNumber = editBankDetailsDialog.findViewById<TextInputEditText>(R.id.editAccNumEditText).text.toString().capitalize()
            val ifsc = editBankDetailsDialog.findViewById<TextInputEditText>(R.id.editIfscEditText).text.toString().capitalize()

            updateBankDetails(bankName, branch, accountNumber, ifsc)
            Toast.makeText(requireActivity(), "Saved successfully", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialogInterface, i ->
            dialogInterface.cancel()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun signOut(){
        AuthUI.getInstance().signOut(requireActivity())
        startActivity(Intent(requireActivity(), SignInActivity::class.java))
        requireActivity().finish()
    }


}