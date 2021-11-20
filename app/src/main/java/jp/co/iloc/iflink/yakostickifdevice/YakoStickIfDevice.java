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

import java.nio.ByteBuffer;
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

public class YakoStickIfDevice<syncronized> extends DeviceConnector {
    /**
     * ログ出力用タグ名.
     */
    private static final String TAG = "YAKOSTICKIFDEVICE-DEV";
    /**
     * メッセージを取得するキー.
     */
    private static final String COLORANDBRIGHTNESS_KEY = "ColorAndBrightness";
    private static final String DRIVE_KEY = "Drive";
    private static final String COLOR_KEY = "Color";
    private static final String BRIGHTNESS_KEY = "Brightness";
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
     * Then処理用パラメータ
     */
    private final byte BRIGHTNESS_OFF   = (byte) 0;
    private final byte BRIGHTNESS_LOW   = (byte) 10;
    private final byte BRIGHTNESS_MID   = (byte) 30;
    private final byte BRIGHTNESS_HIGH  = (byte) 50;
    private byte m_drive = (byte) 'l';
    private byte m_red   = (byte) 5;
    private byte m_green = (byte) 5;
    private byte m_blue  = (byte) 5;
    private byte m_brightness = BRIGHTNESS_OFF;

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
        m_bluetoothGatt = null;
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
        m_bluetoothGatt.close();
        m_bluetoothGatt = null;
        if (bDBG) Log.i(TAG, "------------------------★ gatt disconnected and closed: ");
        // 送信停止が別途完了通知を受ける場合には、falseを返してください。
        return true;
    }

    @Override
    public boolean onJob(final HashMap<String, Object> map) {

        byte red   = m_red;
        byte green = m_green;
        byte blue  = m_blue;
        byte brightness = m_brightness;
        byte drive = m_drive;
        String tempStr;
        int temp;

        if (map.containsKey(DRIVE_KEY)) {
            /* int  ０:常時点灯 1:高速点滅 2:ゆっくり点滅 */
            tempStr = String.valueOf(map.get(DRIVE_KEY));
            temp = Integer.parseInt(tempStr);
            switch (temp) {
                case 0: // 常時点灯
                    if (bDBG) Log.i(TAG, "onJob DRIVE=0. TURN ON");
                    drive = (byte) 'l';
                    break;
                case 1: // 高速点滅
                    if (bDBG) Log.i(TAG, "onJob DRIVE=1. BRINK FAST");
                    drive = (byte) 'b';
                    break;
                case 2: // ゆっくり点滅
                    if (bDBG) Log.i(TAG, "onJob DRIVE=2. BRINK SLOW");
                    drive = (byte) 's';
                    break;
            }

        } else if (map.containsKey(COLOR_KEY)) {
            /* int  ０から白・赤・橙・黄・緑・水色・青・紫の順。 */
            tempStr = String.valueOf(map.get(COLOR_KEY));
            temp = Integer.parseInt(tempStr);
            switch (temp) {
                case 0: // 白
                    if (bDBG) Log.i(TAG, "onJob COLOR=0. drive white");
                    red = 5;
                    green = 5;
                    blue = 5;
                    break;
                case 1: // 赤
                    if (bDBG) Log.i(TAG, "onJob COLOR=1. drive red");
                    red = 5;
                    green = 0;
                    blue = 0;
                    break;
                case 2: // 橙
                    if (bDBG) Log.i(TAG, "onJob COLOR=2. drive orange");
                    red = 4;
                    green = 1;
                    blue = 0;
                    break;
                case 3: // 黄
                    if (bDBG) Log.i(TAG, "onJob COLOR=3. drive yellow");
                    red = 3;
                    green = 3;
                    blue = 0;
                    break;
                case 4: // 緑
                    if (bDBG) Log.i(TAG, "onJob COLOR=4. drive green");
                    red = 0;
                    green = 5;
                    blue = 0;
                    break;
                case 5: // 水色
                    if (bDBG) Log.i(TAG, "onJob COLOR=5. drive cyan");
                    red = 1;
                    green = 1;
                    blue = 4;
                    break;
                case 6: // 青
                    if (bDBG) Log.i(TAG, "onJob COLOR=6. drive blue");
                    red = 0;
                    green = 0;
                    blue = 5;
                    break;
                case 7: // 紫
                    if (bDBG) Log.i(TAG, "onJob COLOR=7. drive purple");
                    red = 3;
                    green = 0;
                    blue = 3;
                    break;
                default:
                    if (bDBG) Log.i(TAG, "onJob COLOR=invalid. ignore");
                    break;
            }

        } else if (map.containsKey(BRIGHTNESS_KEY)) {
            /* 明るさ int型 0:消灯 1:弱 2:中 3:強 */
            tempStr = String.valueOf(map.get(BRIGHTNESS_KEY));
            temp = (byte) Integer.parseInt(tempStr);
            switch (temp) {
                case 0: // 消灯
                    if (bDBG) Log.i(TAG, "onJob BRIGHTNESS=0. drive off");
                    brightness = BRIGHTNESS_OFF;
                    break;
                case 1: // 弱
                    if (bDBG) Log.i(TAG, "onJob BRIGHTNESS=1. drive low");
                    brightness = BRIGHTNESS_LOW;
                    break;
                case 2: // 中
                    if (bDBG) Log.i(TAG, "onJob BRIGHTNESS=2. drive mid");
                    brightness = BRIGHTNESS_MID;
                    break;
                case 3: // 強
                    if (bDBG) Log.i(TAG, "onJob BRIGHTNESS=3. drive high");
                    brightness = BRIGHTNESS_HIGH;
                    break;
                default:
                    break;
            }
        } else if (map.containsKey(COLORANDBRIGHTNESS_KEY)) {
            int coloaandbrightness = (int) map.get(COLORANDBRIGHTNESS_KEY);
            if (bDBG) Log.i(TAG, "onJob COLORANDBRIGHTNESS=" + coloaandbrightness + " nop");

        } else {
            if (bDBG) Log.d(TAG, "onJob no execute.");
        }

        // 変化があった場合のみデバイスに通知
        if ( drive != m_drive || red != m_red || green != m_green || blue != m_blue || brightness != m_brightness ) {
            m_drive = drive;
            m_red = red;
            m_green = green;
            m_blue = blue;
            m_brightness = brightness;
            // LED制御
            executeLedControl( drive, red, green, blue, brightness );
        }


        return true;
    }

    synchronized public void executeLedControl( byte drive, byte red, byte green, byte blue, byte brightness ) {
        BluetoothGattService service = m_bluetoothGatt.getService(UUID_PRIMARY_SERVICE);
        if (service != null) {
            BluetoothGattCharacteristic write_characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_WRITE);
            if (write_characteristic != null) {

                byte[] ledParam = new byte[5];
                ledParam[0] = (byte)'$';
                ledParam[1] = drive;
                ledParam[2] = (byte) (red * brightness);
                ledParam[3] = (byte) (green * brightness);
                ledParam[4] = (byte) (blue * brightness);

                Log.i(TAG, " executeLedControl : drive" + ledParam[1] + " red:" +  ledParam[2] + " green:" + ledParam[3] + " blue:" +ledParam[4]);

                // デバイスに通知.
                write_characteristic.setValue(ledParam);
                write_characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                m_bluetoothGatt.writeCharacteristic(write_characteristic);
                return;
            }
        }
        Log.d(TAG, " executeLedControl : no execute.");
    }

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
        if (m_connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            if (bDBG) Log.i(TAG, "------------------------★ startScan");
            m_scanner.startScan(m_scanFilters, m_scanSettings, m_scanCallback);
            m_startDeviceRequest = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    m_scanner.stopScan(m_scanCallback);
                }
            }, SCAN_PERIOD);
        }
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
            if ( m_bluetoothGatt != null ) {
                m_bluetoothGatt.disconnect();
                m_bluetoothGatt.close();
                m_bluetoothGatt = null;
            }
            m_bluetoothGatt = m_device.connectGatt( mIms.getApplicationContext(), false, gattCallback );
            m_scanner.stopScan(m_scanCallback);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (bDBG) Log.i(TAG,"----------------------★ onBatchScanResults");
        };

        @Override
        public void onScanFailed(int errorCode) {
            if (bDBG) Log.i(TAG,"----------------------★ onScanFailed:" + errorCode );
        };
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        private byte[] m_data = new byte[7];

        private void initData() {
            for ( int i = 0 ; i < 7 ; i++ ) {
                m_data[i] = (byte) 0xFF;
            }
        }
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (bDBG) Log.i(TAG, "------------------★★★ Connected to GATT Server:" + status);
                boolean ret = m_bluetoothGatt.discoverServices();
                if (bDBG) Log.i(TAG, "Attempting to start service discovery:" + ret );
                m_connectionState = newState;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (bDBG) Log.i(TAG, "----------------------★ Disconnected from GATT server.");
                m_dev.notifyStopDevice();
                m_connectionState = newState;
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (bDBG) Log.i(TAG, "----------------★★★★ onServicesDiscovered gatt success");
                if ( m_startDeviceRequest == true ) {
                    initData();
                    enableNotification();
                    m_dev.notifyCompleteStartDevice(true);
                }
            } else {
                if (bDBG) Log.i(TAG, "onServicesDiscovered received: " + status);
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
            byte checksum = 0;
            for ( int i = 0 ; i < 6 ; i++ ) {
                m_data[i] = m_data[i+1];
                checksum += m_data[i];
            }
            m_data[6] = data[0];
            if ( m_data[6] == checksum ) {
                // xのみ0xFFFFならSensorOnOffにONを設定
                if (m_data[0] == -1 && m_data[1] == -1 &&
                        m_data[2] == 0 && m_data[3] == 0 &&
                        m_data[4] == 0 && m_data[5] == 0) {
                    if (bDBG) Log.i(TAG, "send data : SensorOnOff=ON");
                    // 送信データクリア
                    clearData();
                    // データの登録.
                    addData(new EPAdata("SensorOnOff", "int", String.valueOf(1)));
                    //ifLink Coreへデータを送信する.
                    notifyRecvData();
                    initData();
                }

                // xとyが0xFFFFならSensorOnOffにOFFを設定
                if (m_data[0] == -1 && m_data[1] == -1 &&
                        m_data[2] == -1 && m_data[3] == -1 &&
                        m_data[4] == 0 && m_data[5] == 0) {
                    if (bDBG) Log.i(TAG, "send data : SensorOnOff=OFF");
                    // 送信データクリア
                    clearData();
                    // データの登録.
                    addData(new EPAdata("SensorOnOff", "int", String.valueOf(0)));
                    //ifLink Coreへデータを送信する.
                    notifyRecvData();
                    initData();
                }

                if ( !(m_data[0] == -1 && m_data[1] == -1) &&
                        !(m_data[2] == -1 && m_data[3] == -1) &&
                        !(m_data[4] == -1 && m_data[5] == -1)) {
                    byte[] xBytes = {m_data[1], m_data[0]};
                    byte[] yBytes = {m_data[3], m_data[2]};
                    byte[] zBytes = {m_data[5], m_data[4]};
                    int xVal = ByteBuffer.wrap(xBytes).getShort();
                    int yVal = ByteBuffer.wrap(yBytes).getShort();
                    int zVal = ByteBuffer.wrap(zBytes).getShort();

                    if (bDBG)
                        Log.i(TAG, "send data :" + String.format("AdVal X=%d Y=%d Z=%d", xVal, yVal, zVal));

                    // 送信データクリア
                    clearData();
                    // データの登録.
                    addData(new EPAdata("AdValX", "int", String.valueOf(xVal)));
                    addData(new EPAdata("AdValY", "int", String.valueOf(yVal)));
                    addData(new EPAdata("AdValZ", "int", String.valueOf(zVal)));
                    //ifLink Coreへデータを送信する.
                    notifyRecvData();
                    initData();
                }
            }
        }
    };

    /**
     * センサーの値は通知によって行う。ここでは通知を有効にするためにCharacteristicNotificationを書き込む。
     * @return
     */
    public boolean enableNotification() {
        BluetoothGattService service = m_bluetoothGatt.getService(UUID_PRIMARY_SERVICE);
        if (service != null) {
            if (bDBG) Log.i(TAG, "-----------------★★★★★ enable notification");
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_READ);
            m_bluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            m_bluetoothGatt.writeDescriptor(descriptor);
            return true;
        } else {
            if (bDBG) Log.i(TAG, "------------------------★ service　is null: ");
            m_dev.notifyStopDevice();
            m_connectionState = BluetoothProfile.STATE_DISCONNECTED;
        }
        return false;
    }
}

