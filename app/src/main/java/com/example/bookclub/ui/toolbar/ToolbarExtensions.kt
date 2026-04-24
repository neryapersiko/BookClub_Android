package com.example.bookclub.ui.toolbar

import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController

fun Toolbar.bindBack(navController: NavController) {
    setNavigationOnClickListener { navController.popBackStack() }
}

