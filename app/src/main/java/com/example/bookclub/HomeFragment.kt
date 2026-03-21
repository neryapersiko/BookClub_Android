package com.example.bookclub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.FragmentHomeBinding
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.viewmodel.HomeViewModel
import com.example.bookclub.viewmodel.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()
    
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure we handle authentication before setting up the UI
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun navigateToLogin() {
        // Use the Intent for Activity-based Login if that's your structure,
        // or use NavController if you're using Fragments.
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid
        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onLikeClick = { post ->
                currentUserId?.let { uid ->
                    viewModel.likePost(post.id, uid)
                }
            },
            onCommentClick = { post ->
                val intent = Intent(requireContext(), CommentsActivity::class.java)
                intent.putExtra("POST_ID", post.id)
                intent.putExtra("USER_NAME", post.userName)
                intent.putExtra("BOOK_TITLE", post.bookTitle)
                intent.putExtra("CONTENT", post.content)
                
                // Pass the profile image URL to CommentsActivity
                val imageUrl = post.profileImageUrl.ifEmpty { post.userImageUrl }
                intent.putExtra("USER_IMAGE_URL", imageUrl)
                
                startActivity(intent)
            }
        )
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnAddPost.setOnClickListener {
            val intent = Intent(requireContext(), AddPostActivity::class.java)
            startActivity(intent)
        }

        binding.btnProfile.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: ""
            val action = HomeFragmentDirections.actionHomeFragmentToProfileFragment(currentUserId)
            findNavController().navigate(action)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            // If posts are empty, this might be why you see a "white screen" (empty list)
            postAdapter.submitList(posts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
