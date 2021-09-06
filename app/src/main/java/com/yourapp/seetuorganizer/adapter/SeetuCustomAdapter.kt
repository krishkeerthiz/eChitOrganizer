package com.yourapp.seetuorganizer.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.yourapp.seetuorganizer.R
import com.yourapp.seetuorganizer.databinding.SeetuCardViewBinding
import com.yourapp.seetuorganizer.model.SeetuModel

class SeetuCustomAdapter(
    private val context : Context?,
    private val seetuList : List<SeetuModel>,
    private val onItemClicked : (position : Int) -> Unit,
    private val onDeleteButtonClicked : (position : Int) -> Unit
) : RecyclerView.Adapter<SeetuCustomAdapter.MyViewHolder>() {

    private var selectedItem = -1
    private var lastSelectedItem = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
        val binding = SeetuCardViewBinding.inflate(view, parent, false)
        return MyViewHolder(binding, onItemClicked, onDeleteButtonClicked)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int ) {
        if(position == selectedItem)
            holder.selectedBg()
        else
            holder.defaultBg()
        holder.nameTextView.text = seetuList[position].name
        holder.amountTextView.text = "₹"+seetuList[position].amount.toString()
        val monthsString = context?.getString(R.string.months)
        holder.monthsTextView.text = seetuList[position].months.toString() +" "+monthsString

    }

    override fun getItemCount() = seetuList.size

    inner class MyViewHolder(private val binding : SeetuCardViewBinding,  private val onItemClicked : (position : Int) -> Unit,
                              onDeleteButtonClicked: (position: Int) -> Unit)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        val nameTextView: TextView = binding.seetuCardName
        val amountTextView: TextView = binding.seetuCardAmount
        val monthsTextView: TextView = binding.seetuCardMonths
        val deleteButton = binding.seetuCardDeleteButton

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
            deleteButton.setOnClickListener {
                onDeleteButtonClicked(absoluteAdapterPosition)
             }
        }

        override fun onClick(view: View?) {
            val position = absoluteAdapterPosition
            onItemClicked(position)
        }


        override fun onLongClick(v: View?): Boolean {
            selectedItem = absoluteAdapterPosition

            lastSelectedItem = if (lastSelectedItem == -1)
                selectedItem
            else {
                notifyItemChanged(lastSelectedItem)
                selectedItem
            }
            notifyItemChanged(selectedItem)
            return true
        }

        fun defaultBg() {
            binding.cardView.setCardBackgroundColor(context?.resources?.getColor(R.color.blue)!!)
            binding.seetuCardDeleteButton.visibility = View.GONE
        }

        fun selectedBg() {
            binding.cardView.setCardBackgroundColor(context?.resources?.getColor(R.color.color_green)!!)
            binding.seetuCardDeleteButton.visibility = View.VISIBLE
        }
    }
}

