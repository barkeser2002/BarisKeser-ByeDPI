package com.bariskeser.byedpi.activities

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bariskeser.byedpi.R
import com.bariskeser.byedpi.core.StrategyCandidate
import com.bariskeser.byedpi.core.StrategyOutcome
import com.bariskeser.byedpi.core.StrategyResult
import com.bariskeser.byedpi.core.StrategyTester
import com.bariskeser.byedpi.data.AppStatus
import com.bariskeser.byedpi.databinding.ActivityStrategyTestBinding
import com.bariskeser.byedpi.databinding.ItemStrategyBinding
import com.bariskeser.byedpi.services.appStatus
import com.bariskeser.byedpi.utility.getPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var testJob: Job? = null
    private var running = false

    private var candidates: List<StrategyCandidate> = emptyList()
    private val itemViews = mutableListOf<ItemStrategyBinding>()
    private var results: Array<StrategyResult?> = emptyArray()
    private var testingIndex: Int? = null
    private var bestIndex: Int? = null
    private var completed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStrategyTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.st_title)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(
                ColorDrawable(ContextCompat.getColor(this@StrategyTestActivity, R.color.th_bg))
            )
        }

        candidates = buildCandidates()
        renderList()

        binding.startButton.setOnClickListener {
            if (running) stopTest() else startTest()
        }
        binding.applyButton.setOnClickListener { applyBest() }
    }

    override fun onResume() {
        super.onResume()
        if (!running) refreshAvailability()
    }

    override fun onDestroy() {
        // Cancelling stops the in-flight proxy through StrategyTester's finally block.
        testJob?.cancel()
        super.onDestroy()
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
                name = presetNames.getOrElse(index) { getString(R.string.st_extra_name, index + 1) },
                args = args.trim(),
            )
        }
        val extras = EXTRA_ARGS.mapIndexed { index, args ->
            StrategyCandidate(name = getString(R.string.st_extra_name, index + 1), args = args)
        }
        return (fromPresets + extras).distinctBy { it.args }
    }

    /** Rebuilds the candidate list with every row pending. */
    private fun renderList() {
        binding.resultsContainer.removeAllViews()
        itemViews.clear()
        results = arrayOfNulls(candidates.size)
        testingIndex = null
        bestIndex = null
        completed = 0

        candidates.forEachIndexed { index, candidate ->
            val item = ItemStrategyBinding.inflate(layoutInflater, binding.resultsContainer, false)
            item.itemName.text = candidate.name
            item.itemArgs.text = candidate.args
            binding.resultsContainer.addView(item.root)
            itemViews += item
            showItem(index)
        }

        binding.progressBar.max = candidates.size
        binding.progressBar.progress = 0
        binding.progressText.text = getString(R.string.st_progress, 0, candidates.size)
        updateApplyButton()
    }

    private fun refreshAvailability() {
        val halted = appStatus.first == AppStatus.Halted
        binding.startButton.isEnabled = halted
        if (!halted) {
            binding.statusText.setText(R.string.st_blocked)
        }
    }

    private fun startTest() {
        val host = normalizeHost(binding.hostInput.text?.toString().orEmpty())
        if (host.isEmpty()) {
            Toast.makeText(this, R.string.st_host_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (appStatus.first != AppStatus.Halted) {
            refreshAvailability()
            return
        }

        renderList()
        setRunning(true)
        binding.statusText.setText(R.string.st_idle)

        val tester = StrategyTester(host)
        testJob = lifecycleScope.launch {
            var failed = false
            try {
                tester.run(
                    candidates = candidates,
                    onStart = { index ->
                        testingIndex = index
                        binding.statusText.text =
                            getString(R.string.st_testing_name, candidates[index].name)
                        showItem(index)
                    },
                    onResult = { index, result ->
                        results[index] = result
                        testingIndex = null
                        completed++
                        binding.progressBar.progress = completed
                        binding.progressText.text =
                            getString(R.string.st_progress, completed, candidates.size)
                        refreshBest()
                    },
                )
                binding.statusText.setText(
                    if (bestIndex != null) R.string.st_done else R.string.st_no_success
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
                binding.statusText.setText(R.string.st_no_success)
            } finally {
                if (!failed && completed < candidates.size) {
                    binding.statusText.setText(R.string.st_cancelled)
                }
                testingIndex = null
                setRunning(false)
            }
        }
    }

    private fun stopTest() {
        binding.statusText.setText(R.string.st_stopping)
        testJob?.cancel()
    }

    private fun setRunning(value: Boolean) {
        running = value
        binding.startButton.setText(if (value) R.string.st_stop else R.string.st_start)
        binding.startButton.isEnabled = value || appStatus.first == AppStatus.Halted
        binding.hostInput.isEnabled = !value
        updateApplyButton()
    }

    /** Best = lowest latency among successful candidates. */
    private fun refreshBest() {
        bestIndex = results.indices
            .filter { results[it]?.outcome == StrategyOutcome.Success }
            .minByOrNull { results[it]?.latencyMs ?: Long.MAX_VALUE }
        itemViews.indices.forEach { showItem(it) }
        updateApplyButton()
    }

    private fun updateApplyButton() {
        binding.applyButton.isEnabled = !running && bestIndex != null
    }

    private fun showItem(index: Int) {
        val item = itemViews.getOrNull(index) ?: return
        val result = results.getOrNull(index)

        if (result == null) {
            if (testingIndex == index) {
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

        item.itemBadge.visibility = if (index == bestIndex) View.VISIBLE else View.GONE
    }

    private fun setResultText(item: ItemStrategyBinding, text: String, colorRes: Int) {
        val view: TextView = item.itemResult
        view.text = text
        view.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun applyBest() {
        val index = bestIndex ?: return
        val candidate = candidates[index]

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
