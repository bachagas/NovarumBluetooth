/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.novarum.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiIntentWrapper;
import org.appcelerator.kroll.common.Log;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;

@Kroll.module(name="Novarumbluetooth", id="com.novarum.bluetooth")
public class NovarumbluetoothModule extends KrollModule
{

	// Standard Debugging variables
	private static final String TAG = "NovarumbluetoothModule";
	
	       BluetoothDevice  bluetoothDevice  = null;
	static BluetoothAdapter bluetoothAdapter = null;
	       KrollDict devicelist              = null;
    public static BluetoothSocket  btsocket;
    
    public static String DEFAULT_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    
    private static InputStream inputStream   = null;
    private static OutputStream outputStream = null;
    public boolean isConnected               = false; 
    public dataReceiver datareceiver;
    public AcceptThread acceptthread;
    public String SERVERNAME                 = "NovarumBluetooth";
    public static NovarumbluetoothModule staticNovarumbluetoothModule;
    public boolean useService                = false;
    TiIntentWrapper BluetoothServiceIntent = null;
    
	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant public static final String EXTERNAL_NAME = value;
	
	public NovarumbluetoothModule()
	{
		super();
		staticNovarumbluetoothModule = this;
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(TAG, "inside onAppCreate");
		// put module init code that needs to run when the application is created
		
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
	}
	

	// Methods
	@Kroll.method
	public String example()
	{
		Log.d(TAG, "example called");
		return "hello world";
	}
	
	
	@Kroll.method
	public void useService()
	{
		Log.d(TAG, "useService called");
		this.useService = true;
	}	
	

	@Kroll.method
	public boolean searchDevices()
	{
		Log.d(TAG, "searchDevices called");
		
		//Halilk: if not enabled, enable bluetooth
		enableBluetooth();
		
		//Get Current activity//
		TiApplication appContext = TiApplication.getInstance();
		Activity activity = appContext.getCurrentActivity();		
		
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        activity.registerReceiver(myReceiver, intentFilter);
        bluetoothAdapter.cancelDiscovery(); //cancel if it's already searching
        bluetoothAdapter.startDiscovery();		
	
		return true;
	}	
	
	
	@Kroll.method
	public void enableBluetooth()
	{
        if(!bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.enable();
            Log.i(TAG, "Bluetooth Enabled");
        }		
	}
	
	@Kroll.method
	public void disableBluetooth()
	{
        if(bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.disable();
            Log.i(TAG, "Bluetooth Disabled");
        }		
	}	
	
	
	@Kroll.method
	public void destroy()
	{
		Log.d(TAG, "destroy called");
		
		//Get Current activity//
		TiApplication appContext = TiApplication.getInstance();
		Activity activity = appContext.getCurrentActivity();
        
		try
		{
          activity.unregisterReceiver(myReceiver);		
		}
		catch(Exception e)
		{
			
		}
		
        //Close reader thread//       
        isConnected = false;
        
        //Close Server//
        stopServer();
        
        //Close connection//
		if (btsocket != null) 
		{
			try 
			{
				if (bluetoothAdapter != null)
					bluetoothAdapter.cancelDiscovery();
				
				btsocket.close();
			} 
			catch (Exception e) 
			{
				Log.w(TAG, "Bluetooth Socket close exception");
			}
		}        
        
        
		
		disableBluetooth();
		bluetoothAdapter = null;
		
		
	}	
	
	
	@Kroll.method
	public KrollDict getPairedDevices()
	{
		Log.d(TAG, "getPairedDevices called");
		
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		
		KrollDict result = new KrollDict();
		
		// If there are paired devices
		if (pairedDevices.size() > 0) 
		{
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) 
		    {
		    	//Halilk: for some devices, device name is the same so put some of mac digits
		    	String strDigitstoAdd = device.getAddress();
		    	strDigitstoAdd = strDigitstoAdd.replace(":","");
		    	result.put(device.getName()+"_"+strDigitstoAdd, device.getAddress());		    	
		    }
		}
		
		return result;
		
	}	
	

	private void devicesFound()
	{    	
    	this.fireEvent("nb_DevicesFound", devicelist);     			
	}
	
	public static void sendEvent(String eventname,KrollDict data)
	{
		staticNovarumbluetoothModule.fireEvent(eventname, data); 
	}
	
	
	public boolean pairDevice(BluetoothDevice btDevice)
	{
		
		Method createBondMethod = null;
		boolean returnValue = false;
		
		try 
		{
			createBondMethod = BluetoothDevice.class.getMethod("createBond");
			
		} 
		catch (NoSuchMethodException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (SecurityException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try 
		{
			returnValue = (Boolean) createBondMethod.invoke(btDevice);
		} 
		catch (IllegalAccessException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IllegalArgumentException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (InvocationTargetException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return returnValue;
	}
	
	
	public boolean socketConnect(BluetoothDevice btDevice)
	{
		try
		{
			btsocket = btDevice.createRfcommSocketToServiceRecord(UUID.fromString(DEFAULT_UUID));
			
			Method m = btDevice.getClass().getMethod("createRfcommSocket",new Class[] { int.class });
			
			btsocket = (BluetoothSocket) m.invoke(btDevice, 1);
			btsocket.connect();	

			inputStream   = btsocket.getInputStream();
			outputStream  = btsocket.getOutputStream();			
			
			isConnected = true;
			
			//open a thread for reading data//
			datareceiver = new dataReceiver();
			datareceiver.start();			
			//open a thread for reading data//
			
			//Fire an event//
			this.fireEvent("nb_onConnect",null);
			return true;
			
		}
		catch(Exception e)
		{
			postError(e.getMessage());
			return false;
		}
		
		
	}
	
	
	@Kroll.method
	public boolean connect(String devicemac)
	{
		if(devicemac == null)
		   return false;
		
		//Check if we should use the service//
		if(useService)
		{
			
			//Start Service//
			try
			{
				//Get Current activity//
				TiApplication appContext = TiApplication.getInstance();
				Activity activity = appContext.getCurrentActivity();
				
				BluetoothServiceIntent = new TiIntentWrapper(new Intent(activity,BluetoothService.class));
				BluetoothServiceIntent.getIntent().putExtra("MacAddress",devicemac);
				appContext.startService(BluetoothServiceIntent.getIntent());
				
				return true;
			}
			catch(Exception e)
			{
				Log.w(TAG,"error on creating bluetooth service: "+e.getMessage());
				return false;					
			}
			
			
		}
		else
		{		
			bluetoothDevice = bluetoothAdapter.getRemoteDevice(devicemac);
			
			if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) 
			{
				if(pairDevice(bluetoothDevice))
				{
					
					return socketConnect(bluetoothDevice);
				}
				else
				{
					postError("Could not pair device");
					return false;
				}
			}
			else
			{
				return socketConnect(bluetoothDevice);				
			}
		}
	}

	
	private void PostReceivedData(String data)
	{
		if(data == null)
			return;
		
		
		KrollDict receivedict = new KrollDict();
		
		receivedict.put("data", data);
		this.fireEvent("nb_onReceiveData", receivedict);
		
		
		
	}
	
	
	
	@Kroll.method
	public boolean sendData(String data) 
	{
		if(useService)
		{
			  return BluetoothService.sendData(data);
		}
		else
		{
			if (btsocket != null && isConnected == true) 
			{
				try 
				{
					outputStream.write(data.getBytes());
					outputStream.flush();
					
					return true;
					
				} 
				catch (Exception e) 
				{
					postError(e.getMessage());
					return false;
				}
			}
			else
			{
				postError("Not connected or data is null");
				return false;
			}
		}
	}	
	
	
	@Kroll.method
	public boolean isConnected() 
	{	
		if(useService)
		  return BluetoothService.isConnected();
		else		  
		  return isConnected;
	}
	
	@Kroll.method
	public void Disconnect() 
	{	
		if(useService)
		{
			//Destroy Service//
			try
			{
				TiApplication appContext = TiApplication.getInstance();
			    appContext.stopService(BluetoothServiceIntent.getIntent());
			}
			catch(Exception e)
			{
				Log.w(TAG,"error on stopping the service: "+e.getMessage());
			}
		}
		else
		{
			if(isConnected)
			{
			    try
			    {
			    	isConnected = false;
			    	
			    	if(inputStream != null)
					   inputStream.close();
						
					if(outputStream != null)
					   outputStream.close();
						
					if(btsocket != null)
					   btsocket.close();
							
			    }
			    catch(Exception e)
			    {
			       e.printStackTrace();
			    }
			}
		}
	}	
	
	
	private void postError(String Error)
	{
		KrollDict data = new KrollDict();
		data.put("error", Error);
		this.fireEvent("nb_onError", data);
	}
	
	
    private BroadcastReceiver myReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            Message msg = Message.obtain();
            String action = intent.getAction();
            
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {

            	bluetoothDevice        = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            	BluetoothClass blclass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            	
            	int Majorclass = blclass.getMajorDeviceClass();
            	int minorclass = blclass.getDeviceClass();
            	
            	
                devicelist = new KrollDict();
                
                try
                {
                	devicelist.put("name",bluetoothDevice.getName());
                	devicelist.put("macaddress",bluetoothDevice.getAddress());
                	devicelist.put("pairedstatus",bluetoothDevice.getBondState());
                }
                catch (Exception e) 
                {
                    Log.w(TAG, "devicesFound exception: "+e.getMessage());
                    postError(e.getMessage());
                }

                devicesFound();
                
            }           
        }
    };	
	
	
    
    
	//Halilk: Bluetooth data reciever thread//
	class dataReceiver extends Thread 
	{
		public void run() 
		{

			while (isConnected) 
			{
				if (inputStream != null) 
				{
					try 
					{

						byte[] data = new byte[1024]; //read data 1kb

						int length = inputStream.read(data);
						
						
						if(length > 0)
						{
							String ReceivedData = null;
							try
							{
							   ReceivedData = new String(data,0,length,"UTF-8");
							}
							catch(Exception e)
							{
							   Log.w(TAG,"Error on creating data script: "+e.getMessage());
							}
							
							if(ReceivedData != null)
							{
							   PostReceivedData(ReceivedData); // send received data to the app
							}
						}

					} 
					catch (IOException e) 
					{
						postError(e.getMessage());
					}
				}
			}
		}
	}
	
	
	
	private void manageConnectedSocket()
	{
		try 
		{
			inputStream   = btsocket.getInputStream();
			outputStream  = btsocket.getOutputStream();
		} 
		catch (IOException e) 
		{
			postError(e.getMessage());
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		isConnected = true;
				
		//open a thread for reading data//
		try
		{
			datareceiver = new dataReceiver();
			datareceiver.start();			
		}
		catch(Exception e)
		{
			postError(e.getMessage());
		}
		//open a thread for reading data//
		
		this.fireEvent("nb_onConnect",null);
		
	}
	
	
	
	@Kroll.method
	public void startServer() 
	{	
		try
		{
			acceptthread = new AcceptThread();
			acceptthread.start();
			this.fireEvent("nb_onServerStarted",null);
		}
		catch(Exception e)
		{
			postError(e.getMessage());
		}
	}
	
	
	@Kroll.method
	public void stopServer() 
	{
		acceptthread.cancel();
	}	
	
	
	@Kroll.method
	public void setServerName(String name) 
	{	
		this.SERVERNAME = name;
	}	
	

	@Kroll.method
	public void setUUID(String uuid) 
	{	
		this.DEFAULT_UUID = uuid;
	}	
	
	
	@Kroll.method
	public void makeDiscoverable() 
	{	
		if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
		{		
			TiApplication appContext = TiApplication.getInstance();
			Activity activity = appContext.getCurrentActivity();
			
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		    activity.startActivity(discoverableIntent);
		}
	}
	
	
	public void discoverabilityResult(int Result)
	{
		KrollDict data = new KrollDict();
		
		data.put("result",Integer.toString(Result));
		
		Log.w(TAG,"NovarumBluetooth Discoverability Result" + Result);
		
		fireEvent("nb_onDiscoverabilityResult", data);
	}
	
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{

		switch (resultCode) 
		{
		   case Activity.RESULT_OK:
		      discoverabilityResult(1);
		   break;
		   
		   case Activity.RESULT_CANCELED:
			  discoverabilityResult(0);
		   break;		   
		   
		   default:
			  discoverabilityResult(resultCode);
		   break;
		   
		}
	}
	
	
	
	
	
	private class AcceptThread extends Thread 
	{
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() 
	    {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try 
	        {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVERNAME, UUID.fromString(DEFAULT_UUID));
	        } 
	        catch (IOException e) 
	        { 
	        	postError(e.getMessage());
	        }
	        
	        mmServerSocket = tmp;
	    }
	 
	    public void run() 
	    {
	        btsocket = null;
	        
	        // Keep listening until exception occurs or a socket is returned
	        while (true) 
	        {
	            try 
	            {
	            	btsocket = mmServerSocket.accept();
	            } 
	            catch (IOException e) 
	            {
	            	postError(e.getMessage());
	            }
	            
	            // If a connection was accepted
	            if (btsocket != null) 
	            {
	                // Do work to manage the connection (in a separate thread)
	                manageConnectedSocket();
	                
	                try 
	                {
						mmServerSocket.close();
					} 
	                catch (IOException e) 
	                {
	                	postError(e.getMessage());
	                	// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                
	                break;
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() 
	    {
	        try 
	        {
	            mmServerSocket.close();
	        } 
	        catch (IOException e) 
	        { 
	        	postError(e.getMessage());
	        }
	    }
	}	
	

}

