package com.brent.navigationexercise

/**
 * Adapted from https://github.com/commonsguy/cw-omnibus/tree/master/MapsV2/Location
 */

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.DialogInterface
import android.content.pm.FeatureInfo
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

open class AbstractMapActivity : AppCompatActivity() {

    protected fun readyToGo(): Boolean {
        val checker = GoogleApiAvailability.getInstance()

        val status = checker.isGooglePlayServicesAvailable(this)

        if (status == ConnectionResult.SUCCESS) {
            if (getVersionFromPackageManager(this) >= 2) {
                return true
            } else {
                Toast.makeText(this, R.string.no_maps, Toast.LENGTH_LONG).show()
                finish()
            }
        } else if (checker.isUserResolvableError(status)) {
            ErrorDialogFragment.newInstance(status)
                    .show(fragmentManager,
                            TAG_ERROR_DIALOG_FRAGMENT)
        } else {
            Toast.makeText(this, R.string.no_maps, Toast.LENGTH_LONG).show()
            finish()
        }

        return false
    }

    class ErrorDialogFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
            val args = arguments
            val checker = GoogleApiAvailability.getInstance()

            return checker.getErrorDialog(activity,
                    args.getInt(ARG_ERROR_CODE), 0)
        }

        override fun onDismiss(dlg: DialogInterface) {
            if (activity != null) {
                activity.finish()
            }
        }

        companion object {
            internal val ARG_ERROR_CODE = "errorCode"

            internal fun newInstance(errorCode: Int): ErrorDialogFragment {
                val args = Bundle()
                val result = ErrorDialogFragment()

                args.putInt(ARG_ERROR_CODE, errorCode)
                result.arguments = args

                return result
            }
        }
    }

    companion object {
        internal val TAG_ERROR_DIALOG_FRAGMENT = "errorDialog"

        // following from
        // https://android.googlesource.com/platform/cts/+/master/tests/tests/graphics/src/android/opengl/cts/OpenGlEsVersionTest.java

        /*
   * Copyright (C) 2010 The Android Open Source Project
   *
   * Licensed under the Apache License, Version 2.0 (the
   * "License"); you may not use this file except in
   * compliance with the License. You may obtain a copy of
   * the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in
   * writing, software distributed under the License is
   * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
   * CONDITIONS OF ANY KIND, either express or implied. See
   * the License for the specific language governing
   * permissions and limitations under the License.
   */

        private fun getVersionFromPackageManager(context: Context): Int {
            val packageManager = context.packageManager
            val featureInfos = packageManager.systemAvailableFeatures
            if (featureInfos != null && featureInfos.size > 0) {
                for (featureInfo in featureInfos) {
                    // Null feature name means this feature is the open
                    // gl es version feature.
                    if (featureInfo.name == null) {
                        return if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                            getMajorVersion(featureInfo.reqGlEsVersion)
                        } else {
                            1 // Lack of property means OpenGL ES
                            // version 1
                        }
                    }
                }
            }
            return 1
        }

        /** @see FeatureInfo.getGlEsVersion
         */
        private fun getMajorVersion(glEsVersion: Int): Int {
            return glEsVersion and -0x10000 shr 16
        }
    }
}
