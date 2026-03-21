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
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.example.bookclub.databinding.FragmentEditProfileBinding
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
            Picasso.get().load(it).into(binding.ivEditProfileImage)
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
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

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
                Picasso.get().invalidate(newUrl)
                Picasso.get().load(newUrl)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivEditProfileImage)
                binding.root.invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
