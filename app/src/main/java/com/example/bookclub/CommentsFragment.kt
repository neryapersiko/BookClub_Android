package com.example.bookclub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookclub.adapter.CommentAdapter
import com.example.bookclub.databinding.ActivityCommentsBinding
import com.example.bookclub.di.ServiceLocator
import com.example.bookclub.ui.toolbar.bindBack
import com.example.bookclub.viewmodel.CommentsViewModel
import com.example.bookclub.viewmodel.CommentsViewModelFactory
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class CommentsFragment : Fragment() {

    private var _binding: ActivityCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentAdapter: CommentAdapter
    
    private val args: CommentsFragmentArgs by navArgs()
    
    private val viewModel: CommentsViewModel by viewModels {
        CommentsViewModelFactory(ServiceLocator.provideRepository(requireContext()), args.postId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar()
        setupHeader()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupActionBar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.bindBack(findNavController())
        binding.toolbar.title = "Comments"
    }

    private fun setupHeader() {
        binding.tvHeaderBookTitle.text = args.bookTitle
        binding.tvHeaderUserName.text = args.userName
        binding.tvHeaderContent.text = args.content

        // User profile image
        if (args.userImageUrl.isNotEmpty()) {
            val key = "url:${args.userImageUrl}"
            viewLifecycleOwner.lifecycleScope.launch {
                val localUri = ServiceLocator.provideImageRepository(requireContext())
                    .getOrFetchLocalUri(key = key, url = args.userImageUrl)
                val request = if (localUri != null) {
                    Picasso.get().load(localUri)
                } else {
                    Picasso.get().load(args.userImageUrl)
                }
                request
                    .placeholder(R.drawable.avatar_default)
                    .error(R.drawable.avatar_default)
                    .resize(120, 120)
                    .centerCrop()
                    .into(binding.ivHeaderProfile)
            }
        } else {
            binding.ivHeaderProfile.setImageResource(R.drawable.avatar_default)
        }

        // Book author
        if (args.bookAuthor.isNotEmpty()) {
            binding.tvHeaderBookAuthor.text = "by ${args.bookAuthor}"
            binding.tvHeaderBookAuthor.visibility = View.VISIBLE
        } else {
            binding.tvHeaderBookAuthor.visibility = View.GONE
        }

        // Book publish year (0 means not set)
        if (args.bookPublishYear != 0) {
            binding.tvHeaderBookPublishYear.text = "(${args.bookPublishYear})"
            binding.tvHeaderBookPublishYear.visibility = View.VISIBLE
        } else {
            binding.tvHeaderBookPublishYear.visibility = View.GONE
        }

        // Book cover image
        if (args.bookImageUrl.isNotEmpty()) {
            val coverUrl = args.bookImageUrl.replace("http://", "https://")
            val key = "url:$coverUrl"
            viewLifecycleOwner.lifecycleScope.launch {
                val localUri = ServiceLocator.provideImageRepository(requireContext())
                    .getOrFetchLocalUri(key = key, url = coverUrl)
                val request = if (localUri != null) {
                    Picasso.get().load(localUri)
                } else {
                    Picasso.get().load(coverUrl)
                }
                request
                    .placeholder(R.drawable.book_cover_default)
                    .error(R.drawable.book_cover_default)
                    .resize(180, 270)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivHeaderBookCover)
            }
        } else {
            binding.ivHeaderBookCover.setImageResource(R.drawable.book_cover_default)
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onEditClick = { comment -> showEditDialog(comment.id, comment.content) },
            onDeleteClick = { comment -> viewModel.deleteComment(comment.id) }
        )
        binding.rvComments.adapter = commentAdapter
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendComment(text)
                binding.etComment.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarComments.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.isSending.observe(viewLifecycleOwner) { sending ->
            binding.btnSend.isEnabled = !sending
            binding.etComment.isEnabled = !sending
        }

        viewModel.comments.observe(viewLifecycleOwner) { list ->
            commentAdapter.submitList(list)
            if (list.isNotEmpty()) {
                binding.rvComments.scrollToPosition(list.size - 1)
            }
        }
        
        viewModel.operationStatus.observe(viewLifecycleOwner) { result ->
            result.onFailure {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(commentId: String, currentText: String) {
        val editText = EditText(requireContext())
        editText.setText(currentText)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Comment")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    viewModel.editComment(commentId, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
