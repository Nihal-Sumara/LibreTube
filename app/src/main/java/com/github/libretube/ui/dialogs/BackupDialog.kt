package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.DialogBackupBinding
import com.github.libretube.db.DatabaseHolder.Companion.Database
import com.github.libretube.obj.BackupFile
import com.github.libretube.obj.PreferenceItem
import com.github.libretube.ui.adapters.BackupOptionsAdapter
import com.github.libretube.util.PreferenceHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BackupDialog(
    private val createBackupFile: (BackupFile) -> Unit
) : DialogFragment() {
    private lateinit var binding: DialogBackupBinding

    private val backupFile = BackupFile()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val backupOptions = listOf(
            R.string.watch_history,
            R.string.watch_positions,
            R.string.search_history,
            R.string.local_subscriptions,
            R.string.backup_customInstances,
            R.string.preferences
        )

        val selected = MutableList(backupOptions.size) { false }

        binding = DialogBackupBinding.inflate(layoutInflater)
        binding.backupOptionsRecycler.layoutManager = LinearLayoutManager(context)
        binding.backupOptionsRecycler.adapter =
            BackupOptionsAdapter(backupOptions) { position, isChecked ->
                selected[position] = isChecked
            }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.backup) { _, _ ->
                val thread = Thread {
                    if (selected[0]) {
                        backupFile.watchHistory =
                            Database.watchHistoryDao().getAll()
                    }
                    if (selected[1]) {
                        backupFile.watchPositions =
                            Database.watchPositionDao().getAll()
                    }
                    if (selected[2]) {
                        backupFile.searchHistory =
                            Database.searchHistoryDao().getAll()
                    }
                    if (selected[3]) {
                        backupFile.localSubscriptions =
                            Database.localSubscriptionDao().getAll()
                    }
                    if (selected[4]) {
                        backupFile.customInstances =
                            Database.customInstanceDao().getAll()
                    }
                    if (selected[5]) {
                        backupFile.preferences = PreferenceHelper.settings.all.map {
                            PreferenceItem(
                                it.key,
                                it.value
                            )
                        }
                    }
                }
                thread.start()
                thread.join()

                createBackupFile(backupFile)
            }
            .create()
    }
}