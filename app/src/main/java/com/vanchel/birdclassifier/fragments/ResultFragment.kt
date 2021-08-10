package com.vanchel.birdclassifier.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.navArgs
import com.vanchel.birdclassifier.R
import com.vanchel.birdclassifier.ml.LiteModelAiyVisionClassifierBirdsV13
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category

class ResultFragment : Fragment() {
    private val args: ResultFragmentArgs by navArgs()
    private val probability: List<Category> by lazy(::recognizeImage)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_result, container, false)

        val imageView: ImageView = view.findViewById(R.id.imageView)
        imageView.setImageURI(args.photoUri)

        val result = probability.maxByOrNull { it.score }!!

        val textView: TextView = view.findViewById(R.id.textView)
        textView.text = "${result.label} - ${result.score}"

        return view
    }

    private fun recognizeImage(): List<Category> {
        val model = LiteModelAiyVisionClassifierBirdsV13.newInstance(requireContext())

        val bitmap =
            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, args.photoUri)
        val image = TensorImage.fromBitmap(bitmap)

        val outputs = model.process(image)

        model.close()

        return outputs.probabilityAsCategoryList
    }
}