package com.clearchain.app.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.auth.RegisterUseCase
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> {
                _state.update { it.copy(name = event.name, nameError = null) }
            }

            is RegisterEvent.TypeChanged -> {
                _state.update { it.copy(type = event.type) }
            }

            is RegisterEvent.EmailChanged -> {
                _state.update { it.copy(email = event.email, emailError = null) }
            }

            is RegisterEvent.PasswordChanged -> {
                _state.update { it.copy(password = event.password, passwordError = null) }
            }

            is RegisterEvent.ConfirmPasswordChanged -> {
                _state.update { it.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null) }
            }

            is RegisterEvent.PhoneChanged -> {
                _state.update { it.copy(phone = event.phone, phoneError = null) }
            }

            is RegisterEvent.AddressChanged -> {
                _state.update { it.copy(address = event.address, addressError = null) }
            }

            is RegisterEvent.LocationChanged -> {
                _state.update { it.copy(location = event.location, locationError = null) }
            }

            is RegisterEvent.HoursChanged -> {
                _state.update { it.copy(hours = event.hours) }
            }

            RegisterEvent.Register -> {
                register()
            }

            RegisterEvent.NavigateToLogin -> {
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.NavigateUp)
                }
            }

            RegisterEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun register() {
        val currentState = _state.value

        // Validate
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = registerUseCase(
                name = currentState.name,
                type = currentState.type,
                email = currentState.email,
                password = currentState.password,
                phone = currentState.phone,
                address = currentState.address,
                location = currentState.location,
                hours = currentState.hours.ifBlank { null }
            )

            result.fold(
                onSuccess = { (user, tokens) ->
                    _state.update { it.copy(isLoading = false) }

                    // Navigate based on user type
                    val route = when (user.type.name.lowercase()) {
                        "grocery" -> "grocery_dashboard"
                        "ngo" -> "ngo_dashboard"
                        "admin" -> "admin_dashboard"
                        else -> "login"
                    }

                    _uiEvent.send(UiEvent.Navigate(route))
                    _uiEvent.send(UiEvent.ShowSnackbar("Welcome to ClearChain, ${user.name}!"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Registration failed"
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Registration failed"))
                }
            )
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

        // Validate name
        if (currentState.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
            isValid = false
        } else if (currentState.name.length < 3) {
            _state.update { it.copy(nameError = "Name must be at least 3 characters") }
            isValid = false
        }

        // Validate email
        if (currentState.email.isBlank()) {
            _state.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!ValidationUtils.isValidEmail(currentState.email)) {
            _state.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }

        // Validate password
        if (currentState.password.isBlank()) {
            _state.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (!ValidationUtils.isValidPassword(currentState.password)) {
            _state.update {
                it.copy(passwordError = "Password must be at least 8 characters with uppercase, lowercase, and number")
            }
            isValid = false
        }

        // Validate confirm password
        if (currentState.confirmPassword.isBlank()) {
            _state.update { it.copy(confirmPasswordError = "Please confirm your password") }
            isValid = false
        } else if (currentState.password != currentState.confirmPassword) {
            _state.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }

        // Validate phone
        if (currentState.phone.isBlank()) {
            _state.update { it.copy(phoneError = "Phone number is required") }
            isValid = false
        } else if (!ValidationUtils.isValidPhone(currentState.phone)) {
            _state.update { it.copy(phoneError = "Invalid phone number") }
            isValid = false
        }

        // Validate address
        if (currentState.address.isBlank()) {
            _state.update { it.copy(addressError = "Address is required") }
            isValid = false
        }

        // Validate location
        if (currentState.location.isBlank()) {
            _state.update { it.copy(locationError = "Location is required") }
            isValid = false
        }

        return isValid
    }
}