package com.example.voctrainer.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.voctrainer.R // Or your specific binding class if you use ViewBinding

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
        // If using ViewBinding, you would do something like:
        // val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        // return binding.root
    }
}