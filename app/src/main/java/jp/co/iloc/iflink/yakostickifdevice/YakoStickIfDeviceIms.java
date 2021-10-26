package jp.co.iloc.iflink.yakostickifdevice;

import android.Manifest;

import androidx.annotation.NonNull;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.Set;

import jp.co.toshiba.iflink.epaapi.EPADevice;
import jp.co.toshiba.iflink.imsif.DeviceConnector;
import jp.co.toshiba.iflink.imsif.IfLinkConnector;
import jp.co.toshiba.iflink.ui.PermissionActivity;

public class YakoStickIfDeviceIms extends IfLinkConnector {
    /**
     * ログ出力用タグ名
     */
    private static final String TAG = "YAKOSTICKIFDEVICE-IMS";
    /**
     * ログ出力レベル：CustomDevice
     */
    private static final String LOG_LEVEL_CUSTOM_DEV = "CUSTOM-DEV";
    /**
     * ログ出力レベル：CustomDevice
     */
    private static final String LOG_LEVEL_CUSTOM_IMS = "CUSTOM-IMS";

    /**
     * ログ出力切替フラグ
     */
    private boolean bDBG = false;

    /**
     * コンストラクタ.
     */
    public YakoStickIfDeviceIms() {
        super("YakoStickIfDeviceIms");
    }

    @NonNull
    @Override
    protected final String getPreferencesName() {
        return YakoStickIfDeviceSettingsActivity.PREFERENCE_NAME;
    }

    @Override
    protected final void updateLogLevelSettings(final Set<String> settings) {
        if (bDBG) Log.d(TAG, "LogLevel settings=" + settings);
        super.updateLogLevelSettings(settings);

        boolean isEnabledLog = false;
        if (settings.contains(LOG_LEVEL_CUSTOM_IMS)) {
            isEnabledLog = true;
        }
        bDBG = isEnabledLog;

        isEnabledLog = false;
        if (settings.contains(LOG_LEVEL_CUSTOM_DEV)) {
            isEnabledLog = true;
        }
        for (DeviceConnector device : mDeviceList) {
            device.enableLogLocal(isEnabledLog);
        }
    }

    @Override
    protected void onActivationResult(final boolean result,
                                      final EPADevice epaDevice) {
        if (bDBG) Log.d(TAG, "onActivationResult");
        //Bluetoothアダプター初期化
        BluetoothManager manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        //bluetoothの使用が許可されていない場合は許可を求める。
        if( adapter == null || !adapter.isEnabled() ){
            Log.d(TAG,"bluetooth off. please turn on.");
            return;
        }

        YakoStickIfDevice yakoStick = new YakoStickIfDevice( this, adapter );
    }

    @Override
    protected final String[] getPermissions() {
        if (bDBG) Log.d(TAG, "getPermissions");
        // サンプル用：BLE利用の場合のコードを生成しています。
        return new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    @Override
    protected void onPermissionGranted() {
        // パーミッションを許可された場合の処理を記述してください。
        if (bDBG) Log.d(TAG, "onPermissionGranted");
    }

    @Override
    protected Class getPermissionActivityClass() {
        return PermissionActivity.class;
    }
}
