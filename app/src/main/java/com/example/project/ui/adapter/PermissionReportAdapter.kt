package com.example.project.ui.adapter

import android.content.pm.PackageManager
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.db.PermissionLogEntity

data class GroupedPermissionLog(
    val appName: String,
    val packageName: String,
    val permissionType: String,
    val accessType: String,
    val events: List<Long>,
    val isSuspicious: Boolean
)

class PermissionReportAdapter(
    private var logs: List<GroupedPermissionLog>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PermissionReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        
        val ivStatusIcon: ImageView = view.findViewById(R.id.ivStatusIcon)
        val tvStatusHeader: TextView = view.findViewById(R.id.tvStatusHeader)
        
        val llRowsContainer: LinearLayout = view.findViewById(R.id.llRowsContainer)
        
        val llGeminiVerdict: LinearLayout = view.findViewById(R.id.llGeminiVerdict)
        val tvGeminiText: TextView = view.findViewById(R.id.tvGeminiText)
        
        val btnRevoke: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnRevoke)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        val context = holder.itemView.context

        try {
            val pm = context.packageManager
            val icon = pm.getApplicationIcon(log.packageName)
            holder.ivAppIcon.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.tvAppName.text = log.appName
        
        val eventTypeStr = if (log.accessType == "BACKGROUND") "background" else "in-use"
        val eventsText = "${log.events.size} $eventTypeStr event" + if (log.events.size > 1) "s" else ""
        holder.tvSubtitle.text = "${log.permissionType} · $eventsText"

        if (log.accessType == "BACKGROUND") {
            holder.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#F44336"))
            holder.tvStatusHeader.setTextColor(Color.parseColor("#F44336"))
            holder.tvStatusHeader.text = "NOT IN USE"
            
            holder.llGeminiVerdict.visibility = View.VISIBLE
            holder.btnRevoke.visibility = View.VISIBLE
            holder.btnRevoke.text = "Revoke ${log.permissionType} Access"
            
            val timesStr = if (log.events.size == 1) "1 time" else "${log.events.size} times"
            holder.tvGeminiText.text = "${log.appName} accessed your ${log.permissionType.lowercase()} $timesStr while you were not using it. Their privacy policy does not justify background ${log.permissionType.lowercase()} access. This is suspicious behavior."
        } else {
            // Foreground events
            holder.ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background)
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#4CAF50"))
            holder.tvStatusHeader.setTextColor(Color.parseColor("#4CAF50"))
            holder.tvStatusHeader.text = "WHILE IN USE"
            
            holder.llGeminiVerdict.visibility = View.GONE
            holder.btnRevoke.visibility = View.GONE
        }

        // Populate timestamps
        holder.llRowsContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        val dateFormat = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())

        for (timestamp in log.events) {
            val rowView = inflater.inflate(R.layout.item_timestamp_row, holder.llRowsContainer, false)
            val vStatusDot = rowView.findViewById<View>(R.id.vStatusDot)
            val tvTimestamp = rowView.findViewById<TextView>(R.id.tvTimestamp)
            val tvStatusText = rowView.findViewById<TextView>(R.id.tvStatusText)
            val llTimestampRowBackground = rowView.findViewById<LinearLayout>(R.id.llTimestampRowBackground)

            tvTimestamp.text = dateFormat.format(java.util.Date(timestamp))

            if (log.accessType == "BACKGROUND") {
                vStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
                tvStatusText.setTextColor(Color.parseColor("#F44336"))
                tvStatusText.text = "NOT IN USE"
                llTimestampRowBackground.setBackgroundResource(R.drawable.bg_risk_box_high)
            } else {
                vStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                tvStatusText.setTextColor(Color.parseColor("#4CAF50"))
                tvStatusText.text = "WHILE IN USE"
                llTimestampRowBackground.setBackgroundResource(R.drawable.bg_risk_box_medium)
            }

            holder.llRowsContainer.addView(rowView)
        }

        holder.btnRevoke.setOnClickListener {
            onItemClick(log.packageName)
        }
    }

    override fun getItemCount() = logs.size

    fun updateData(newLogs: List<PermissionLogEntity>) {
        val groupedLogsMap = newLogs.groupBy { 
            "${it.packageName}_${it.permissionType}_${it.accessType}" 
        }
        val groupedList = groupedLogsMap.values.map { logsInGroup ->
            val first = logsInGroup.first()
            GroupedPermissionLog(
                appName = first.appName,
                packageName = first.packageName,
                permissionType = first.permissionType,
                accessType = first.accessType,
                events = logsInGroup.map { it.timestamp }.sortedDescending(),
                isSuspicious = first.isSuspicious
            )
        }.sortedByDescending { it.events.maxOrNull() ?: 0L }
        
        this.logs = groupedList
        notifyDataSetChanged()
    }
}
