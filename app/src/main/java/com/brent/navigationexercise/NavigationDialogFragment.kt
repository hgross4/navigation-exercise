package com.brent.navigationexercise


import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ELAPSED_TIME = "elapsed_time"
private const val DISTANCE_TRAVELED = "distance_traveled"

/**
 * A simple [Fragment] subclass.
 * Use the [NavigationDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class NavigationDialogFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var elapsedTime: String? = null
    private var distanceTraveled: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            elapsedTime = it.getString(ELAPSED_TIME)
            distanceTraveled = it.getString(DISTANCE_TRAVELED)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_navigation_dialog, container, false)

        val elapsedTimeView = rootView.findViewById<TextView>(R.id.elapsed_time)
        elapsedTimeView.setText(String.format(getString(R.string.elapsed_time, ": " + elapsedTime)))

        val distanceTraveledView = rootView.findViewById<TextView>(R.id.distance_traveled)
        distanceTraveledView.setText(String.format(getString(R.string.distance_traveled, ": " + distanceTraveled)))

        return rootView
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param elapsedTime Parameter 1.
         * @param distanceTraveled Parameter 2.
         * @return A new instance of fragment NavigationDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(elapsedTime: String, distanceTraveled: String) =
                NavigationDialogFragment().apply {
                    arguments = Bundle().apply {
                        putString(ELAPSED_TIME, elapsedTime)
                        putString(DISTANCE_TRAVELED, distanceTraveled)
                    }
                }
    }
}
