package com.yourapp.seetuorganizer.ui.info

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {
    private lateinit var binding : FragmentInfoBinding
    private val args : InfoFragmentArgs by navArgs()
    private lateinit var databse : DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        activity?.actionBar?.setHomeButtonEnabled(false)
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    private fun updateInfo(name : String, area : String, address : String, bankName : String, branch : String, accNum : String, ifsc : String){
        val orgInfoRef = databse.child("organizerInfo").child(args.organizerPhoneNumber)
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()){
                    snapshot.child("name").ref.setValue(name)
                    snapshot.child("area").ref.setValue(area)
                    snapshot.child("address").ref.setValue(address)
                    snapshot.child("phone").ref.setValue(args.organizerPhoneNumber)
                    snapshot.child("bankName").ref.setValue(bankName)
                    snapshot.child("branch").ref.setValue(branch)
                    snapshot.child("accNum").ref.setValue(accNum)
                    snapshot.child("ifsc").ref.setValue(ifsc)
                }
                val action = InfoFragmentDirections.actionInfoFragmentToHomeFragment()
                view?.findNavController()?.navigate(action)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        orgInfoRef.addListenerForSingleValueEvent(listener)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInfoBinding.bind(view)

        databse = Firebase.database.reference

        binding.nextButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().capitalize()
            val area = binding.areaEditText.text.toString().capitalize()
            val address = binding.addressEditText.text.toString().capitalize()
            val bankName = binding.bankNameEditText.text.toString().capitalize()
            val branch = binding.branchEditText.text.toString().capitalize()
            val accNum = binding.accNumEditText.text.toString().capitalize()
            val ifsc = binding.ifscEditText.text.toString().capitalize()

            updateInfo(name, area, address, bankName, branch, accNum, ifsc)
        }
        binding.nextButton.isEnabled = false
        val watcher = object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding.nextButton.isEnabled = !(binding.nameEditText.text.toString().isEmpty() ||
                        binding.areaEditText.text.toString().isEmpty() ||
                        binding.addressEditText.text.toString().isEmpty() )
            }
        }
        binding.nameEditText.addTextChangedListener(watcher)
        binding.areaEditText.addTextChangedListener(watcher)
        binding.addressEditText.addTextChangedListener(watcher)
    }
}