package com.pbl.grandmarket_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pbl.grandmarket_android.databinding.AcitvityHomeBinding

class MainActivity : AppCompatActivity() {
    private val binding:AcitvityHomeBinding by lazy {
        AcitvityHomeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}