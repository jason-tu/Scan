package io.synople.scan

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.fragment_ar.*
import java.lang.IllegalStateException
import kotlin.concurrent.thread


class ScanFragment : Fragment() {

    private lateinit var recognizer: Recognizer
    private lateinit var arFragment: ArFragment
    private val anchors: MutableList<Anchor> = mutableListOf()
    private var totalCount = 0
    private var washingtons = 0
    private var lincolns = 0
    private var hamiltons = 0
    private var jacksons = 0
    private var grants = 0
    private var franklins = 0

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.total).text = totalCount.toString()
        recognizer = Recognizer(context)

        scan.setOnClickListener {
            scan.text = "Scanning..."
            scan.isEnabled = false

            thread(true) {
                analyze()
            }
        }

        arFragment = (childFragmentManager.findFragmentById(R.id.uxFragment) as ArFragment)
        arFragment.arSceneView.planeRenderer.isVisible = false
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
                    }
        }

        autoFocus.setOnClickListener {
            val config = Config(arFragment.arSceneView.session)
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            arFragment.arSceneView.session.configure(config)
            autoFocus.visibility = View.GONE
        }

        total.setOnClickListener {
            total.visibility = View.INVISIBLE
            breakdown.visibility = View.VISIBLE
        }

        breakdown.setOnClickListener {
            total.visibility = View.VISIBLE
            breakdown.visibility = View.INVISIBLE
        }

        thread(true) {
            while (true) {
                Thread.sleep(3000)
                try {
                    analyze()
                } catch (e: IllegalStateException) {
                }
            }
        }
    }

    private fun analyze() {
        val image =
                (childFragmentManager.findFragmentById(R.id.uxFragment) as ArFragment).arSceneView.arFrame.acquireCameraImage()

        val moneyList = recognizer.getFaces(image)
        image.close()

        totalCount = 0
        washingtons = 0
        lincolns = 0
        hamiltons = 0
        jacksons = 0
        grants = 0
        franklins = 0
        anchors.forEach {
            it.detach()
        }
        anchors.clear()

        activity!!.runOnUiThread {
            moneyList.forEach { money ->
                if (arFragment.arSceneView.arFrame.hitTest(money.x, money.y).size == 0) {
                    return@forEach
                }

                val anchor = arFragment.arSceneView.arFrame.hitTest(money.x, money.y)[0].createAnchor()

                anchors.add(anchor)

                ViewRenderable.builder()
                        .setView(context, R.layout.renderable_money)
                        .build()
                        .thenAccept { renderable ->
                            val anchorNode = AnchorNode(anchor)
                            anchorNode.setParent(arFragment.arSceneView.scene)

                            when (money.value) {
                                "washington" -> (renderable?.view as TextView).text = "$1"
                                "lincoln" -> (renderable?.view as TextView).text = "$5"
                                "hamilton" -> (renderable?.view as TextView).text = "$10"
                                "jackson" -> (renderable?.view as TextView).text = "$20"
                                "grant" -> (renderable?.view as TextView).text = "$50"
                                "franklin" -> (renderable?.view as TextView).text = "$100"
                            }
                            val transformableNode = TransformableNode(arFragment.transformationSystem)
                            transformableNode.setParent(anchorNode)
                            transformableNode.renderable = renderable
                        }

                when (money.value) {
                    "washington" -> {
                        totalCount += 1
                        washingtons += 1
                    }
                    "lincoln" -> {
                        totalCount += 5
                        lincolns += 1
                    }
                    "hamilton" -> {
                        totalCount += 10
                        hamiltons += 1
                    }
                    "jackson" -> {
                        totalCount += 20
                        jacksons += 1
                    }
                    "grant" -> {
                        totalCount += 50
                        grants += 1
                    }
                    "franklin" -> {
                        totalCount += 100
                        franklins += 1
                    }
                }

                if (totalCount > 0) {
                    total.text = "Total: $" + totalCount.toString()
                }

                var breakdownString = ""
                if (washingtons != 0) {
                    breakdownString += "$1 - $washingtons\n"
                }
                if (lincolns != 0) {
                    breakdownString += "$5 - $lincolns\n"
                }
                if (hamiltons != 0) {
                    breakdownString += "$10 - $hamiltons\n"
                }
                if (jacksons != 0) {
                    breakdownString += "$20 - $jacksons\n"
                }
                if (grants != 0) {
                    breakdownString += "$50 - $grants\n"
                }
                if (franklins != 0) {
                    breakdownString += "$100 - $franklins"
                }
                breakdown.text = breakdownString

                scan.text = "Scan"
                scan.isEnabled = true
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ScanFragment()
    }
}
