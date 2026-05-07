package com.example.project.ui.adapter

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.util.DangerousComboResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class PrivacyFixAdapter(
    private var issues: List<DangerousComboResult>,
    private val onFixClicked: (String) -> Unit
) : RecyclerView.Adapter<PrivacyFixAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAppIconIssue: ImageView = view.findViewById(R.id.ivAppIconIssue)
        val tvAppNameIssue: TextView = view.findViewById(R.id.tvAppNameIssue)
        val tvSeverityBadge: TextView = view.findViewById(R.id.tvSeverityBadge)
        val cgPermissions: ChipGroup = view.findViewById(R.id.cgPermissions)
        val btnRemovePermissions: MaterialButton = view.findViewById(R.id.btnRemovePermissions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_privacy_issue, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val issue = issues[position]
        val context = holder.itemView.context

        try {
            val pm = context.packageManager
            val icon = pm.getApplicationIcon(issue.packageName)
            holder.ivAppIconIssue.setImageDrawable(icon)
        } catch (e: PackageManager.NameNotFoundException) {
            holder.ivAppIconIssue.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.tvAppNameIssue.text = issue.appName

        // Setup Severity Badge
        holder.tvSeverityBadge.text = issue.severity
        if (issue.severity.equals("Critical", ignoreCase = true)) {
            holder.tvSeverityBadge.setTextColor(Color.parseColor("#F44336"))
            holder.tvSeverityBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3A2525"))
        } else {
            holder.tvSeverityBadge.setTextColor(Color.parseColor("#FFC107"))
            holder.tvSeverityBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#332B1A"))
        }

        // Setup Permissions Chips
        holder.cgPermissions.removeAllViews()
        issue.permissionsFound.forEach { perm ->
            val simpleName = mapPermissionName(perm)
            val chip = Chip(context).apply {
                text = simpleName
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(android.R.color.transparent)
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#2A2A2A"))
                setTextColor(Color.parseColor("#A0A0A0"))
                chipStrokeWidth = 0f
            }
            holder.cgPermissions.addView(chip)
        }

        // Action Button
        holder.btnRemovePermissions.setOnClickListener {
            onFixClicked(issue.packageName)
        }
    }

    override fun getItemCount() = issues.size

    fun updateData(newIssues: List<DangerousComboResult>) {
        issues = newIssues
        notifyDataSetChanged()
    }

    private fun mapPermissionName(permission: String): String {
        return when (permission) {
            android.Manifest.permission.RECORD_AUDIO -> "Microphone"
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Location"
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS -> "Contacts"
            android.Manifest.permission.CAMERA -> "Camera"
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage"
            else -> permission.substringAfterLast('.')
        }
    }
}
