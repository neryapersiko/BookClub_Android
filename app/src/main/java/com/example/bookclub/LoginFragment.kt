package com.example.bookclub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bookclub.database.AppDatabase
import com.example.bookclub.databinding.ActivityLoginBinding
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.viewmodel.AuthViewModel
import com.example.bookclub.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        AuthViewModelFactory(BookRepository(database.postDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.btnLogin.isEnabled = !state.isLoading

                if (state.isLoggedIn) {
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }

                state.errorMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
