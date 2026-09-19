package com.communityos.complaints.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.complaints.domain.CreateComplaintUseCase
import com.communityos.complaints.event.CreateComplaintEvent
import com.communityos.complaints.state.CreateComplaintState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateComplaintViewModel @Inject constructor(
    private val createComplaintUseCase: CreateComplaintUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateComplaintState())
    val state: StateFlow<CreateComplaintState> = _state.asStateFlow()

    fun onEvent(event: CreateComplaintEvent) {
        when (event) {
            is CreateComplaintEvent.OnCategoryChanged -> {
                _state.update {
                    it.copy(
                        category = event.category,
                        categoryError = null,
                        generalError = null
                    )
                }
            }
            is CreateComplaintEvent.OnDescriptionChanged -> {
                _state.update {
                    it.copy(
                        description = event.description,
                        descriptionError = null,
                        generalError = null
                    )
                }
            }
            is CreateComplaintEvent.Submit -> submitComplaint()
            is CreateComplaintEvent.ResetSuccess -> {
                _state.update { it.copy(isSuccess = false) }
            }
        }
    }

    private fun submitComplaint() {
        val current = _state.value
        val category = current.category.trim()
        val description = current.description.trim()

        var hasError = false
        var catErr: String? = null
        var descErr: String? = null

        if (category.isBlank()) {
            catErr = "Please select or enter a category"
            hasError = true
        }

        if (description.isBlank()) {
            descErr = "Description cannot be empty"
            hasError = true
        }

        if (hasError) {
            _state.update { it.copy(categoryError = catErr, descriptionError = descErr) }
            return
        }

        if (current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, generalError = null) }
            val result = createComplaintUseCase(category, description)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSaving = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            generalError = error.message ?: "Failed to create complaint"
                        )
                    }
                }
            )
        }
    }
}
