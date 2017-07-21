package com.bibby.howtouseble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BleInstance {

	private final String TAG = "BleInstance";
	private final boolean D = true;
	
	String tpms_device;
	Handler ApplicationHandler;
	Context context;
	
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;
	private BroadcastReceiver mReceiver;

	public final static int MSG_GOT_DATA = 3333;
	public final static int MSG_GOT_NOTSUPPORTBLE = 5555;
	public final static int MSG_GOT_NODEVICE = 6666;
	public final static int MSG_GOT_HAVEDEVICE = 7777;
	public final static int MSG_GOT_CONNECTED = 8888;
	public final static int MSG_GOT_DISCONNECTED = 9999;
	public final static int MSG_GOT_OPENBT = 11111;
	public final static int MSG_GOT_STARTREAD = 22222;
	
    private boolean mScanning;  
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
	
	private static final int REQUEST_ENABLE_BT = 8000;

    public final static String ACTION_GATT_CONNECTED =
            "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "EXTRA_DATA";
	
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    ArrayList<BluetoothGattCharacteristic> Characteristic_to_check =
            new ArrayList<BluetoothGattCharacteristic>();
    
    private boolean have_search_device = false;
    private boolean hava_connected_device = false;
    
    private UUID serviceUuid;
    private UUID writeUuid;
    private UUID[] readUuids;
    
    private BluetoothDevice ble_device;
    
    private boolean reading = true;
	
	public BleInstance(final Context context, final String device, final Handler handler, final UUID serviceUuid, final UUID writeUuid, final UUID[] readUuids){
		this.context = context;
		this.tpms_device = device;
		this.ApplicationHandler = handler;
		
		this.serviceUuid = serviceUuid;
		this.writeUuid = writeUuid;
		this.readUuids = readUuids;
		
		mHandler = new Handler();

    	mReceiver = new BroadcastReceiver() {
    		public void onReceive(Context context, Intent intent) {
    		    String action = intent.getAction();

    		    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
    	            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
    	                                                 BluetoothAdapter.ERROR);
    	            switch (state) {
	    	            case BluetoothAdapter.STATE_OFF:
	    	            	Log.d("ACTION_STATE_CHANGED","STATE_OFF");
	    	                break;
	    	            case BluetoothAdapter.STATE_TURNING_OFF:
	    	            	Log.d("ACTION_STATE_CHANGED","STATE_TURNING_OFF");
	    	                break;
	    	            case BluetoothAdapter.STATE_ON:
	    	            	Log.d("ACTION_STATE_CHANGED","STATE_ON");
							Message message = new Message();
							message.what = MSG_GOT_OPENBT;
							ApplicationHandler.sendMessage(message);
							scanLeDevice(true);
	    	                break;
	    	            case BluetoothAdapter.STATE_TURNING_ON:
	    	            	Log.d("ACTION_STATE_CHANGED","STATE_TURNING_ON");
	    	                break;
    	            }
    	        }
    		    
    		 }
    	};
    	
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    	context.registerReceiver(mReceiver, filter);

		if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
			if(D) Log.d("FEATURE_BLUETOOTH","判斷支援藍芽");
			
			if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
				if(D) Log.d("FEATURE_BLUETOOTH_LE","判斷支援藍芽BLE");
				
				if(initialize()){
					if( !mBluetoothAdapter.isEnabled() ){
						if(D) Log.d(TAG,"藍芽未開啟");
						if(mBluetoothAdapter.enable()){
							if(D) Log.d(TAG,"藍芽已自動開啟");
							//Message message = new Message();
							//message.what = TpmsApplication.MSG_GOT_OPENBT;
							//ApplicationHandler.sendMessage(message);
						}
						else{
							if(D) Log.d(TAG,"藍芽未自動開啟");
							//Message message = new Message();
							//message.what = TpmsApplication.MSG_GOT_OPENBT;
							//ApplicationHandler.sendMessage(message);
				            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				            //context.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
						}
					}
					else{
						if(D) Log.d(TAG,"藍芽已開啟");
						scanLeDevice(true);
					}
				}
			}
			else{
				if(D) Log.d("FEATURE_BLUETOOTH_LE","判斷不支援藍芽BLE");
				Message message = new Message();
				message.what = MSG_GOT_NOTSUPPORTBLE;
				ApplicationHandler.sendMessage(message);
			}
		}
		else{
			if(D) Log.d("FEATURE_BLUETOOTH","判斷不支援藍芽");
			Message message = new Message();
			message.what = MSG_GOT_NOTSUPPORTBLE;
			ApplicationHandler.sendMessage(message);
		}

	}
	
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                if(D) Log.e("initialize()", "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            if(D) Log.e("initialize()", "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }
    
    public void scanLeDevice(final boolean enable) {  
        if (enable) {
        	if(D) Log.d(TAG,"開始搜尋對應的藍芽裝置");
            mHandler.postDelayed(new Runnable() {  
                @Override  
                public void run() {
                	if(!have_search_device){
	                	if(D) Log.d(TAG,"搜尋時間到,未搜尋到對應的藍芽裝置");
	                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
	                    
						Message message = new Message();
						message.what = MSG_GOT_NODEVICE;
						ApplicationHandler.sendMessage(message);
	                    
	            		//Toast.makeText(context, "未搜尋到對應的裝置!!", Toast.LENGTH_LONG).show();
                	}
                }  
            }, SCAN_PERIOD);
 
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            
        } else {
        	if(D) Log.d(TAG,"停止搜尋對應的藍芽裝置");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);  
        } 
    }
    
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    	
    	@Override  
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
    		
    		if(device.getName() != null){
	    		if(D) Log.i("getName",device.getName());
	    		if(D) Log.i("getAddress",device.getAddress());
	    		if(D) Log.i("getBondState",device.getBondState()+"");
	    		if(D) Log.i("getType",device.getType()+"");
	    		if(D) Log.i("rssi",rssi+"");
	    		
	    		if( have_search_device==false && device.getName().equalsIgnoreCase(tpms_device) ){
	    			mBluetoothAdapter.stopLeScan(mLeScanCallback);
	    			
	    			if(D) Log.d(TAG,"搜尋到對應的藍芽裝置");
	    			have_search_device = true;
	    			
	    			ble_device = device;
	    			
					Message message = new Message();
					message.what = MSG_GOT_HAVEDEVICE;
					ApplicationHandler.sendMessage(message);
	    			
	    			//mBluetoothAdapter.stopLeScan(mLeScanCallback);
	    			
//					if(device.getBondState()==BluetoothDevice.BOND_NONE){
//				        try {
//				            Method method = device.getClass().getMethod("createBond", (Class[]) null);
//				            method.invoke(device, (Object[]) null);
//				        } catch (Exception e) {
//				            e.printStackTrace();
//				        }
//					}
//					else if(device.getBondState()==BluetoothDevice.BOND_BONDING){
//						
//					}
//					else if(device.getBondState()==BluetoothDevice.BOND_BONDED){
//						
//					}
					
	    			connectLeDevice(ble_device);
	    		}
    		}
    		
    	}

    };
    
    private void connectLeDevice(BluetoothDevice device) {
    	mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
    }
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
        	
            String intentAction;
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                
                Log.i(TAG, "Connected to GATT server.");
                
                //20150917
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                //connectReadUUID(READ_UUID.toString().toUpperCase(), 0);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Disconnected from GATT server.");
            }
            
            else {
                Log.e(TAG, "Connection state changed.  New state: " + newState);
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
            
			Message message = new Message();
			message.what = MSG_GOT_STARTREAD;
			ApplicationHandler.sendMessage(message);
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
        	Log.e("onCharacteristicRead","onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {

        		final byte[] data = characteristic.getValue();
        		//Log.d("data.length",String.valueOf(data.length));
        		if (data != null && data.length > 0) {
        			final StringBuilder stringBuilder = new StringBuilder(data.length);
        			for(byte byteChar : data)
        				stringBuilder.append(String.format("%02X ", byteChar));
        			
                    if(D) Log.e(TAG,"onCharRead "+gatt.getDevice().getName()  
                            + " read "  
                            + characteristic.getUuid().toString()  
                            + " -> "  
                            + stringBuilder.toString());
        			
        			//if(D) Log.d("",stringBuilder.toString());

        			//intent.putExtra(EXTRA_DATA, new String(data));
        			//intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        			//intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        		}

                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                
            } else {
                Log.e(TAG, "onCharacteristicRead received: " + status);
            }
        }

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			//super.onCharacteristicWrite(gatt, characteristic, status);
			
    		final byte[] data = characteristic.getValue();
    		//Log.d("data.length",String.valueOf(data.length));
    		if (data != null && data.length > 0) {
    			final StringBuilder stringBuilder = new StringBuilder(data.length);
    			for(byte byteChar : data)
    				stringBuilder.append(String.format("%02X ", byteChar));
    			
                if(D) Log.e(TAG,"onCharWrite "+gatt.getDevice().getName()  
                        + " write "  
                        + characteristic.getUuid().toString()  
                        + " -> "  
                        + stringBuilder.toString());
    			
    			//if(D) Log.d("",stringBuilder.toString());

    			//intent.putExtra(EXTRA_DATA, new String(data));
    			//intent.putExtra(EXTRA_DATA, stringBuilder.toString());
    			//intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
    		}

		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			//super.onCharacteristicChanged(gatt, characteristic);
			Log.d("onCharacteristicChanged","onCharacteristicChanged");

    		final byte[] data = characteristic.getValue();
    		//Log.d("data.length",String.valueOf(data.length));
    		if (data != null && data.length > 0) {
    			final StringBuilder stringBuilder = new StringBuilder(data.length);
    			for(byte byteChar : data)
    				stringBuilder.append(String.format("%02X ", byteChar));
    			
                if(D) Log.e(TAG,"onCharChange "+gatt.getDevice().getName()  
                        + " read "  
                        + characteristic.getUuid().toString()  
                        + " -> "  
                        + stringBuilder.toString());
    			
    			if(D) Log.d("",stringBuilder.toString());

    			//intent.putExtra(EXTRA_DATA, new String(data));
    			//intent.putExtra(EXTRA_DATA, stringBuilder.toString());
    			//intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
    		}

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

		}

		@Override
		public void onDescriptorRead(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			//super.onDescriptorRead(gatt, descriptor, status);
			Log.d("onDescriptorRead","onDescriptorRead");
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status) {
			//super.onDescriptorWrite(gatt, descriptor, status);
			Log.d("onDescriptorWrite","onDescriptorWrite");
		}

		@Override
		public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
			//super.onReliableWriteCompleted(gatt, status);
			Log.d("onReliableWriteCompleted","onReliableWriteCompleted");
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			//super.onReadRemoteRssi(gatt, rssi, status);
			Log.d("onReadRemoteRssi","onReadRemoteRssi");
		}

    };
	
    private void broadcastUpdate(final String action) {
        //final Intent intent = new Intent(action);

        if (ACTION_GATT_CONNECTED.equals(action)) {
        	hava_connected_device = true;
			Message message = new Message();
			message.what = MSG_GOT_CONNECTED;
			ApplicationHandler.sendMessage(message);
            if(D) Log.i(TAG, "連線成功");
            
            reading = true;
            
        } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
        	have_search_device = false;
        	hava_connected_device = false;
			Message message = new Message();
			message.what = MSG_GOT_DISCONNECTED;
			ApplicationHandler.sendMessage(message);
            if(D) Log.i(TAG, "連線中斷");
            
            reading = false;
            
            //disconnect();
            //close();
        } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            // Show all the supported services and characteristics on the user interface.
            displayGattServices(getSupportedGattServices());
            if(D) Log.i(TAG, "列出所有的UUID服務");

            connectReadUUIDs(this.readUuids, 2);
            
//            for(UUID uuid : this.readUuids){
//            	connectReadUUID(uuid, 2);
//            }
//            connectReadUUID(TEMP_UUID.toString().toUpperCase(), 2);
//            connectReadUUID(HUMI_UUID.toString().toUpperCase(), 2);
//            connectReadUUID(PRESSURE_UUID.toString().toUpperCase(), 2);
        }

    }
    
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        
        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristic";
        
        //ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>(); // Service 顯示用
        
        //ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
        //        = new ArrayList<ArrayList<HashMap<String, String>>>(); // Characteristic 顯示用

        
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
        	
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            
            
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, unknownCharaString);
            currentServiceData.put(LIST_UUID, uuid);
            //gattServiceData.add(currentServiceData);
            
            
            if(D) Log.i(TAG, "- service uuid:"+uuid);
            if(D) Log.i(TAG, "- service type:"+gattService.getType()); // 0:primary 1:secondary
            if(D) Log.i(TAG, "- includedServices size:"+gattService.getIncludedServices().size());  
            
            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

            	charas.add(gattCharacteristic);
            	Characteristic_to_check.add(gattCharacteristic); // bibby比對用
                
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                
                
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, unknownCharaString);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                
                
                if(D) Log.d(TAG, "  - chara uuid:" + uuid);
                if(D) Log.d(TAG, "  - chara properties:" + gattCharacteristic.getProperties()); // 10:read&write 2:read 32:indicate 18:read&notify 4:write_no_response
                if(D) Log.d(TAG, "  - chara permissions:" + gattCharacteristic.getPermissions());
                if(D) Log.d(TAG, "  - chara value:" + gattCharacteristic.getValue());

                
                // Loops through available Descriptors.
                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();  
                for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {  
                    if(D) Log.e(TAG, "    - desc uuid:" + gattDescriptor.getUuid());  
                    int descPermission = gattDescriptor.getPermissions();
                    if(D) Log.e(TAG,"    - desc permission:"+ descPermission);
                      
                    byte[] desData = gattDescriptor.getValue();  
                    if (desData != null && desData.length > 0) {  
                    	if(D) Log.e(TAG, "    - desc value:"+ new String(desData));  
                    }  
                } 
            }
            
            //mGattCharacteristics.add(charas);
            //gattCharacteristicData.add(gattCharacteristicGroupData);
        }
        
        if(D) Log.d(TAG,"列出所有服務結束");

    }
    
    private void broadcastUpdate(final String action,
            final BluetoothGattCharacteristic characteristic) {
    	
    	if(D) Log.i(TAG, "收到資料");
    	final Intent intent = new Intent(action);
    	
		// For all other profiles, writes the data formatted in HEX.
		final byte[] data = characteristic.getValue();
		//if(D) Log.d("data.length",String.valueOf(data.length));
		if (data != null && data.length > 0) {
			
			//byte buffer[] = new byte[20];
			//int readByte = 0;

			Message message = new Message();
			message.what = MSG_GOT_DATA;
			// 151120 : change obj from data to characteristic
			message.obj = characteristic;
			ApplicationHandler.sendMessage(message);
			
		}

    }
    
    public void disconnect() {
    	
    	if(mBluetoothAdapter != null){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
    	}
    	
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.disconnect();
    }

    public void close() {
    	
		//context.unregisterReceiver(mGattUpdateReceiver);
		//context.unbindService(mServiceConnection);
    	reading = false;
    	
    	if(mReceiver!=null)
    		context.unregisterReceiver(mReceiver);
    	
        if (mBluetoothGatt == null) {
            return;
        }
        
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        
    }
    
    public void connectReadUUID(UUID targetUuid, int mode) {
    	Log.d("connectReadUUID","start");
    	
//    	UUID UUID_CHECK = UUID.fromString(uuid);

        	Iterator<BluetoothGattCharacteristic> itr = Characteristic_to_check.iterator();
        	while (itr.hasNext()) {
        		BluetoothGattCharacteristic element = itr.next();
        		
        		if(D) Log.d(TAG,element.getUuid().toString().toUpperCase());
        		
        		if(element.getUuid().compareTo(targetUuid)==0){
        			if(D) Log.e(TAG,"connectReadUUID 1 此裝置提供此服務");

                    final int charaProp = element.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    	Log.d("PROPERTY_READ","PROPERTY_READ");
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                    	mBluetoothGatt.setCharacteristicNotification(
                        		element, false);
//                        mBluetoothGatt.readCharacteristic(element);
                        new ReadCharCharacteristic(element).start();
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    	Log.d("PROPERTY_NOTIFY","PROPERTY_NOTIFY");

                    	mBluetoothGatt.setCharacteristicNotification(element, true);
                    }
                    
    				//Message message = new Message();
    				//message.what = TpmsApplication.MSG_GOT_STARTREAD;
    				//ApplicationHandler.sendMessage(message);
                    
        			break;
        		}
        	}
    }
    
    private class ReadCharCharacteristic extends Thread {
        private BluetoothGattCharacteristic to_read;
        
        public ReadCharCharacteristic(BluetoothGattCharacteristic to_read) {
            this.to_read = to_read;
        }
    	
		@Override
		public void run() {
			super.run();
			
			while(reading){
				
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if(mBluetoothGatt!=null)
					mBluetoothGatt.readCharacteristic(to_read);

			}
		}
    }
    
	public void connectReadUUIDs(UUID[] uuidsToRead, int mode) {
		Log.d("connectReadUUID", "start");

		// UUID UUID_CHECK = UUID.fromString(uuid);

		ArrayList<BluetoothGattCharacteristic> characteristicsToRead = new ArrayList<BluetoothGattCharacteristic>();
		Iterator<BluetoothGattCharacteristic> itr = Characteristic_to_check.iterator();
		while (itr.hasNext()) {
			BluetoothGattCharacteristic element = itr.next();
			if (D)
				Log.d(TAG, element.getUuid().toString().toUpperCase());
			for (UUID uuidToRead : uuidsToRead) {
				if (element.getUuid().compareTo(uuidToRead) == 0) {
				//if (element.getUuid()==uuidToRead) {	
					if (D)Log.e(TAG, "connectReadUUID 1 此裝置提供此服務");

					final int charaProp = element.getProperties();
					if (D)Log.e(TAG,"element.getProperties() : " + charaProp);

					if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
						Log.d("PROPERTY_READ", "PROPERTY_READ");
						
						// If there is an active notification on a
						// characteristic, clear
						// it first so it doesn't update the data field on the
						// user interface.
						
						mBluetoothGatt.setCharacteristicNotification(element, false);
						
						// mBluetoothGatt.readCharacteristic(element);
						// new ReadCharCharacteristic(element).start();
//						characteristicsToRead.add(element);
					}
					
					if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
						Log.d("PROPERTY_NOTIFY", "PROPERTY_NOTIFY");
						
						mBluetoothGatt.setCharacteristicNotification(element, true);	
//						characteristicsToRead.add(element);

                    	UUID uuid = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
                    	BluetoothGattDescriptor descriptor = element.getDescriptor(uuid);
                    	descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    	mBluetoothGatt.writeDescriptor(descriptor);
					}
					
					// Message message = new Message();
					// message.what = TpmsApplication.MSG_GOT_STARTREAD;
					// ApplicationHandler.sendMessage(message);
				}
			}

		}
		
//		if(characteristicsToRead.size()!=0){
//			new ReadCharCharacteristics(characteristicsToRead).start();
//		}
	}

    private class ReadCharCharacteristics extends Thread {
    	ArrayList<BluetoothGattCharacteristic> characteristicsToRead = new ArrayList<BluetoothGattCharacteristic>();
        
        public ReadCharCharacteristics(final ArrayList<BluetoothGattCharacteristic> characteristicsToRead) {
        	for(BluetoothGattCharacteristic characteristic : characteristicsToRead){
//        		if(characteristic.getService().getUuid().compareTo(serviceUuid)==0){
	        		this.characteristicsToRead.add(characteristic);
	        		Log.d("Read char thread", "add service uuid = "+characteristic.getService().getUuid().toString());
	        		Log.d("Read char thread", "add char uuid = "+characteristic.getUuid().toString());
//        		}
        	}
        }
    	
		@Override
		public void run() {
			super.run();
			
			int step = 0;
			while(reading){
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(mBluetoothGatt!=null){
					mBluetoothGatt.readCharacteristic(this.characteristicsToRead.get(step));
				}
				if(step==this.characteristicsToRead.size()-1){
					step = 0;
				}
				else{
					step ++;
				}
			}
		}
    }
    
    
    public void sendCommand(final byte[] command) {
    	connectWriteUUID(writeUuid.toString().toUpperCase(), command, 2);
    }
    
    public void connectWriteUUID(String uuid, byte[] array, int mode) {
    	Log.d("connectWriteUUID","start");
    	
    	UUID UUID_CHECK = UUID.fromString(uuid);

    	if(mode==0){   	
	    	UUID DESC1_UUID = UUID
	    			.fromString("00002901-0000-1000-8000-00805F9B34FB");
	
	    	BluetoothGattCharacteristic bluetoothGattCharacteristic = new BluetoothGattCharacteristic(UUID_CHECK, 8, 0);
	
	        BluetoothGattDescriptor descriptor1 = new BluetoothGattDescriptor(DESC1_UUID, 0);
	        
	        bluetoothGattCharacteristic.addDescriptor(descriptor1);
	    	
	        final int charaProp = bluetoothGattCharacteristic.getProperties();
	        Log.d("charaProp",""+charaProp);
	        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
	        	bluetoothGattCharacteristic.setValue(array);  
	            mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
	            //mBluetoothGatt.writeDescriptor(descriptor);
	        }
    	}
    	else if(mode==1){
	    	Iterator<BluetoothGattCharacteristic> itr = Characteristic_to_check.iterator();
	    	while (itr.hasNext()) {
	    		BluetoothGattCharacteristic element = itr.next();
	    		
	    		if(D) Log.d(TAG,element.getUuid().toString().toUpperCase());
	    		
	    		if(element.getUuid().toString().toUpperCase().equals(uuid)){
	    			if(D) Log.e(TAG,"connectWriteUUID 1 此裝置提供此服務");
	
	                final int charaProp = element.getProperties();
	                Log.d("charaProp",""+charaProp);
	                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
	                	element.setValue(array);  
	                    mBluetoothGatt.writeCharacteristic(element);
	                    //mBluetoothGatt.writeDescriptor(descriptor);
	                }
	
	    			break;
	    		}
	    	}
    	}
    	else{
    		if(mBluetoothGatt!=null){
	        	BluetoothGattService RxService = mBluetoothGatt.getService(serviceUuid);
	        	if (RxService == null) {
	        		Log.d("connectWriteUUID","Rx service not found!");
	                return;
	            }
	        	BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(writeUuid);
	            if (RxChar == null) {
	            	Log.d("connectWriteUUID","Rx charateristic not found!");
	                return;
	            }
	            
	            if(D) Log.e(TAG,"connectWriteUUID 2 此裝置提供此服務");
	            
	            final int charaProp = RxChar.getProperties();
	            Log.d("charaProp",""+charaProp);
	            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
	            	RxChar.setValue(array);  
	            	boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
	            	Log.d(TAG, "write RXchar - status=" + status);
	                //mBluetoothGatt.writeDescriptor(descriptor);
	            }
    		}
    	}
    }
    
}
