/******************************************************************************
 * Copyright 2017 The Baidu Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.carlifevehicle.fragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.baidu.carlife.sdk.CarLifeContext
import com.baidu.carlife.sdk.receiver.CarLife
import com.baidu.carlife.sdk.util.Logger.Companion.d
import com.baidu.carlifevehicle.R
import com.baidu.carlifevehicle.util.CommonParams
import com.baidu.carlifevehicle.util.PreferenceUtil
import com.permissionx.guolindev.PermissionX
import kotlinx.android.synthetic.main.frag_help_android.*

class HelpAndroidAOAFragment : BaseFragment() {
    private var mBackBtn: ImageButton? = null
    private var mTitle: TextView? = null
    private var mAndroid: TextView? = null
    private var mAndroidLayout: View? = null


    companion object {
        private const val TAG = "HelpAndroidAOAFragment"
        private var mHelpAndroidAOAFragment: HelpAndroidAOAFragment? = null

        @JvmStatic
        val instance: HelpAndroidAOAFragment?
            get() {
                if (mHelpAndroidAOAFragment == null) {
                    mHelpAndroidAOAFragment = HelpAndroidAOAFragment()
                }
                return mHelpAndroidAOAFragment
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        d(TAG, "onCreateView")
        mContentView = LayoutInflater.from(mActivity)
            .inflate(R.layout.frag_help_android, null) as ViewGroup
        mBackBtn = mContentView.findViewById<View>(R.id.ib_left) as ImageButton
        mBackBtn!!.setOnClickListener { onBackPressed() }
        mTitle = mContentView.findViewById<View>(R.id.tv_title) as TextView
        mTitle!!.text = getString(R.string.help_android_aoa_title)
        mAndroid = mContentView.findViewById<View>(R.id.android_device) as TextView
        mAndroid!!.text = getString(R.string.help_android_aoa_device)
        mAndroidLayout = mContentView.findViewById(R.id.goto_android_layout)
        mAndroidLayout!!.setOnClickListener(View.OnClickListener {
            mFragmentManager.showFragment(
                AuthorizationRequestHelpFragment.getInstance()
            )
        })
        return mContentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _connect_type.check(
            when (CarLife.receiver().connectionType) {
                CarLifeContext.CONNECTION_TYPE_AOA -> R.id._usb
                CarLifeContext.CONNECTION_TYPE_HOTSPOT -> R.id._wifi
                CarLifeContext.CONNECTION_TYPE_WIFIDIRECT -> R.id._dir
                else -> R.id._usb
            }
        )
        _connect_type.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id._usb -> {
                    CarLife.receiver().setConnectType(CarLifeContext.CONNECTION_TYPE_AOA)
                    PreferenceUtil.getInstance()
                        .putInt(
                            CommonParams.CONNECT_TYPE_SHARED_PREFERENCES,
                            CarLifeContext.CONNECTION_TYPE_AOA
                        )
                }
                R.id._wifi -> {
                    CarLife.receiver()
                        .setConnectType(CarLifeContext.CONNECTION_TYPE_HOTSPOT)
                    PreferenceUtil.getInstance()
                        .putInt(
                            CommonParams.CONNECT_TYPE_SHARED_PREFERENCES,
                            CarLifeContext.CONNECTION_TYPE_HOTSPOT
                        )
                }
                R.id._dir -> {
                    PermissionX.init(this)
                        .permissions(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        .onExplainRequestReason { scope, deniedList ->
                            scope.showRequestReasonDialog(
                                deniedList,
                                "打开权限",
                                "确定",
                                "取消"
                            )
                        }
                        .request { allGranted, grantedList, deniedList ->
                            if (!allGranted) {
                                Toast.makeText(
                                    requireContext(),
                                    "权限未打开: $deniedList",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                CarLife.receiver()
                                    .setConnectType(CarLifeContext.CONNECTION_TYPE_WIFIDIRECT)
                                PreferenceUtil.getInstance()
                                    .putInt(
                                        CommonParams.CONNECT_TYPE_SHARED_PREFERENCES,
                                        CarLifeContext.CONNECTION_TYPE_WIFIDIRECT
                                    )
                            }
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        d(TAG, "onDestroyView")
    }

    override fun onBackPressed(): Boolean {
        if (mFragmentManager != null) {
            mFragmentManager.showFragment(HelpMainFragment.getInstance())
        }
        return true
    }
}