package com.example.aging;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.widget.*;
import android.view.View.OnClickListener;
import java.util.List;
import android.content.Intent;

public class MainActivity extends Activity {
	private Button mButton;
	private TextView mTextView,mCleanInfoTextView;
	
	private boolean isRunning = true;
	private Thread mThread;
	private double LimitMemThreshold = 90;
	private Handler handler;	
	int oldHandleLevel;
	private List mRunningAppProcessInfoList;
	private ActivityManager mActivityManager;
	private StringBuffer mCleanInfoStringBuffer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

        mButton = (Button) findViewById(R.id.button1);
        mTextView = (TextView) findViewById(R.id.textView1);     
        mCleanInfoTextView = (TextView) findViewById(R.id.textView2);
        mActivityManager=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mCleanInfoStringBuffer = new StringBuffer();
                
        mButton.setOnClickListener(new OnClickListener() {

        	@Override
        	public void onClick(View arg0) {
        		// TODO Auto-generated method stub
                try { 
                    	Log.i("sure debug", "reboot directly");
			PowerManager pManager=(PowerManager) getSystemService(Context.POWER_SERVICE);  
			pManager.reboot("");  
                }catch (Exception ex){
                    ex.printStackTrace();
                }
        		//isRunning = false;
            }
        });
   
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            	String totalMemory = msg.getData().getString("TotalMemory");
            	String freeMemory = msg.getData().getString("FreeMemory");
            	
                switch (msg.what) {            	
                case 0:                	
                //	Log.i("sure debug", "action 0");
                	mTextView.setText("Total memory: " + totalMemory + ", " + "avaliable memory: " 
            				+ freeMemory + ", " + "memory usage: " + msg.obj + "%");
                    //mTextView.setText("lost:" + msg.obj);
                	break;
                case 1:	// clean cache 
                //	Log.i("sure debug", "action 1");
                	mTextView.setText("Total memory: " + totalMemory + ", " + "avaliable memory: " 
            				+ freeMemory + ", " + "memory usage: " + msg.obj + "%");
                	mCleanInfoTextView.setText("");
                	cleanApplication();
                	break;
                case 2:
			PowerManager pManager=(PowerManager) getSystemService(Context.POWER_SERVICE);  
			pManager.reboot("");  
                //	Log.i("sure debug", "action 2");

                	break;                         	
                	
                }
            }

			private void cleanApplication() {
				// TODO Auto-generated method stub
	               RunningAppProcessInfo runningAppProcessInfo=null;
	                mRunningAppProcessInfoList= mActivityManager.getRunningAppProcesses();
	                //List serviceInfos = mActivityManager.getRunningServices(100);
	 
	          //      Log.i("sure debug", mRunningAppProcessInfoList.size() + "\t");
	                
	                if (mRunningAppProcessInfoList != null) {
	                    for (int i = 0; i < mRunningAppProcessInfoList.size(); ++i) {
	                        runningAppProcessInfo= (RunningAppProcessInfo) mRunningAppProcessInfoList.get(i);
	                        // Importance > RunningAppProcessInfo.IMPORTANCE_SERVICE (300)
	                        // no using for long time or NULL process
	                        // Importance > RunningAppProcessInfo.IMPORTANCE_VISIBLE (200)
	                        // background process
	                        if (runningAppProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
	                            String[] pkgList = runningAppProcessInfo.pkgList;
	                            for (int j = 0; j < pkgList.length; ++j) {
	                                mActivityManager.killBackgroundProcesses(pkgList[j]);
	                                mCleanInfoStringBuffer.append(pkgList[j] + " is killed...\n");
	                                mCleanInfoTextView.setText(mCleanInfoStringBuffer.toString());
	                            }
	                        }
	 
	                    }
	                }

			}
        };
        
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
					long TotalMemory = this.getTotalMemory();
					long AvailMemory = this.getAvailMemory();
					double MemUsage = (TotalMemory - AvailMemory) * 100 / TotalMemory ;

				    Message message = new Message();
				    Bundle bundle = new Bundle();
				    
				    if (MemUsage < LimitMemThreshold )
				    {
				    	bundle.putString("TotalMemory",Formatter.formatFileSize(getBaseContext(), TotalMemory));
				    	bundle.putString("FreeMemory",Formatter.formatFileSize(getBaseContext(), AvailMemory));
				    	message.setData(bundle);
				    	message.obj = MemUsage ;
				    
				    	message.what = 0;	// Memory usage normal
				    	oldHandleLevel = 0;
				    }
				    else
				    {// Memory usage over weight
				    	bundle.putString("TotalMemory",Formatter.formatFileSize(getBaseContext(), TotalMemory));
				    	bundle.putString("FreeMemory",Formatter.formatFileSize(getBaseContext(), AvailMemory));
				    	message.setData(bundle);
				    	message.obj = MemUsage ;
				    	
				    	switch(oldHandleLevel){
				    		case 0:			
				    			message.what = 1;	// kill unused/background application
				    			break;
				    		case 1:			
				    			message.what = 2;	// reboot device
				    			break;
				    	}
				    	oldHandleLevel = message.what;
				    }
				    handler.sendMessage(message);
				}
            }
            
			private long getAvailMemory() {
				// TODO Auto-generated method stub
				ActivityManager activManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
				MemoryInfo memoryInfo = new MemoryInfo();
				activManager.getMemoryInfo(memoryInfo);

				return memoryInfo.availMem;
				//return Formatter.formatFileSize(getBaseContext(), memoryInfo.availMem);
			}

			private long getTotalMemory() {
				// TODO Auto-generated method stub
				
				String memfile = "/proc/meminfo";
				String Memtotal;
				String[] arrayOfString;
				long initial_memory = 0;

				try {
					FileReader localFileReader = new FileReader(memfile);
					BufferedReader localBufferedReader = new BufferedReader(
							localFileReader, 8192);
					Memtotal = localBufferedReader.readLine();

				arrayOfString = Memtotal.split("\\s+");
				//for (String num : arrayOfString) {
					//Log.i(Memtotal, num + "\t");
				//}

				initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;
				localBufferedReader.close();
				} 
				
				catch (IOException e) {
				}

				return initial_memory;
				//return Formatter.formatFileSize(getBaseContext(), initial_memory);
			}
        });
        mThread.start();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		System.out.println(id);
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
