package io.synople.scanmoney

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.fragment_ar.*
import kotlin.concurrent.thread


class MainFragment : Fragment() {

    lateinit var recognizer: Recognizer
    lateinit var arFragment: ArFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recognizer = Recognizer(context)

        scan.setOnClickListener {
            val image =
                (childFragmentManager.findFragmentById(R.id.uxFragment) as ArFragment).arSceneView.arFrame.acquireCameraImage()

            val money = recognizer.recognize(image)
            Log.v("Money", money.toString())
        }

        arFragment = (childFragmentManager.findFragmentById(R.id.uxFragment) as ArFragment)
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            val anchor = hitResult.createAnchor()

            ViewRenderable.builder()
                .setView(context, R.layout.renderable_money)
                .build()
                .thenAccept { renderable ->
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arFragment.arSceneView.scene)

                    (renderable?.view as TextView).text = "!"
                    val transformableNode = TransformableNode(arFragment.transformationSystem)
                    transformableNode.setParent(anchorNode)
                    transformableNode.renderable = renderable
                    transformableNode.select()
                }
        }

        thread(false) {
            Thread.sleep(3000)
            val image =
                (childFragmentManager.findFragmentById(R.id.uxFragment) as ArFragment).arSceneView.arFrame.acquireCameraImage()

            val money = recognizer.recognize(image)
            Log.v("Money", money.toString())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}
