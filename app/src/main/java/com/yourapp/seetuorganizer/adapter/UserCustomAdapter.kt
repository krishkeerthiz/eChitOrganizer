package com.yourapp.seetuorganizer.adapter

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.databinding.UserCardViewBinding
import com.yourapp.seetuorganizer.model.UserModel

class UserCustomAdapter(
    private val userList : List<UserModel>,
    private val onItemClicked : (position : Int) -> Unit
) : RecyclerView.Adapter<UserCustomAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
        val binding = UserCardViewBinding.inflate(view, parent, false)
        return MyViewHolder(binding, onItemClicked )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int ) {
        holder.nameTextView.text = userList[position].name
        holder.localityTextView.text = userList[position].locality
        holder.pendingTextView.text = "₹"+ userList[position].pending.toString()
    }

    override fun getItemCount() = userList.size

    inner class MyViewHolder(private val binding : UserCardViewBinding,  private val onItemClicked : (position : Int) -> Unit)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener{
        val nameTextView : TextView = binding.userCardName
        val localityTextView : TextView = binding.userCardLocality
        val pendingTextView : TextView = binding.userCardPending

        init{
            binding.root.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val position = absoluteAdapterPosition
            onItemClicked(position)
            //view?.findNavController()?.navigate(R.id.monthsFragment)
        }

    }
}

