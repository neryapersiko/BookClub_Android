package com.example.bookclub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookclub.databinding.FragmentEditPostBinding
import com.example.bookclub.di.ServiceLocator
import com.example.bookclub.ui.images.CachedImageLoader
import com.example.bookclub.ui.toolbar.bindBack
import com.example.bookclub.viewmodel.EditPostViewModel
import com.example.bookclub.viewmodel.EditPostViewModelFactory
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private val args: EditPostFragmentArgs by navArgs()

    private val viewModel: EditPostViewModel by viewModels {
        EditPostViewModelFactory(
            repository = ServiceLocator.provideRepository(requireContext()),
            imageRepository = ServiceLocator.provideImageRepository(requireContext()),
            postId = args.postId
        )
    }

    private val imageScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setSelectedImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.bindBack(findNavController())
        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnChangePostImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSavePostChanges.setOnClickListener {
            val title = binding.etEditBookTitle.text?.toString()?.trim().orEmpty()
            val author = binding.etEditBookAuthor.text?.toString()?.trim().orEmpty()
            val year = binding.etEditBookYear.text?.toString()?.trim()?.toIntOrNull()
            val content = binding.etEditContent.text?.toString()?.trim().orEmpty()

            if (title.isBlank() || content.isBlank()) {
                Toast.makeText(requireContext(), "Title and content are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.savePost(
                newTitle = title,
                newAuthor = author,
                newYear = year,
                newContent = content
            )
        }
    }

    private fun observeState() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post == null) return@observe

            if (binding.etEditBookTitle.text.isNullOrBlank()) binding.etEditBookTitle.setText(post.bookTitle)
            if (binding.etEditBookAuthor.text.isNullOrBlank()) binding.etEditBookAuthor.setText(post.bookAuthor)
            if (binding.etEditBookYear.text.isNullOrBlank() && post.bookPublishYear != null) {
                binding.etEditBookYear.setText(post.bookPublishYear.toString())
            }
            if (binding.etEditContent.text.isNullOrBlank()) binding.etEditContent.setText(post.content)

            if (viewModel.selectedImageUri.value == null) {
                if (post.bookImageUrl.isNotBlank()) {
                    CachedImageLoader.load(
                        scope = imageScope,
                        imageView = binding.ivEditPostCover,
                        cacheKey = "book:${post.id}",
                        url = post.bookImageUrl,
                        placeholder = R.drawable.book_cover_default
                    ) { it.resize(240, 360).centerCrop().onlyScaleDown() }
                } else {
                    binding.ivEditPostCover.setImageResource(R.drawable.book_cover_default)
                }
            }
        }

        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.book_cover_default)
                    .error(R.drawable.book_cover_default)
                    .into(binding.ivEditPostCover)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarEditPost.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSavePostChanges.isEnabled = !loading
            binding.btnChangePostImage.isEnabled = !loading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Post updated!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

