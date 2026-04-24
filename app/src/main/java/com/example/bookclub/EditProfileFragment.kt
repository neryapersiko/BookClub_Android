package com.example.bookclub

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.example.bookclub.databinding.FragmentEditProfileBinding
import com.example.bookclub.di.ServiceLocator
import com.example.bookclub.ui.toolbar.bindBack
import com.example.bookclub.viewmodel.EditProfileViewModel
import com.example.bookclub.viewmodel.EditProfileViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EditProfileViewModel by viewModels {
        EditProfileViewModelFactory(requireContext())
    }
    private var selectedLocalUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedLocalUri = it
            Picasso.get()
                .load(it)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .into(binding.ivEditProfileImage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.toolbar.bindBack(findNavController())

        binding.ivEditProfileImage.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnChangeImage.setOnClickListener { galleryLauncher.launch("image/*") }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateProfile(name, selectedLocalUri)
        }
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { data ->
            binding.etEditName.setText(data["name"])
            val imageUrl = data["profileImageUrl"]
            if (!imageUrl.isNullOrEmpty() && selectedLocalUri == null) {
                refreshProfileImage(imageUrl)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarEdit.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    // Brute-force: invalidate Picasso memory/disk caches for this profile key
                    val uid = ServiceLocator.provideRepository(requireContext()).getCurrentUserId()
                    if (uid != null) {
                        // Invalidate both the latest file URI (if available) and the remote URL (if available)
                        viewLifecycleOwner.lifecycleScope.launch {
                            val remoteUrl = viewModel.userData.value?.get("profileImageUrl") ?: ""
                            val localUri = if (remoteUrl.isNotBlank()) {
                                ServiceLocator.provideImageRepository(requireContext())
                                    .getOrFetchLocalUri(key = "profile:$uid", url = remoteUrl)
                            } else null
                            localUri?.let { Picasso.get().invalidate(it) }
                            if (remoteUrl.isNotBlank()) Picasso.get().invalidate(remoteUrl)
                        }
                    }
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Update failed: ${it.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetUpdateResult()
                }
            }
        }
    }

    private fun refreshProfileImage(newUrl: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            binding.ivEditProfileImage.post {
                val uid = ServiceLocator.provideRepository(requireContext()).getCurrentUserId()
                val key = if (uid != null) "profile:$uid" else "url:$newUrl"
                viewLifecycleOwner.lifecycleScope.launch {
                    val localUri = ServiceLocator.provideImageRepository(requireContext())
                        .getOrFetchLocalUri(key = key, url = newUrl)
                    val request = if (localUri != null) Picasso.get().load(localUri) else Picasso.get().load(newUrl)
                    request
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .placeholder(R.drawable.avatar_default)
                        .error(R.drawable.avatar_default)
                        .into(binding.ivEditProfileImage)
                }
                binding.root.invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
