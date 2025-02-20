package com.example.finjoy.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.finjoy.NotificationWorker
import com.example.finjoy.databinding.FragmentSettingsBinding
import androidx.work.WorkManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.finjoy.DataExporter
import com.example.finjoy.ui.category.CategoryManagementActivity
import kotlinx.coroutines.launch
import java.time.LocalDate


class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // Setup category management
        binding.manageCategoriesCard.setOnClickListener {
            val intent = Intent(requireContext(), CategoryManagementActivity::class.java)
            startActivity(intent)
        }

        // Setup notification switch
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.notificationSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (isChecked) {
                NotificationWorker.schedule(requireContext())
                Toast.makeText(requireContext(),
                    "Notifications enabled - test notification in 15 seconds",
                    Toast.LENGTH_LONG).show()
            } else {
                WorkManager.getInstance(requireContext())
                    .cancelAllWorkByTag("daily_notification")
                WorkManager.getInstance(requireContext())
                    .cancelAllWorkByTag("test_notification")
                Toast.makeText(requireContext(),
                    "Notifications disabled",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Setup export/import buttons
        binding.exportButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "finjoy_backup_${LocalDate.now()}.csv")
            }
            exportLauncher.launch(intent)
        }

        binding.importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "application/csv",
                    "text/plain"
                ))
            }
            importLauncher.launch(intent)
        }
    }
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                exportData(uri)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importData(uri)
            }
        }
    }

    private fun exportData(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                DataExporter(requireContext()).exportData(uri)
                Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importData(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                DataExporter(requireContext()).importData(uri)
                Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}