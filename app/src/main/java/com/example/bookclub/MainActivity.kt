package com.example.bookclub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.bookclub.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find NavHostFragment safely
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Initialize Navigation Graph dynamically
        setupNavigationGraph()
    }

    private fun setupNavigationGraph() {
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_graph)
        
        // Dynamic Start Destination: Home if logged in, Login if not
        if (FirebaseAuth.getInstance().currentUser != null) {
            graph.setStartDestination(R.id.homeFragment)
        } else {
            graph.setStartDestination(R.id.loginFragment)
        }
        
        navController.graph = graph
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
