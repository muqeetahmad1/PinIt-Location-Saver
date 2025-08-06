package com.example.pinit

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class LocationAdapter(
    var locations: MutableList<SavedLocation>,
    private val context: Context
) : RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(), Filterable {

    private var allLocations: MutableList<SavedLocation> = ArrayList(locations)

    inner class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationName: TextView = itemView.findViewById(R.id.locationName)
        val locationCoords: TextView = itemView.findViewById(R.id.locationCoords)
        val viewMapButton: ImageButton = itemView.findViewById(R.id.btnViewMap)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
        val shareButton: ImageButton = itemView.findViewById(R.id.btnShare)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.locationName.text = location.name
        holder.locationCoords.text = "${location.latitude}, ${location.longitude}"

        holder.viewMapButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.name})"
            intent.data = android.net.Uri.parse(uri)
            context.startActivity(intent)
        }

        holder.shareButton.setOnClickListener {
            val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Location: ${location.name}")
                putExtra(Intent.EXTRA_TEXT, "Check this location: ${location.name}\n$mapsUrl")
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        holder.deleteButton.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Location")
                .setMessage("Are you sure you want to delete '${location.name}'?")
                .setPositiveButton("Yes") { _, _ ->
                    if (context is MainActivity) {
                        context.removeLocation(location)
                    }
                    val removedIndex = holder.adapterPosition
                    val removedLocation = locations[removedIndex]

                    locations.removeAt(removedIndex)
                    allLocations.remove(removedLocation)

                    notifyItemRemoved(removedIndex)
                    Toast.makeText(context, "Deleted ${location.name}", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun getItemCount(): Int = locations.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase(Locale.ROOT)?.trim() ?: ""
                val filtered = if (query.isEmpty()) {
                    allLocations
                } else {
                    allLocations.filter {
                        it.name.lowercase(Locale.ROOT).contains(query)
                    }.toMutableList()
                }

                return FilterResults().apply {
                    values = filtered
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                locations = results?.values as? MutableList<SavedLocation> ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }

    // Call this whenever you update the locations list externally
    fun updateData(newList: List<SavedLocation>) {
        locations = newList.toMutableList()
        allLocations = newList.toMutableList()
        notifyDataSetChanged()
    }
}
