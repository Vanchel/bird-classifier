package com.vanchel.birdclassifier.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.vanchel.birdclassifier.R
import com.vanchel.birdclassifier.databinding.FragmentCameraBinding
import com.vanchel.birdclassifier.viewmodels.CameraViewModel
import java.io.File
import java.util.*
import java.util.concurrent.Executors

private const val FILENAME = "buff.jpg"

class CameraFragment : Fragment() {
    private var binding: FragmentCameraBinding? = null

    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.onPermissionGranted()
            } else {
                viewModel.onPermissionDenied()
            }
        }

    private val getContentLauncher =
        registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let(::goToResults)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCameraBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@CameraFragment.viewModel

            pictureButton.setOnClickListener { takePhoto() }
            rationaleView.permissionButton.setOnClickListener {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        viewModel.isPermissionGranted.observe(viewLifecycleOwner) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    getString(R.string.permissions_not_granted),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        outputDirectory = getOutputDirectory()

        requestCameraPermission()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_camera_fragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chooseFromGallery -> {
                getContentLauncher.launch("image/*")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onPermissionGranted()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.CAMERA
            ) -> {
                viewModel.onPermissionDenied()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val aspectRatio = AspectRatio.RATIO_4_3

            val resolution = Size(512, 512)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .build()

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(resolution)
                .build()

            cameraProvider.unbindAll()

            try {
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                val previewView = binding!!.previewView

                preview.setSurfaceProvider(previewView.surfaceProvider)
                previewView.setOnTouchListener(createPinchToZoomListener(camera))
            } catch (_: Exception) {
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createPinchToZoomListener(camera: Camera): View.OnTouchListener {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f

                val delta = detector.scaleFactor

                camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)

        return View.OnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(outputDirectory, FILENAME)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {}

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    goToResults(savedUri)
                }
            })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireActivity().externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else requireActivity().filesDir
    }

    private fun goToResults(imageUri: Uri) {
        findNavController().navigate(
            CameraFragmentDirections.actionCameraFragmentToResultFragment(imageUri)
        )
    }
}
