package de.gerritstapper.bikegarage.feature.part

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.gerritstapper.bikegarage.R
import de.gerritstapper.bikegarage.domain.model.Part
import de.gerritstapper.bikegarage.domain.usecase.DeletePartResult
import de.gerritstapper.bikegarage.domain.usecase.DeletePartUseCase
import de.gerritstapper.bikegarage.domain.usecase.PartDeletionInspection
import de.gerritstapper.bikegarage.ui.navigation.PART_ID_ARGUMENT
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class PartDeletionError {
    DELETE_FAILED,
    PART_NOT_FOUND,
}

internal data class PartDeletionUiState(
    val part: Part? = null,
    val showConfirmation: Boolean = false,
    val error: PartDeletionError? = null,
    val isChecking: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val isBusy: Boolean
        get() = isChecking || isDeleting
}

internal sealed interface PartDeletionEvent {
    data object Deleted : PartDeletionEvent
}

@HiltViewModel
class PartDeletionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deletePart: DeletePartUseCase,
) : ViewModel() {
    private val partId = checkNotNull(savedStateHandle.get<Long>(PART_ID_ARGUMENT))

    private val _uiState = MutableStateFlow(PartDeletionUiState())
    internal val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PartDeletionEvent>()
    internal val events = _events.asSharedFlow()

    fun requestDeletion() {
        if (_uiState.value.isBusy) return

        _uiState.update {
            it.copy(
                part = null,
                showConfirmation = false,
                error = null,
                isChecking = true,
            )
        }
        viewModelScope.launch {
            try {
                when (val inspection = deletePart.inspect(partId)) {
                    is PartDeletionInspection.Ready -> _uiState.update {
                        it.copy(
                            part = inspection.part,
                            showConfirmation = true,
                            isChecking = false,
                        )
                    }

                    PartDeletionInspection.NotFound -> _uiState.update {
                        it.copy(error = PartDeletionError.PART_NOT_FOUND, isChecking = false)
                    }
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update {
                    it.copy(error = PartDeletionError.DELETE_FAILED, isChecking = false)
                }
            }
        }
    }

    fun dismissConfirmation() {
        if (!_uiState.value.isDeleting) {
            _uiState.update { it.copy(showConfirmation = false) }
        }
    }

    fun confirmDeletion() {
        val state = _uiState.value
        if (state.isBusy || !state.showConfirmation) return

        _uiState.update {
            it.copy(showConfirmation = false, error = null, isDeleting = true)
        }
        viewModelScope.launch {
            when (val result = deletePart.delete(partId)) {
                is DeletePartResult.Deleted -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.emit(PartDeletionEvent.Deleted)
                }

                DeletePartResult.NotFound -> _uiState.update {
                    it.copy(error = PartDeletionError.PART_NOT_FOUND, isDeleting = false)
                }

                DeletePartResult.Failed -> _uiState.update {
                    it.copy(error = PartDeletionError.DELETE_FAILED, isDeleting = false)
                }
            }
        }
    }
}

internal fun PartDeletionError.messageRes(): Int = when (this) {
    PartDeletionError.DELETE_FAILED -> R.string.part_delete_failed
    PartDeletionError.PART_NOT_FOUND -> R.string.part_not_found
}
