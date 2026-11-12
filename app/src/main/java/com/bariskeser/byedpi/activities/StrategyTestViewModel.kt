package com.bariskeser.byedpi.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bariskeser.byedpi.core.StrategyCandidate
import com.bariskeser.byedpi.core.StrategyOutcome
import com.bariskeser.byedpi.core.StrategyResult
import com.bariskeser.byedpi.core.StrategyTester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The status line shown above the progress bar. Testing names are resolved by the view. */
enum class StrategyStatus { Idle, Testing, Stopping, Done, NoSuccess, Cancelled }

data class StrategyUiState(
    val candidates: List<StrategyCandidate> = emptyList(),
    val results: List<StrategyResult?> = emptyList(),
    val testingIndex: Int? = null,
    val bestIndex: Int? = null,
    val completed: Int = 0,
    val running: Boolean = false,
    val status: StrategyStatus = StrategyStatus.Idle,
)

/**
 * Holds the strategy test so it survives configuration changes. The test runs in
 * [viewModelScope], so a rotation does not restart it. Clearing the ViewModel
 * cancels the test, which stops the in-flight proxy.
 */
class StrategyTestViewModel : ViewModel() {
    private val _state = MutableStateFlow(StrategyUiState())
    val state: StateFlow<StrategyUiState> = _state.asStateFlow()

    private var testJob: Job? = null

    /** Sets the candidate list once. Later calls keep the existing results. */
    fun ensureCandidates(candidates: List<StrategyCandidate>) {
        if (_state.value.candidates.isEmpty()) {
            _state.value = StrategyUiState(
                candidates = candidates,
                results = List(candidates.size) { null },
            )
        }
    }

    /** Resets every row to pending and starts testing [host] against all candidates. */
    fun start(host: String) {
        val current = _state.value
        if (current.running || current.candidates.isEmpty()) return

        _state.value = current.copy(
            results = List(current.candidates.size) { null },
            testingIndex = null,
            bestIndex = null,
            completed = 0,
            running = true,
            status = StrategyStatus.Idle,
        )

        val candidates = current.candidates
        val tester = StrategyTester(host)
        testJob = viewModelScope.launch {
            var failed = false
            try {
                tester.run(
                    candidates = candidates,
                    onStart = { index ->
                        _state.update { it.copy(testingIndex = index, status = StrategyStatus.Testing) }
                    },
                    onResult = { index, result ->
                        _state.update { s ->
                            val results = s.results.toMutableList()
                            results[index] = result
                            s.copy(
                                results = results,
                                testingIndex = null,
                                completed = s.completed + 1,
                                bestIndex = bestIndex(results),
                            )
                        }
                    },
                )
                _state.update {
                    it.copy(
                        status = if (it.bestIndex != null) {
                            StrategyStatus.Done
                        } else {
                            StrategyStatus.NoSuccess
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
                _state.update { it.copy(status = StrategyStatus.NoSuccess) }
            } finally {
                _state.update { s ->
                    val status = if (!failed && s.completed < s.candidates.size) {
                        StrategyStatus.Cancelled
                    } else {
                        s.status
                    }
                    s.copy(testingIndex = null, running = false, status = status)
                }
            }
        }
    }

    /** Asks the running test to stop. The finally block in [start] finishes the state. */
    fun stop() {
        if (!_state.value.running) return
        _state.update { it.copy(status = StrategyStatus.Stopping) }
        // Cancelling stops the current proxy through StrategyTester's finally block.
        testJob?.cancel()
    }

    /** Best = lowest latency among successful candidates. */
    private fun bestIndex(results: List<StrategyResult?>): Int? =
        results.indices
            .filter { results[it]?.outcome == StrategyOutcome.Success }
            .minByOrNull { results[it]?.latencyMs ?: Long.MAX_VALUE }
}
