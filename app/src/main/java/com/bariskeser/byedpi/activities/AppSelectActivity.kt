package com.bariskeser.byedpi.activities

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.databinding.ActivityAppSelectBinding
import com.bariskeser.byedpi.utility.SplitAppEntry
import com.bariskeser.byedpi.utility.getPreferences
import com.bariskeser.byedpi.utility.getSplitExcludedApps
import com.bariskeser.byedpi.utility.setSplitExcludedApps
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class AppSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppSelectBinding
    private val viewModel: AppSelectViewModel by viewModels()
    private val adapter = AppAdapter()

    private var excludedPackages: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark-only, so the system bars always use light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        binding = ActivityAppSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()

        title = getString(R.string.split_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        excludedPackages = getPreferences().getSplitExcludedApps()

        binding.splitList.adapter = adapter
        binding.splitSearch.doAfterTextChanged {
            applyFilter(binding.splitSearch.text?.toString().orEmpty())
        }

        // Collect the app list from the ViewModel. It is already loaded after a
        // rotation, so the list is in place before the ListView restores its position.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apps.collect {
                    applyFilter(binding.splitSearch.text?.toString().orEmpty())
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    /**
     * Keeps the content clear of the system bars and the display cutout. The list's
     * bottom padding is added to its own padding and clipToPadding is false, so rows
     * scroll behind the navigation bar but the last row can be reached.
     * The listeners return the insets rather than consuming them, so child views still
     * receive them on older releases.
     */
    private fun applyWindowInsets() {
        val rootLeft = binding.root.paddingLeft
        val rootRight = binding.root.paddingRight
        val listBottom = binding.splitList.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = rootLeft + bars.left, right = rootRight + bars.right)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.splitList) { view, insets ->
            val bottom = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            ).bottom
            view.updatePadding(bottom = listBottom + bottom)
            insets
        }
    }

    /** Shows the filtered list. The spinner is shown while the list is still loading. */
    private fun applyFilter(query: String) {
        val allApps = viewModel.apps.value
        val needle = query.trim()
        val filtered: List<SplitAppEntry> = allApps.orEmpty().filter {
            needle.isEmpty() || it.label.contains(needle, ignoreCase = true)
        }
        adapter.items = filtered

        val loading = allApps == null
        binding.splitProgress.visibility = if (loading) View.VISIBLE else View.GONE
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
