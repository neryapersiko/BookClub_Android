package com.example.bookclub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bookclub.databinding.ActivityRegisterBinding
import com.example.bookclub.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private var selectedLocalUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedLocalUri = it
            binding.etImageUrl.text?.clear() // Clear URL if gallery is used
            Glide.with(this).load(it).circleCrop().into(binding.ivProfilePreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnPreviewUrl.setOnClickListener {
            val url = binding.etImageUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                selectedLocalUri = null // Clear local URI if URL is used
                Glide.with(this).load(url).circleCrop().placeholder(android.R.drawable.ic_menu_gallery).into(binding.ivProfilePreview)
            } else {
                Toast.makeText(this, "Please enter a URL first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val webUrl = binding.etImageUrl.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerUser(name, email, password, selectedLocalUri, webUrl.ifEmpty { null })
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }

        viewModel.registrationStatus.observe(this) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${it.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetStatus()
                }
            }
        }
    }
}
