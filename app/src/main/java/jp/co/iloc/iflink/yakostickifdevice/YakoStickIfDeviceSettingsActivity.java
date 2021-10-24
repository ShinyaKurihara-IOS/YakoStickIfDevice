package jp.co.iloc.iflink.yakostickifdevice;


import android.content.Intent;

import androidx.annotation.NonNull;

import jp.co.toshiba.iflink.ui.BaseSettingsActivity;

public class YakoStickIfDeviceSettingsActivity extends BaseSettingsActivity {
    /**
     * Preferences名.
     */
    public static final String PREFERENCE_NAME
            = "jp.co.iloc.iflink.yakostickifdevice";

    @Override
    protected final int getPreferencesResId() {
        return R.xml.pref_yakostickifdevice;
    }

    @NonNull
    @Override
    protected final String getPreferencesName() {
        return PREFERENCE_NAME;
    }

    @Override
    protected final Intent getIntentForService() {
        Intent intent = new Intent(
                getApplicationContext(),
                YakoStickIfDeviceIms.class);
        intent.setPackage(getClass().getPackage().getName());
        return intent;
    }
}
