package com.bariskeser.byedpi.activities

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.core.StrategyCandidate
import com.bariskeser.byedpi.core.StrategyOutcome
import com.bariskeser.byedpi.data.AppStatus
import com.bariskeser.byedpi.databinding.ActivityStrategyTestBinding
import com.bariskeser.byedpi.databinding.ItemStrategyBinding
import com.bariskeser.byedpi.services.appStatus
import com.bariskeser.byedpi.utility.getPreferences
import kotlinx.coroutines.launch

class StrategyTestActivity : AppCompatActivity() {
    companion object {
        // Candidates beyond the Turkey presets. Duplicates of a preset are dropped.
        private val EXTRA_ARGS = listOf(
            "--split 1 --disorder 3+s --mod-http=h,d --auto=torst --tlsrec 1+s",
            "--disorder 1 --tlsrec 1+s",
            "--split 2 --disorder 1 --tlsrec 3+s",
            "--oob 1 --split 2",
            "--fake -1 --ttl 8",
        )
    }

    private lateinit var binding: ActivityStrategyTestBinding
    private val viewModel: StrategyTestViewModel by viewModels()

    /** Row views for the current candidate list. Rebuilt when the list changes. */
    private val itemViews = mutableListOf<ItemStrategyBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark-only, so the system bars always use light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        binding = ActivityStrategyTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.st_title)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(this@StrategyTestActivity, R.color.th_bg))
            )
        }

        viewModel.ensureCandidates(buildCandidates())

        binding.startButton.setOnClickListener {
            if (viewModel.state.value.running) viewModel.stop() else startTest()
        }
        binding.applyButton.setOnClickListener { applyBest() }

        // Collect in STARTED so the rows are rebuilt from the ViewModel after a
        // recreation, and so nothing updates while the screen is in the background.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check the VPN/proxy status when returning from another screen.
        render(viewModel.state.value)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }

    private fun buildCandidates(): List<StrategyCandidate> {
        val presetNames = resources.getStringArray(R.array.turkey_presets)
        val presetArgs = resources.getStringArray(R.array.turkey_presets_values)

        val fromPresets = presetArgs.mapIndexed { index, args ->
            StrategyCandidate(
                name = presetNames.getOrElse(index) { extraStrategyName(index + 1) },
                args = args.trim(),
            )
        }
        val extras = EXTRA_ARGS.mapIndexed { index, args ->
            StrategyCandidate(name = extraStrategyName(index + 1), args = args)
        }
        return (fromPresets + extras).distinctBy { it.args }
    }

    /** Formats the numbered fallback name, e.g. "Extra strategy 2". */
    private fun extraStrategyName(number: Int): String =
        getString(R.string.st_extra_name, number)

    private fun startTest() {
        val host = normalizeHost(binding.hostInput.text?.toString().orEmpty())
        if (host.isEmpty()) {
            Toast.makeText(this, R.string.st_host_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (appStatus.first != AppStatus.Halted) {
            render(viewModel.state.value)
            return
        }
        viewModel.start(host)
    }

    /** Draws the whole screen from [state]. Safe to call repeatedly. */
    private fun render(state: StrategyUiState) {
        if (itemViews.size != state.candidates.size) {
            rebuildRows(state.candidates)
        }
        state.candidates.forEachIndexed { index, _ ->
            renderRow(index, state)
        }

        binding.progressBar.max = state.candidates.size
        binding.progressBar.progress = state.completed
        binding.progressText.text = getString(R.string.st_progress, state.completed, state.candidates.size)

        binding.statusText.text = statusText(state)

        binding.startButton.setText(if (state.running) R.string.st_stop else R.string.st_start)
        binding.startButton.isEnabled = state.running || appStatus.first == AppStatus.Halted
        binding.hostInput.isEnabled = !state.running
        binding.applyButton.isEnabled = !state.running && state.bestIndex != null
    }

    /** Creates one row per candidate. Called only when the list size changes. */
    private fun rebuildRows(candidates: List<StrategyCandidate>) {
        binding.resultsContainer.removeAllViews()
        itemViews.clear()
        candidates.forEach { candidate ->
            val item = ItemStrategyBinding.inflate(layoutInflater, binding.resultsContainer, false)
            item.itemName.text = candidate.name
            item.itemArgs.text = candidate.args
            binding.resultsContainer.addView(item.root)
            itemViews += item
        }
    }

    private fun statusText(state: StrategyUiState): String = when (state.status) {
        StrategyStatus.Idle ->
            if (appStatus.first == AppStatus.Halted) {
                getString(R.string.st_idle)
            } else {
                getString(R.string.st_blocked)
            }

        StrategyStatus.Testing -> state.testingIndex
            ?.let { getString(R.string.st_testing_name, state.candidates[it].name) }
            ?: getString(R.string.st_idle)

        StrategyStatus.Stopping -> getString(R.string.st_stopping)
        StrategyStatus.Done -> getString(R.string.st_done)
        StrategyStatus.NoSuccess -> getString(R.string.st_no_success)
        StrategyStatus.Cancelled -> getString(R.string.st_cancelled)
    }

    private fun renderRow(index: Int, state: StrategyUiState) {
        val item = itemViews.getOrNull(index) ?: return
        val result = state.results.getOrNull(index)

        if (result == null) {
            if (state.testingIndex == index) {
                setResultText(item, getString(R.string.st_testing), R.color.th_text2)
            } else {
                setResultText(item, getString(R.string.st_pending), R.color.th_text3)
            }
        } else {
            when (result.outcome) {
                StrategyOutcome.Success -> setResultText(
                    item,
                    getString(R.string.st_latency, result.latencyMs ?: 0L),
                    R.color.th_green,
                )

                StrategyOutcome.Failed -> setResultText(
                    item,
                    getString(R.string.st_failed),
                    R.color.th_text3,
                )

                StrategyOutcome.Timeout -> setResultText(
                    item,
                    getString(R.string.st_timeout),
                    R.color.th_text3,
                )
            }
        }

        item.itemBadge.visibility = if (index == state.bestIndex) View.VISIBLE else View.GONE
    }

    private fun setResultText(item: ItemStrategyBinding, text: String, colorRes: Int) {
        val view: TextView = item.itemResult
        view.text = text
        view.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun applyBest() {
        val state = viewModel.state.value
        val index = state.bestIndex ?: return
        val candidate = state.candidates[index]

        getPreferences().edit()
            .putString("byedpi_cmd_args", candidate.args)
            .putBoolean("byedpi_enable_cmd_settings", true)
            .apply()

        Toast.makeText(
            this,
            getString(R.string.st_applied, candidate.name),
            Toast.LENGTH_SHORT,
        ).show()
        finish()
    }

    private fun normalizeHost(raw: String): String =
        raw.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
}
