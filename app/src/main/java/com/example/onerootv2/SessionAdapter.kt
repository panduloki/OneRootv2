package com.example.onerootv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Recycler view tutorial
// https://www.youtube.com/watch?v=VVXKVFyYQdQ

// when recycler item clicked navigate to new view
// https://www.youtube.com/watch?v=WqrpcWXBz14
// https://www.youtube.com/watch?v=dB9JOsVx-yY
// https://www.youtube.com/watch?v=EoJX7h7lGxM


// create adapter and view holder
class SessionAdapter(private val sessionList:ArrayList<SessionUser>):RecyclerView.Adapter<SessionAdapter.SessionViewHolder> (){

    // on click listener when user clicks
    private lateinit var mListener: OnItemClickListener
    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
    fun setOnClickListener(listener: OnItemClickListener){
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        // create item View from parent and inflate with session_item
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.session_item, parent, false)

        // return SessionViewHolder with mListener
        return SessionViewHolder(itemView,mListener)
    }

    override fun getItemCount(): Int {
        return sessionList.size
    }

    // binding view holder data with sessionList data
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val currentItem = sessionList[position]
        holder.sessionNoText.text = currentItem.sessionNo
        holder.sessionDateText.text = currentItem.sessionDate
        holder.coconutNo.text= currentItem.coconutCount.toString()

        // change image based on current item
        when (currentItem.sessionType) {
            "loading" -> {
                holder.loadingImage.setImageResource(R.drawable.truck_loading)
            }
            "unloading" -> {
                holder.loadingImage.setImageResource(R.drawable.truck_unload)
            }
            else -> {
                holder.loadingImage.setImageResource(R.color.black)
            }
        }

    }

    // create view Holder parameters with mListener as input
    class SessionViewHolder(itemView: View, listener: OnItemClickListener):RecyclerView.ViewHolder(itemView){
        var sessionNoText:TextView = itemView.findViewById(R.id.sessionNoTextView)
        var sessionDateText:TextView = itemView.findViewById(R.id.DateTextView)
        var coconutNo:TextView = itemView.findViewById(R.id.coconutNoTextView)
        var loadingImage:ImageView = itemView.findViewById(R.id.loadingImageView)
        // user clicked
        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }

}