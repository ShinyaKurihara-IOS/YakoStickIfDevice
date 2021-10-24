package jp.co.iloc.iflink.yakostickifdevice;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Looper;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import java.util.Set;
import java.util.HashSet;

import java.util.HashMap;


//Toast Sample start
import android.widget.Toast;
//Toast Sample end

//If Sample start
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.toshiba.iflink.epaapi.EPAdata;
//If Sample end

import jp.co.toshiba.iflink.imsif.IfLinkConnector;
import jp.co.toshiba.iflink.imsif.DeviceConnector;
import jp.co.toshiba.iflink.imsif.IfLinkSettings;
import jp.co.toshiba.iflink.imsif.IfLinkAlertException;
import jp.co.toshiba.iflink.ui.PermissionActivity;

public class YakoStickIfDevice extends DeviceConnector {
    /**
     * ログ出力用タグ名.
     */
    private static final String TAG = "YAKOSTICKIFDEVICE-DEV";
    /**
     * メッセージを取得するキー.
     */
    private static final String YAKOSTICKIFDEVICE_JOB_KEY = "yakostickifdevice_job_key";
    /**
     * ログ出力切替フラグ.
     */
    private boolean bDBG = false;
    //Toast Sample start
    /**
     * 処理実行のハンドラ.
     */
    private Handler handler = new Handler(Looper.getMainLooper());
    //Toast Sample end
    //If Sample start
    /**
     * 設定パラメータ.
     */
    private int settingsParameter;
    /**
     * データ送信用タイマー.
     */
    private Timer sendDataTimer;
    //If Sample end

    /**
     * コンストラクタ.
     *
     * @param ims IMS
     */
    public YakoStickIfDevice(final IfLinkConnector ims) {
        super(ims, MONITORING_LEVEL4, PermissionActivity.class);
        mDeviceName = "YakoStickIfDevice";
        mDeviceSerial = "epa";
        mSchemaName = "yakostickifdevice";
        setSchema();

        mCookie = IfLinkConnector.EPA_COOKIE_KEY_TYPE + "=" + IfLinkConnector.EPA_COOKIE_VALUE_CONFIG
                + IfLinkConnector.COOKIE_DELIMITER
                + IfLinkConnector.EPA_COOKIE_KEY_TYPE + "=" + IfLinkConnector.EPA_COOKIE_VALUE_ALERT
                + IfLinkConnector.COOKIE_DELIMITER
                + IfLinkConnector.EPA_COOKIE_KEY_TYPE + "=" + IfLinkConnector.EPA_COOKIE_TYPE_VALUE_JOB
                + IfLinkConnector.COOKIE_DELIMITER
                + IfLinkConnector.EPA_COOKIE_KEY_DEVICE + "=" + mDeviceName
                + IfLinkConnector.COOKIE_DELIMITER
                + IfLinkConnector.EPA_COOKIE_KEY_ADDRESS + "=" + IfLinkConnector.EPA_COOKIE_VALUE_ANY;

        mAssetName = "YAKOSTICKIFDEVICE_EPA";

        // サンプル用：ここでデバイスを登録します。
        // 基本は、デバイスとの接続確立後、デバイスの対応したシリアル番号に更新してからデバイスを登録してください。
        addDevice();
        // 基本は、デバイスとの接続が確立した時点で呼び出します。
        notifyConnectDevice();
    }

    @Override
    public boolean onStartDevice() {
        if (bDBG) Log.d(TAG, "onStartDevice");
        // デバイスからのデータ送信開始処理を記述してください。
        //If Sample start
        // 初回実行時刻（次分の00秒）を取得
        Calendar startTime = Calendar.getInstance();
        startTime.add(Calendar.MINUTE, 1);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
        // 1秒ごとにタスクを起動
        if (sendDataTimer != null) {
            sendDataTimer.cancel();
        }
        sendDataTimer = new Timer(true);
        sendDataTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        // 秒数を取得
                        Calendar now = Calendar.getInstance();
                        int second = now.get(Calendar.SECOND);
                        // データ送信
                        sendData(encodeData(second));
                    }
                });
            }
        }, startTime.getTime(), 1000);
        //If Sample end
        // 送信開始が別途完了通知を受ける場合には、falseを返してください。
        return true;
    }

    //If Sample start
    private int encodeData(int data) {
        if (this.settingsParameter <= 0) {
            return (int) data;
        }
        if (data % this.settingsParameter == 0) {
            // 設定値の倍数の場合
            data += 100;
        }
        if (data % 10 == this.settingsParameter || data / 10 == this.settingsParameter) {
            // 下1桁or上1桁が設定値の場合
            data += 200;
        }
        return (int) data;
    }

    //If Sample end
    @Override
    public boolean onStopDevice() {
        if (bDBG) Log.d(TAG, "onStopDevice");
        // デバイスからのデータ送信停止処理を記述してください。
        //If Sample start
        if (sendDataTimer != null) {
            sendDataTimer.cancel();
        }
        //If Sample end
        // 送信停止が別途完了通知を受ける場合には、falseを返してください。
        return true;
    }

    @Override
    public boolean onJob(final HashMap<String, Object> map) {
        //Toast Sample start
        if (map.containsKey(YAKOSTICKIFDEVICE_JOB_KEY)) {
            final String strVal = String.valueOf(map.get(YAKOSTICKIFDEVICE_JOB_KEY));
            // 抽出したパラメータの型変換
            String val = strVal;
            // 抽出したパラメータを元に実際の制御を記述してください。
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mIms, strVal, Toast.LENGTH_LONG).show();
                }
            });
        }
        //Toast Sample end
        return false;
    }

    //If Sample start

    /**
     * 「yakostickifdevice」データを送信する.
     * ※サンプルとして実装したメソッドです.
     * このメソッドをデバイスからデータを受信したタイミング等で呼び出せば.
     * ifLink Core側にデータが送信されます.
     *
     * @param data 送信するデータ.
     */
    public void sendData(final int data) {
        Log.i(TAG, "sendDeviceData data=" + data);
        // 送信データクリア
        clearData();
        // データの登録.
        addData(new EPAdata("yakostickifdevice", "int", String.valueOf(data)));
        //ifLink Coreへデータを送信する.
        notifyRecvData();
    }
    //If Sample end

    @Override
    public void enableLogLocal(final boolean enabled) {
        bDBG = enabled;
    }

    @Nullable
    @Override
    protected XmlResourceParser getResourceParser(final Context context) {
        Resources resources = context.getResources();
        if (resources != null) {
            return context.getResources().getXml(R.xml.schema_yakostickifdevice);
        } else {
            return null;
        }

    }

    @Override
    protected void onUpdateConfig(@NonNull IfLinkSettings settings) throws IfLinkAlertException {
        if (bDBG) Log.d(TAG, "onUpdateConfig");
        String key = mIms.getString(R.string.pref_yakostickifdevice_settings_parameter_key);
        int param = settings.getIntValue(key, 1);
        if (bDBG) Log.d(TAG, "parameter[" + key + "] = " + param);
        // 設定パラメータを更新する処理を記述してください。
        // insert routine for reflecting received parameter
        //If Sample start
        this.settingsParameter = (int) param;
        //If Sample end
    }

    @Override
    protected final String[] getPermissions() {
        if (bDBG) Log.d(TAG, "getPermissions");
        return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    @Override
    protected void onPermissionGranted() {
        // パーミッションを許可された後の処理を記述してください。
    }


    @Override
    public final boolean checkPathConnection() {
        if (bDBG) Log.d(TAG, "checkPathConnection");
        // デバイスとの接続経路(WiFi, BLE, and so on・・・)が有効かをチェックする処理を記述してください。
        return true;
    }

    @Override
    public final boolean reconnectPath() {
        if (bDBG) Log.d(TAG, "reconnectPath");
        // デバイスとの接続経路(WiFi, BLE, and so on・・・)を有効にする処理を記述してください。
        return true;
    }

    @Override
    public final boolean checkDeviceConnection() {
        if (bDBG) Log.d(TAG, "checkDeviceConnection");
        // デバイスとの接続が維持されているかをチェックする処理を記述してください。
        return true;
    }

    @Override
    public final boolean reconnectDevice() {
        if (bDBG) Log.d(TAG, "reconnectDevice");
        // デバイスとの再接続処理を記述してください。
        return true;
    }

    @Override
    public final boolean checkDeviceAlive() {
        if (bDBG) Log.d(TAG, "checkDeviceAlive");
        // デバイスから定期的にデータ受信が出来ているかをチェックする処理を記述してください。
        return true;
    }

    @Override
    public final boolean resendDevice() {
        if (bDBG) Log.d(TAG, "resendDevice");
        // デバイスからのデータ受信を復旧する処理を記述してください。
        return true;
    }
}
