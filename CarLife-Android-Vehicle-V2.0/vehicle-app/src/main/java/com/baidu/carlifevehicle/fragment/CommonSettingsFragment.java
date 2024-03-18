package com.baidu.carlifevehicle.fragment;

import static com.baidu.carlife.sdk.Configs.FEATURE_CONFIG_FOCUS_UI;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.baidu.carlifevehicle.R;
import com.baidu.carlifevehicle.util.CommonParams;
import com.baidu.carlifevehicle.util.PreferenceUtil;

public class CommonSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      getPreferenceManager().setSharedPreferencesName(CommonParams.CARLIFE_NORMAL_PREFERENCES);
        addPreferencesFromResource(R.xml.carlife);
        //findPreference(FEATURE_CONFIG_FOCUS_UI).setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        super.onDisplayPreferenceDialog(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
