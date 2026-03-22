package com.example.bookclub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookclub.databinding.ActivityAddPostBinding
import com.example.bookclub.viewmodel.CreatePostViewModel

class AddPostFragment : Fragment() {

    private var _binding: ActivityAddPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar()
        setupListeners()
        observeViewModel()
    }

    private fun setupActionBar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupListeners() {
        binding.btnPublish.setOnClickListener {
            val bookTitle = binding.etBookTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()

            if (bookTitle.isEmpty() || content.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.savePost(bookTitle, content)
        }
    }

    private fun observeViewModel() {
        viewModel.postSaved.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Post Published Successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPublish.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
