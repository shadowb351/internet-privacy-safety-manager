package com.example.project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project.databinding.ItemAppBinding

class AppAdapter(
    private var apps: List<AppModel>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    inner class AppViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppModel) {
            binding.tvAppName.text = app.name
            binding.tvPackageName.text = app.packageName
            binding.ivAppIcon.setImageDrawable(app.icon)
            // Remove listener before setting state to avoid infinite recursion/unwanted triggers
            binding.cbSelect.setOnCheckedChangeListener(null)
            binding.cbSelect.isChecked = app.isSelected

            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    apps.forEach { if (it !== app) it.isSelected = false }
                    app.isSelected = true
                    binding.root.post { notifyDataSetChanged() }
                } else {
                    app.isSelected = false
                }
                onSelectionChanged(getSelectedCount())
            }
            
            // Allow clicking the entire card
            binding.root.setOnClickListener {
                binding.cbSelect.isChecked = !binding.cbSelect.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount() = apps.size

    fun updateData(newApps: List<AppModel>) {
        apps = newApps
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int {
        return apps.count { it.isSelected }
    }

    fun getSelectedApps(): List<AppModel> {
        return apps.filter { it.isSelected }
    }
}
