package com.github.libretube.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.databinding.DialogDownloadBinding
import com.github.libretube.obj.Streams
import com.github.libretube.services.DownloadService
import com.github.libretube.util.PermissionHelper
import com.github.libretube.util.RetrofitInstance
import com.github.libretube.util.ThemeHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.HttpException
import java.io.IOException

class DownloadDialog : DialogFragment() {
    private val TAG = "DownloadDialog"
    private lateinit var binding: DialogDownloadBinding

    private lateinit var videoId: String
    private var duration = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            videoId = arguments?.getString("video_id")!!

            val builder = MaterialAlertDialogBuilder(it)
            binding = DialogDownloadBinding.inflate(layoutInflater)

            fetchAvailableSources()

            PermissionHelper.requestReadWrite(activity as AppCompatActivity)

            binding.title.text = ThemeHelper.getStyledAppName(requireContext())

            builder.setView(binding.root)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun fetchAvailableSources() {
        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getStreams(videoId)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG, "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException, unexpected response")
                Toast.makeText(context, R.string.server_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            }
            initDownloadOptions(response)
        }
    }

    private fun initDownloadOptions(streams: Streams) {
        val vidName = arrayListOf<String>()
        val vidUrl = arrayListOf<String>()

        // add empty selection
        vidName.add(getString(R.string.no_video))
        vidUrl.add("")

        // add all available video streams
        for (vid in streams.videoStreams!!) {
            if (vid.url != null) {
                val name = vid.quality + " " + vid.format
                vidName.add(name)
                vidUrl.add(vid.url!!)
            }
        }

        val audioName = arrayListOf<String>()
        val audioUrl = arrayListOf<String>()

        // add empty selection
        audioName.add(getString(R.string.no_audio))
        audioUrl.add("")

        // add all available audio streams
        for (audio in streams.audioStreams!!) {
            if (audio.url != null) {
                val name = audio.quality + " " + audio.format
                audioName.add(name)
                audioUrl.add(audio.url!!)
            }
        }

        // initialize the video sources
        val videoArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            vidName
        )
        videoArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.videoSpinner.adapter = videoArrayAdapter
        if (binding.videoSpinner.size >= 1) binding.videoSpinner.setSelection(1)

        // initialize the audio sources
        val audioArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            audioName
        )
        audioArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.audioSpinner.adapter = audioArrayAdapter
        if (binding.audioSpinner.size >= 1) binding.audioSpinner.setSelection(1)

        binding.download.setOnClickListener {
            val selectedAudioUrl = audioUrl[binding.audioSpinner.selectedItemPosition]
            val selectedVideoUrl = vidUrl[binding.videoSpinner.selectedItemPosition]

            val intent = Intent(context, DownloadService::class.java)
            intent.putExtra("videoId", videoId)
            intent.putExtra("videoUrl", selectedVideoUrl)
            intent.putExtra("audioUrl", selectedAudioUrl)
            intent.putExtra("duration", duration)
            context?.startService(intent)
            dismiss()
        }
    }
}
