package com.yourapp.seetuorganizer.ui.allUsers

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.adapter.AllUsersCustomAdapter
import com.yourapp.seetuorganizer.databinding.FragmentAllUsersBinding
import com.yourapp.seetuorganizer.model.UserModel

const val AD_UNIT_ID = "ca-app-pub-6773446513562001/5700188399"
const val TAG = "MainActivity"
class AllUsersFragment : Fragment() {
    private lateinit var binding : FragmentAllUsersBinding
    private lateinit var database : DatabaseReference
    private val args : AllUsersFragmentArgs by navArgs()

    private var mInterstitialAd: InterstitialAd? = null
    private var mAdIsLoading: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAllUsersBinding.bind(view)
        database = Firebase.database.reference

        val shader = LinearGradient(0f, 0f, 0f, binding.allUsersHeadingTextView.textSize, Color.RED, Color.BLUE,
            Shader.TileMode.CLAMP)
        binding.allUsersHeadingTextView.paint.shader = shader

        if (!mAdIsLoading && mInterstitialAd == null) {
            mAdIsLoading = true
            loadAd()
        }

        val orgUserInfoReference = database.child("organizerUserInfo").child(args.organizerPhoneNumber)
            .orderByChild("pending").startAt(1.toDouble())
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val allUsersList = mutableListOf<UserModel>()
                for(i in snapshot.children){
                    val user = i.getValue<UserModel>() as UserModel
                    allUsersList.add(user)
                }
                val recyclerView = binding.allUsersRecyclerView
                val linearLayoutManager = LinearLayoutManager(activity?.applicationContext)
                linearLayoutManager.reverseLayout = true
                linearLayoutManager.stackFromEnd = true
                recyclerView.layoutManager = linearLayoutManager

                val allUsersCustomAdapter = AllUsersCustomAdapter(activity?.applicationContext, allUsersList
                ) { position -> onListItemClicked(allUsersList[position]) }

                recyclerView.adapter = allUsersCustomAdapter
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        orgUserInfoReference.addValueEventListener(listener)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            showInterstitial()
            findNavController().popBackStack()
        }

    }

    private fun onListItemClicked(user : UserModel){
        val checkValue = context?.checkCallingOrSelfPermission(android.Manifest.permission.CALL_PHONE)
        if(checkValue == PackageManager.PERMISSION_GRANTED){
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:${user.phone}")
            startActivity(intent)
        }
        else{
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE), 123)
            //Toast.makeText(requireActivity(), "Need permission", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            android.R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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