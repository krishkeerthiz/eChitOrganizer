package com.yourapp.seetuorganizer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.databinding.AllUsersCardViewBinding
import com.yourapp.seetuorganizer.model.UserModel

class AllUsersCustomAdapter(
    private val context : Context?,
    private val allUsersList : List<UserModel>,
    private val onItemClicked : (position : Int) -> Unit
) : RecyclerView.Adapter<AllUsersCustomAdapter.MyViewHolder>() {

    private var selectedItem = -1
    private var lastSelectedItem = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
        val binding = AllUsersCardViewBinding.inflate(view, parent, false)
        return MyViewHolder(binding, onItemClicked )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int ) {
//        if(position == selectedItem)
//            holder.selectedBg()
//        else
//            holder.defaultBg()
        holder.nameTextView.text = allUsersList[position].name
        holder.localityTextView.text = allUsersList[position].locality
        holder.pendingTextView.text = "₹"+ allUsersList[position].pending.toString()
    }

    override fun getItemCount() = allUsersList.size

    inner class MyViewHolder(private val binding : AllUsersCardViewBinding, private val onItemClicked : (position : Int) -> Unit)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener{
        val nameTextView : TextView = binding.allUsersCardName
        val localityTextView : TextView = binding.allUsersCardLocality
        val pendingTextView : TextView = binding.allUsersCardPending

        init{
            binding.allUsersCardCallButton.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val position = absoluteAdapterPosition
            onItemClicked(position)
        }

//        override fun onLongClick(v: View?): Boolean {
//            selectedItem = absoluteAdapterPosition
//
//            lastSelectedItem = if (lastSelectedItem == -1)
//                selectedItem
//            else {
//                notifyItemChanged(lastSelectedItem)
//                selectedItem
//            }
//            notifyItemChanged(selectedItem)
//            return true
//        }
//
//        fun defaultBg() {
//            binding.cardView.setCardBackgroundColor(context?.resources?.getColor(R.color.blue)!!)
//            binding.allUsersCardDeleteButton.visibility = View.GONE
//            binding.allUsersCardEditButton.visibility = View.GONE
//            binding.allUsersCardCallButton.visibility = View.VISIBLE
//        }
//
//        fun selectedBg() {
//            binding.cardView.setCardBackgroundColor(context?.resources?.getColor(R.color.color_green)!!)
//            binding.allUsersCardDeleteButton.visibility = View.VISIBLE
//            binding.allUsersCardEditButton.visibility = View.VISIBLE
//            binding.allUsersCardCallButton.visibility = View.GONE
//        }

    }
}