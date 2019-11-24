package com.example.blockcall;

import java.lang.reflect.Method;

import com.android.internal.telephony.ITelephony;

import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.widget.Toast;

public class Blocker extends BroadcastReceiver {
	private static final int MODE_WORLD_READABLE = 1;
	private ITelephony telephonyService;
	private String incommingNumber;
	private String incommingName=null;
	private SharedPreferences myPrefs; 
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		 myPrefs = context.getSharedPreferences("myPrefs", MODE_WORLD_READABLE);
		 String blockingMode=myPrefs.getString("mode", "not retrieved");
		 
       if(!blockingMode.equals("cancel")) 
       {
		Bundle bb = intent.getExtras();  
	  String state = bb.getString(TelephonyManager.EXTRA_STATE);
	  if ((state != null)&& (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)))     
	  {
		  incommingNumber = bb.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
		  if(blockingMode.equals("all"))
		  {
			  blockCall(context, bb);
		  }
		  else if(blockingMode.equals("unsaved"))
		  {
			  
		        incommingName=getContactDisplayNameByNumber(incommingNumber, context);
			   if((incommingName==null)||(incommingName.length()<=1)){
				   blockCall(context, bb);
			   }
		 }
		   
		  else if(blockingMode.equals("list"))
		  {
			  RemindersDbAdapter mDbAdapter=new RemindersDbAdapter(context);
			  mDbAdapter.open();
			  Cursor c= mDbAdapter.fetchAllReminders();  
				  if(c.moveToFirst())
				  {
			   
			        while (c.isAfterLast() == false) {  
			        String	title= c.getString(c.getColumnIndex(RemindersDbAdapter.KEY_TITLE)); 
			        	  if(title.equals(incommingNumber))      
			        	 {
			        		 
								blockCall(context, bb);
			        	 }
			        	  c.moveToNext(