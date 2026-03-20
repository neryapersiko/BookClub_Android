package com.example.bookclub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.example.bookclub.databinding.ActivityEditProfileBinding
import com.example.bookclub.viewmodel.EditProfileViewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()
    private var selectedLocalUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedLocalUri = it
            binding.etEditImageUrl.setText("") // Clear URL field when gallery is used
            Picasso.get().load(it).into(binding.ivEditProfileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupListeners()
        observeViewModel()
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Profile"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupListeners() {
        // Image selection via clicking the ImageView or the button
        binding.ivEditProfileImage.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnChangeImage.setOnClickListener { galleryLauncher.launch("image/*") }

        // Auto-preview for URL
        binding.etEditImageUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val url = s.toString().trim()
                if (url.isNotEmpty()) {
                    selectedLocalUri = null // Clear local URI when URL is typed
                    Picasso.get().load(url)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(binding.ivEditProfileImage)
                }
            }
        })

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditName.text.toString().trim()
            val webUrl = binding.etEditImageUrl.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateProfile(name, selectedLocalUri, webUrl.ifEmpty { null })
        }
    }

    private fun observeViewModel() {
        viewModel.userData.observe(this) { data ->
            binding.etEditName.setText(data["name"])
            val imageUrl = data["profileImageUrl"]
            if (!imageUrl.isNullOrEmpty() && selectedLocalUri == null && binding.etEditImageUrl.text.isNullOrEmpty()) {
                Picasso.get().load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivEditProfileImage)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarEdit.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(this) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Update failed: ${it.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetUpdateResult()
                }
            }
        }
    }
}
