package jp.co.iloc.iflink.yakostickifdevice;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Looper;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

//Toast Sample start
import android.widget.Toast;
//Toast Sample end

import java.util.Timer;
import java.util.UUID;
import jp.co.toshiba.iflink.epaapi.EPAdata;

import jp.co.toshiba.iflink.imsif.IfLinkConnector;
import jp.co.toshiba.iflink.imsif.DeviceConnector;
import jp.co.toshiba.iflink.imsif.IfLinkSettings;
import jp.co.toshiba.iflink.imsif.IfLinkAlertException;
import jp.co.toshiba.iflink.ui.PermissionActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

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
    //If Sample end

    /**
     * 自分自身
     */
    YakoStickIfDevice m_dev;

    /**
     * Bluetooth le 接続用
     */
    private BluetoothAdapter m_adapter;
    private BluetoothLeScanner m_scanner;
    private BleScancallback m_scanCallback;
    private TYOriginalServiceRead m_read;
    private BluetoothDevice m_device;
    private ArrayList<ScanFilter> m_scanFilters;
    private ScanSettings m_scanSettings;
    private BluetoothGatt m_bluetoothGatt;
    private static final long SCAN_PERIOD = 10000;
    private boolean m_startDeviceRequest = false;

    /**
     * m_characteristic設定用UUID
     */
    private final UUID UUID_PRIMARY_SERVICE        = UUID.fromString( "442F1570-8A00-9A28-CBE1-E1D4212D53EB" );
    private final UUID UUID_CHARACTERISTIC_READ    = UUID.fromString( "442F1571-8A00-9A28-CBE1-E1D4212D53EB" );
    private final UUID UUID_CHARACTERISTIC_WRITE   = UUID.fromString( "442F1572-8A00-9A28-CBE1-E1D4212D53EB" );
    private final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb" );

    /**
     * Bluetooth接続状態
     */
    private int m_connectionState = BluetoothProfile.STATE_DISCONNECTED;

    /**
     * コンストラクタ.
     *
     * @param ims IMS
     */
    public YakoStickIfDevice(final IfLinkConnector ims, BluetoothAdapter adapter) {
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

        m_adapter = adapter;
        m_dev = this;
        // サンプル用：ここでデバイスを登録します。
        // 基本は、デバイスとの接続確立後、デバイスの対応したシリアル番号に更新してからデバイスを登録してください。
        addDevice();
        //スキャンの開始
        m_scanner = m_adapter.getBluetoothLeScanner();
        m_scanCallback = new BleScancallback();

        ScanFilter ｓcanFilter = new ScanFilter.Builder().setDeviceName("YakoStickProto1").build();
        m_scanFilters = new ArrayList<>();
        m_scanFilters.add(ｓcanFilter);
        m_scanSettings = new ScanSettings.Builder().build();

        if (bDBG) Log.i(TAG, "------------------------★ startScan");
        m_scanner.startScan( m_scanFilters, m_scanSettings, m_scanCallback );

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_scanner.stopScan(m_scanCallback);
            }
        }, SCAN_PERIOD);

        // 基本は、デバイスとの接続が確立した時点で呼び出します。
        notifyConnectDevice();
    }

    @Override
    public boolean onStartDevice() {
        if (bDBG) Log.d(TAG, "onStartDevice");
        // 送信開始が別途完了通知を受ける場合には、falseを返してください。
        if (m_connectionState == BluetoothProfile.STATE_CONNECTED) {
            // 通知を有効にする
            return enableNotification();
        } else {
            if (bDBG) Log.i(TAG, "------------------------★ startScan");
            m_scanner.startScan( m_scanFilters, m_scanSettings, m_scanCallback );
            m_startDeviceRequest = true;
        }
        return false;
    }

    @Override
    public boolean onStopDevice() {
        if (bDBG) Log.d(TAG, "onStopDevice");
        // デバイスからのデータ送信停止処理を記述してください。
        m_bluetoothGatt.disconnect();
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
        return new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    @Override
    protected void onPermissionGranted() {
        // パーミッションを許可された後の処理を記述してください。
    }


    @Override
    public final boolean checkPathConnection() {
        if (bDBG) Log.d(TAG, "checkPathConnection");
        // デバイスとの接続経路(WiFi, BLE, and so on・・・)が有効かをチェックする処理を記述してください。
        if (m_connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            return false;
        }
        return true;
    }

    @Override
    public final boolean reconnectPath() {
        if (bDBG) Log.d(TAG, "reconnectPath");
        // デバイスとの接続経路(WiFi, BLE, and so on・・・)を有効にする処理を記述してください。
        if (bDBG) Log.i(TAG, "------------------------★ startScan");
        m_scanner.startScan( m_scanFilters, m_scanSettings, m_scanCallback );
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                m_scanner.stopScan(m_scanCallback);
            }
        }, SCAN_PERIOD);
        return true;
    }

    @Override
    public final boolean checkDeviceConnection() {
        if (bDBG) Log.d(TAG, "checkDeviceConnection");
        // デバイスとの接続が維持されているかをチェックする処理を記述してください。
        if (m_connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            return false;
        }
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
        if (m_connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            return false;
        }
        return true;
    }

    @Override
    public final boolean resendDevice() {
        if (bDBG) Log.d(TAG, "resendDevice");
        // デバイスからのデータ受信を復旧する処理を記述してください。
        return true;
    }

    class BleScancallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (bDBG) Log.d(TAG,"onScanResult");
            m_device = result.getDevice();
            if( m_device == null ) {
                Log.d(TAG,"device is null");
                return;
            }
            if (bDBG) Log.i(TAG, "--------------------★★ address:" + m_device.getAddress());
            if (bDBG) Log.i(TAG, "--------------------★★ name:" + m_device.getName());

            m_bluetoothGatt = m_device.connectGatt( mIms.getApplicationContext(), false, gattCallback );
            m_scanner.stopScan(m_scanCallback);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG,"----------------------★ ");
        };

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG,"----------------------★ onScanFailed:" + errorCode );
        };
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            m_connectionState = newState;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (bDBG) Log.i(TAG, "------------------★★★ Connected to GATT Server" + status);
                boolean ret = m_bluetoothGatt.discoverServices();
                if (bDBG) Log.i(TAG, "Attempting to start service discovery:" + ret );
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (bDBG) Log.i(TAG, "----------------------★ Disconnected from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (bDBG) Log.i(TAG, "----------------★★★★ onServicesDiscovered gatt success");
                if ( m_startDeviceRequest == true ) {
                    enableNotification();
                    m_dev.notifyCompleteStartDevice(true);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                if ( m_startDeviceRequest == true ) {
                    m_dev.notifyCompleteStartDevice(false );
                }
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (bDBG) Log.d(TAG, "onCharacteristicRead gatt success");
            }
        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (bDBG) Log.d(TAG, "notify data :" + String.format("0x%02x", data[0]));
        }
    };

    /**
     * センサーの値は通知によって行う。ここでは通知を有効にするためにCharacteristicNotificationを書き込む。
     * @return
     */
    public boolean enableNotification() {
        BluetoothGattService service = m_bluetoothGatt.getService(UUID_PRIMARY_SERVICE);
        if (service != null) {
            Log.w(TAG, "-----------------★★★★★ enable notification");
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_READ);
            m_bluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            m_bluetoothGatt.writeDescriptor(descriptor);
            return true;
        } else {
            Log.w(TAG, "------------------------★ service　is null: ");
            m_connectionState = BluetoothProfile.STATE_DISCONNECTED;
        }
        return false;
    }
}

