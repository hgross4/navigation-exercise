package com.brent.navigationexercise


import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.*
import android.widget.TextView
import java.util.concurrent.TimeUnit


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
    private var dialogListener: NavigationDialogFragment.DialogListener? = null

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val alertDialogBuilder: AlertDialog.Builder  = AlertDialog.Builder(getActivity())
        val message = StringBuilder()
        val formattedElapsedTime = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedTime!!.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(elapsedTime!!.toLong()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime!!.toLong()))
        )
        message.append(String.format(getString(R.string.elapsed_time, ": $formattedElapsedTime")))
                .append("\n")
                .append(String.format(getString(R.string.distance_traveled, ": $distanceTraveled")))
        alertDialogBuilder.setMessage(message)
        alertDialogBuilder.setPositiveButton("CLOSE"){dialog, which ->
            dialogListener?.onDialogDismissed()
        }

        val alertDialog = alertDialogBuilder.create()
        val window = alertDialog.getWindow()
        val wlp = window.attributes

        wlp.gravity = Gravity.BOTTOM
        wlp.flags = wlp.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        window.attributes = wlp

        return alertDialog
    }

    interface DialogListener {
        fun onDialogDismissed()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is Activity && context is NavigationDialogFragment.DialogListener) {
            dialogListener = context
        }
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
