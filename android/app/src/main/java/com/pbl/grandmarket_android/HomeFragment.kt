package com.pbl.grandmarket_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val role = UserSession.getRole(requireContext())
        val layoutRes = if (role == UserRole.SELLER) {
            R.layout.fragment_home
        } else {
            R.layout.fragment_home_buyer
        }
        return inflater.inflate(layoutRes, container, false)
    }
}
