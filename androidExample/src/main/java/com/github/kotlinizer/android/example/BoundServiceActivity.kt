package com.github.kotlinizer.android.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.kotlinizer.android.example.databinding.ActivityBoundServiceBinding
import com.github.kotlinizer.android.example.service.ExampleServiceBound
import com.github.kotlinizer.mppktx.service.BoundService.Companion.withService
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BoundServiceActivity : AppCompatActivity() {

    private val exampleServiceOne by lazy {
        ExampleServiceBound(this, lifecycleScope)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBoundServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generate.setOnClickListener {
            lifecycleScope.launch {
                withService({ ExampleServiceBound(application, it) }) { exampleServiceBound ->
                    binding.generatedValue.text = exampleServiceBound {
                        generateString()
                    }
                }
            }
        }
        binding.generateSecond.setOnClickListener {
            lifecycleScope.launch {
                withService(application, ::ExampleServiceBound) { exampleServiceBound ->
                    binding.generatedValue.text = exampleServiceBound {
                        generateString()
                    }
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            exampleServiceOne.mapFlow {
                timeFlow
            }.collect {
                withContext(Main) {
                    binding.time.text = it
                }
            }
        }
    }
}