package com.communityos.marketplace.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.communityos.data.local.dao.UserDao
import com.communityos.data.local.session.SessionManager
import com.communityos.marketplace.domain.CreateListingUseCase
import com.communityos.marketplace.domain.GetListingDetailsUseCase
import com.communityos.marketplace.domain.UpdateListingUseCase
import com.communityos.marketplace.event.CreateEditListingEvent
import com.communityos.marketplace.state.CreateEditListingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEditListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createListingUseCase: CreateListingUseCase,
    private val updateListingUseCase: UpdateListingUseCase,
    private val getListingDetailsUseCase: GetListingDetailsUseCase,
    private val sessionManager: SessionManager,
    private val userDao: UserDao
) : ViewModel() {

    private val listingId: String? = savedStateHandle["listingId"]

    private val _state = MutableStateFlow(CreateEditListingState(listingId = listingId))
    val state: StateFlow<CreateEditListingState> = _state.asStateFlow()

    init {
        if (listingId != null) {
            loadExistingListing(listingId)
        } else {
            prefillResidentPhone()
        }
    }

    private fun prefillResidentPhone() {
        viewModelScope.launch {
            val session = sessionManager.getSession() ?: return@launch
            val user = userDao.getUserById(session.userId) ?: return@launch
            _state.update { it.copy(contactPhone = user.phoneNumber) }
        }
    }

    private fun loadExistingListing(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            getListingDetailsUseCase(id).fold(
                onSuccess = { detail ->
                    val listing = detail.listing
                    _state.update {
                        it.copy(
                            isLoading = false,
                            title = listing.title,
                            description = listing.description,
                            category = listing.category,
                            priceText = if (listing.price % 1.0 == 0.0) listing.price.toLong().toString() else listing.price.toString(),
                            contactPhone = listing.contactPhone,
                            status = listing.status
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun onEvent(event: CreateEditListingEvent) {
        when (event) {
            is CreateEditListingEvent.OnTitleChanged -> _state.update { it.copy(title = event.title, titleError = null) }
            is CreateEditListingEvent.OnDescriptionChanged -> _state.update { it.copy(description = event.description, descriptionError = null) }
            is CreateEditListingEvent.OnCategoryChanged -> _state.update { it.copy(category = event.category) }
            is CreateEditListingEvent.OnPriceChanged -> _state.update { it.copy(priceText = event.price, priceError = null) }
            is CreateEditListingEvent.OnPhoneChanged -> _state.update { it.copy(contactPhone = event.phone, phoneError = null) }
            is CreateEditListingEvent.OnStatusChanged -> _state.update { it.copy(status = event.status) }
            is CreateEditListingEvent.Submit -> submit()
            is CreateEditListingEvent.ResetSuccess -> _state.update { it.copy(isSuccess = false) }
        }
    }

    private fun submit() {
        val current = _state.value
        val cleanTitle = current.title.trim()
        val cleanDesc = current.description.trim()
        val cleanPhone = current.contactPhone.trim()
        val priceValue = current.priceText.toDoubleOrNull()

        var hasError = false
        var titleErr: String? = null
        var descErr: String? = null
        var priceErr: String? = null
        var phoneErr: String? = null

        if (cleanTitle.isBlank()) {
            titleErr = "Title cannot be empty"
            hasError = true
        }

        if (cleanDesc.isBlank()) {
            descErr = "Description cannot be empty"
            hasError = true
        }

        if (priceValue == null || priceValue <= 0.0) {
            priceErr = "Price must be greater than 0"
            hasError = true
        }

        val phoneRegex = Regex("^[6-9]\\d{9}$")
        if (cleanPhone.isBlank()) {
            phoneErr = "Contact phone cannot be empty"
            hasError = true
        } else if (!cleanPhone.matches(phoneRegex)) {
            phoneErr = "Enter a valid 10-digit mobile number"
            hasError = true
        }

        if (hasError) {
            _state.update {
                it.copy(
                    titleError = titleErr,
                    descriptionError = descErr,
                    priceError = priceErr,
                    phoneError = phoneErr
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (current.isEditMode) {
                updateListingUseCase(
                    listingId = current.listingId!!,
                    title = cleanTitle,
                    description = cleanDesc,
                    category = current.category,
                    price = priceValue!!,
                    contactPhone = cleanPhone,
                    status = current.status
                )
            } else {
                createListingUseCase(
                    title = cleanTitle,
                    description = cleanDesc,
                    category = current.category,
                    price = priceValue!!,
                    contactPhone = cleanPhone
                )
            }

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to save listing") }
                }
            )
        }
    }
}
