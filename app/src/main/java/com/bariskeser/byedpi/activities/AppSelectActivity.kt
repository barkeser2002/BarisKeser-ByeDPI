package com.bariskeser.byedpi.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.databinding.ActivityAppSelectBinding
import com.bariskeser.byedpi.utility.SplitAppEntry
import com.bariskeser.byedpi.utility.getPreferences
import com.bariskeser.byedpi.utility.getSplitExcludedApps
import com.bariskeser.byedpi.utility.loadSplitLaunchableApps
import com.bariskeser.byedpi.utility.setSplitExcludedApps
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppSelectBinding
    private val adapter = AppAdapter()

    private var allApps: List<SplitAppEntry> = emptyList()
    private var excludedPackages: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.split_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        excludedPackages = getPreferences().getSplitExcludedApps()

        binding.splitList.adapter = adapter
        binding.splitSearch.doAfterTextChanged { text ->
            applyFilter(text?.toString().orEmpty())
        }

        loadApps()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun loadApps() {
        binding.splitProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                loadSplitLaunchableApps(packageManager, packageName)
            }
            allApps = apps
            binding.splitProgress.visibility = View.GONE
            applyFilter(binding.splitSearch.text?.toString().orEmpty())
        }
    }

    private fun applyFilter(query: String) {
        val needle = query.trim()
        val filtered = if (needle.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.label.contains(needle, ignoreCase = true) }
        }
        adapter.items = filtered
        val loading = binding.splitProgress.visibility == View.VISIBLE
        binding.splitEmpty.visibility = if (filtered.isEmpty() && !loading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun onToggle(packageName: String, excluded: Boolean) {
        // Build new sets rather than mutating the one already handed out.
        excludedPackages = if (excluded) {
            excludedPackages + packageName
        } else {
            excludedPackages - packageName
        }
        getPreferences().setSplitExcludedApps(excludedPackages)
    }

    private inner class AppAdapter : BaseAdapter() {
        var items: List<SplitAppEntry> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): SplitAppEntry = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            val app = items[position]

            view.findViewById<ImageView>(R.id.app_icon).setImageDrawable(app.icon)
            view.findViewById<TextView>(R.id.app_label).text = app.label
            view.findViewById<TextView>(R.id.app_package).text = app.packageName

            // Clear the listener before setting state so recycled rows do not
            // fire a save for the previous app.
            val toggle = view.findViewById<SwitchMaterial>(R.id.app_switch)
            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = app.packageName in excludedPackages
            toggle.setOnCheckedChangeListener { _, checked ->
                onToggle(app.packageName, checked)
            }

            view.setOnClickListener { toggle.toggle() }
            return view
        }
    }
}
