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
import androidx.navigation.fragment.findNavController
import com.example.bookclub.databinding.ActivityAddPostBinding
import com.example.bookclub.viewmodel.CreatePostViewModel
import com.squareup.picasso.Picasso

class AddPostFragment : Fragment() {

    private var _binding: ActivityAddPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels()
    private var selectedBookImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedBookImageUri = it
            binding.ivBookCoverPreview.visibility = View.VISIBLE
            Picasso.get().load(it).into(binding.ivBookCoverPreview)
        }
    }

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
        binding.btnAutoFill.setOnClickListener {
            val title = binding.etBookTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a book title first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.fetchBookDetails(title)
        }

        binding.btnUploadBookImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnPublish.setOnClickListener {
            val bookTitle = binding.etBookTitle.text.toString().trim()
            val bookAuthor = binding.etBookAuthor.text.toString().trim()
            val bookPublishYear = binding.etBookPublishYear.text.toString().trim().toIntOrNull()
            val content = binding.etContent.text.toString().trim()

            if (bookTitle.isEmpty() || content.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in title and review", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.savePost(bookTitle, bookAuthor, bookPublishYear, content, selectedBookImageUri)
        }
    }

    private fun observeViewModel() {
        viewModel.bookDetails.observe(viewLifecycleOwner) { details ->
            details?.let {
                binding.etBookAuthor.setText(it.author)
                if (it.publishYear != null) {
                    binding.etBookPublishYear.setText(it.publishYear.toString())
                }
                if (it.imageUrl.isNotEmpty() && selectedBookImageUri == null) {
                    val httpsUrl = it.imageUrl.replace("http://", "https://")
                    binding.ivBookCoverPreview.visibility = View.VISIBLE
                    Picasso.get().load(httpsUrl).into(binding.ivBookCoverPreview)
                }
            }
        }

        viewModel.isAutoFillLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarAutoFill.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnAutoFill.isEnabled = !loading
        }

        viewModel.autoFillError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

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
