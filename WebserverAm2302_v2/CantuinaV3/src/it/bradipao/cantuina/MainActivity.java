/*
CANTUINA
Copyright (c) 2013 Bradipao <bradipao@gmail.com>
http://www.google.com/+SidBradipao

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package it.bradipao.cantuina;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LinePoint;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends Activity {

   // constants
   private static final String LOGTAG = "CANTUINA";
   private static final boolean LOGENABLE = true;
   private static void LOGD(String msg) { if (LOGENABLE) Log.d(LOGTAG,msg); }
   private static void LOGE(String msg) { if (LOGENABLE) Log.e(LOGTAG,msg); }
   public static final int MSG_CONT = 100;
   
   // views
   Button btnSingle,btnContinuous,btnConnect,btnHistory;
   TextView tvTemp,tvHumi,tvResult,tvTempMax,tvHumiMax,tvTempMin,tvHumiMin;
   LineGraph lineGraph;
   Line lineTemp,lineHumi;
   
   // vars
   boolean bOnline = false;
   boolean bAutoscan = false;
   boolean bNetworking = false;
   boolean bConnected = false;
   boolean bFromPref = false;
   boolean bContinuous = false;
   String exactUrl = null;          // example http://10.0.0.14/cantuina
   String subnetUrl = null;         // example http://10.0.0.*/cantuina
   String pingUrl = null;           // currently scanned address
   int iScanTimeout,iContInterval;
   int pingFrom,pingTo;
   int px;
   
   // objects
   SharedPreferences prefs = null;
   AsyncHttpClient netClient = null;
   Message msg = null;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); 
      setContentView(R.layout.ac_main);
      
      // setup
      getViews();
      bOnline = isNetworkPresent();
      netClient = new AsyncHttpClient();
      prefs = PreferenceManager.getDefaultSharedPreferences(this); 
      setupVars();
      
      // views starting status
      tvTemp.setText(R.string.tv_notemp);
      tvTempMax.setText(R.string.tv_notemp);
      tvTempMin.setText(R.string.tv_notemp);
      tvHumi.setText(R.string.tv_nohumi);
      tvHumiMax.setText(R.string.tv_nohumi);
      tvHumiMin.setText(R.string.tv_nohumi);
      btnSingle.setEnabled(false);
      btnHistory.setEnabled(false);
      btnContinuous.setEnabled(false);
      if (bAutoscan) {
         btnConnect.setText(R.string.btn_scan);
         tvResult.setText("press to scan\n"+subnetUrl);
      }
      else {
         btnConnect.setText(R.string.btn_connect);
         tvResult.setText("press to connect to\n"+exactUrl);
      }
      
      // graph setup
      lineTemp = new Line();
      lineTemp.setColor(Color.parseColor("#99ccff"));
      lineTemp.setStrokeWidth(4);
      lineTemp.setShowingPoints(false);
      lineHumi = new Line();
      lineHumi.setColor(Color.parseColor("#ffcc99"));
      lineHumi.setStrokeWidth(4);
      lineHumi.setShowingPoints(false);
      lineGraph.addLine(lineTemp);
      lineGraph.addLine(lineHumi);
      lineGraph.setRangeY(0,50);
      lineGraph.addPoint(0,new LinePoint(0,0));
      lineGraph.addPoint(1,new LinePoint(0,50));
      px = 1;
      
      // scan or connect button listener
      btnConnect.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            // start network scan
            if ((!bNetworking)&(bAutoscan)) {
               bNetworking = true;
               pingFrom = 1;
               pingTo = 50;

               setProgressBarIndeterminateVisibility(bNetworking);
               btnConnect.setText(R.string.btn_stop);
               scanNet();
            }
            // or directly connect to exact URL from settings
            else if (!bNetworking) {
               setProgressBarIndeterminateVisibility(bNetworking);
               btnConnect.setEnabled(false);
               connectUrl();
            } else {
               bNetworking = false;
            }
         }
      });
      
      // single readout
      btnSingle.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            if (bConnected) {
               fetchValues();
            }
         }
      });
      
      // history readout
      btnHistory.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            if (bConnected) {
               fetchHistory();
            }
         }
      });
      
      // continuous readout
      btnContinuous.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            
            // if connected and stopped, start continuous
            if (bConnected & !bContinuous) {
               bContinuous = true;
               btnContinuous.setText(R.string.btn_stop);
               btnSingle.setEnabled(false);
               btnHistory.setEnabled(false);
               // start continuous call of fetchContValues at iContInterval
               msg = new Message();
               msg.what = MSG_CONT;
               msg.arg1 = iContInterval;
               timingHandler.sendMessageDelayed(msg,msg.arg1);
            }
            // else stop continuous
            else if (bContinuous) {
               bContinuous = false;
            }

         }
      });
      
   }
   
   // handler for timed continuous operations
   Handler timingHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
            case MSG_CONT:
               // if still in continuous mode run again
               if (bContinuous) {
                  fetchContValues();
                  msg = timingHandler.obtainMessage(MSG_CONT,iContInterval,0);
                  timingHandler.sendMessageDelayed(msg,msg.arg1);
               }
               // else restore buttons
               else {
                  btnContinuous.setText(R.string.btn_continuous);
                  btnSingle.setEnabled(true);
                  btnHistory.setEnabled(true);
               }
               break;
         }
         super.handleMessage(msg);
      }
   };

   // FETCHCONTVALUES : continuously fetch values from foundUrl
   private void fetchContValues() {
      // prepare net client
      if (netClient==null) return;
      netClient.setTimeout(iContInterval-200);
      // readout
      netClient.get(exactUrl,new AsyncHttpResponseHandler() {
         // callback called on success
         @Override
         public void onSuccess(String response) {
            // parse json from server, example : {"res":"OK","temp":"25.0","humi":"45.4"}
            try {
               JSONObject jso = new JSONObject(response);
               float fTemp = (float) jso.getDouble("temp");
               float fHumi = (float) jso.getDouble("humi");
               tvTemp.setText(String.format("%.1f",fTemp));
               tvHumi.setText(String.format("%.1f",fHumi));
               lineGraph.shiftPoint(0,new LinePoint(px,fTemp),20);
               lineGraph.shiftPoint(1,new LinePoint(px,fHumi/2),20);
               tvResult.setText("ACQUISITION n. "+px);
               px++;
            } catch (JSONException e) {
               tvResult.setText("DATA ERROR");
               LOGE("Error in parsing JSON data");
            } 
         }
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            tvResult.setText("ERROR fetching values");
         }
      });
   }
   
   // FETCHVALUES : fetch values from foundUrl
   private void fetchValues() {
      // prepare net client
      if (netClient==null) return;
      netClient.setTimeout(2000);
      // readout
      btnSingle.setEnabled(false);
      netClient.get(exactUrl,new AsyncHttpResponseHandler() {
         // callback called on success
         @Override
         public void onSuccess(String response) {
            // parse json from server
            // example : {"res":"OK","temp":"25.0","humi":"45.4"}
            try {
               JSONObject jso = new JSONObject(response);
               float fTemp = (float) jso.getDouble("temp");
               float fHumi = (float) jso.getDouble("humi");
               tvTemp.setText(String.format("%.1f",fTemp));
               tvHumi.setText(String.format("%.1f",fHumi));
               lineGraph.shiftPoint(0,new LinePoint(px,fTemp),20);
               lineGraph.shiftPoint(1,new LinePoint(px,fHumi/2),20);
               tvResult.setText("ACQUISITION n. "+px);
               px++;
            } catch (JSONException e) {
               tvResult.setText("DATA ERROR");
               LOGE("Error in parsing JSON data");
            } finally {
               btnSingle.setEnabled(true);
            }
         }
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            btnSingle.setEnabled(true);
            tvResult.setText("ERROR fetching values");
         }
      });
   }
   
   // FETCHHISTORY : fetch history from foundUrl
   private void fetchHistory() {
      // prepare net client
      if (netClient==null) return;
      netClient.setTimeout(2000);
      // readout
      btnHistory.setEnabled(false);
      netClient.get(exactUrl+"?hist=1",new AsyncHttpResponseHandler() {
         // callback called on success
         @Override
         public void onSuccess(String response) {
            // parse json from server
            try {
               JSONObject jso = new JSONObject(response);
               JSONArray jsa = new JSONArray();
               JSONObject hitem = null;
               float fTemp,fHumi;
               float fHumiMin=100,fHumiMax=-100;
               float fTempMin=100,fTempMax=-100;
               jsa = jso.getJSONArray("history");
               LOGD("FETCH HISTORY entries:"+jsa.length());
               for (int i=0;i<jsa.length();i++) {
                  // fetch values
                  hitem = jsa.getJSONObject(i);
                  fTemp = (float) hitem.getDouble("temp");
                  fHumi = (float) hitem.getDouble("humi");
                  // max and min
                  if (fTemp>fTempMax) fTempMax = fTemp;
                  if (fTemp<fTempMin) fTempMin = fTemp;
                  if (fHumi>fHumiMax) fHumiMax = fHumi;
                  if (fHumi<fHumiMin) fHumiMin = fHumi;
                  // graph
                  lineGraph.shiftPoint(0,new LinePoint(px,fTemp),jsa.length());
                  lineGraph.shiftPoint(1,new LinePoint(px,fHumi/2),jsa.length());
                  px++;
                  // logging
                  LOGD("HIST#"+i+" temp:"+fTemp+" humi:"+fHumi);
               }
               tvTempMax.setText(String.format("%.1f",fTempMax));
               tvTempMin.setText(String.format("%.1f",fTempMin));
               tvHumiMax.setText(String.format("%.1f",fHumiMax));
               tvHumiMin.setText(String.format("%.1f",fHumiMin));
               tvResult.setText("ACQUISITION n. "+px);
            } catch (JSONException e) {
               tvResult.setText("DATA ERROR");
               LOGE("Error in parsing JSON data");
            } finally {
               btnHistory.setEnabled(true);
            }
         }
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            btnHistory.setEnabled(true);
            tvResult.setText("ERROR fetching values");
         }
      });
   }
   
   // CONNECTURL : check url waiting for answer
   private void connectUrl() {
      // prepare net client
      if (netClient==null) return;
      netClient.setTimeout(10000);
      // readout
      netClient.get(exactUrl,new AsyncHttpResponseHandler() {
         // callback called on success : connected state
         @Override
         public void onSuccess(String response) {
            // result
            bConnected = true;
            tvResult.setText("CONNECTED Arduino at\n"+exactUrl);
            btnSingle.setEnabled(true);
            btnHistory.setEnabled(true);
            btnContinuous.setEnabled(true);
            // indeterminate progress bar
            setProgressBarIndeterminateVisibility(bNetworking);
         }
         // callback called on failure : return
         @Override
         public void onFailure(Throwable e,String response) {
            // result
            tvResult.setText("NOT FOUND at address\n "+exactUrl);
            // indeterminate progress bar
            setProgressBarIndeterminateVisibility(bNetworking);
            btnConnect.setEnabled(true);
         }
      });
   }
   
   // SCANNET : scan addresses in local subnet
   private void scanNet() {
      // calculating url to ping
      pingUrl = subnetUrl.replace("*",Integer.toString(pingFrom));
      // ping url
      btnConnect.setText(R.string.btn_stop);
      if (netClient==null) return;
      netClient.setTimeout(iScanTimeout);
      netClient.get(pingUrl,new AsyncHttpResponseHandler() {
         
         // callback called on success : url found
         @Override
         public void onSuccess(String response) {
            // result
            exactUrl = pingUrl;
            tvResult.setText("FOUND Arduino at\n"+exactUrl);
            // set preferences and status
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("prefkey_autoscan",false);
            editor.putString("prefkey_network_address",exactUrl);
            editor.commit();
            bAutoscan = false;
            bNetworking = false;
            // button and indeterminate progress bar
            btnConnect.setText(R.string.btn_connect);
            setProgressBarIndeterminateVisibility(bNetworking);
         }
         
         // callback called on failure : continue scanning
         @Override
         public void onFailure(Throwable e,String response) {
            tvResult.setText("SCANNING subnet address\n "+pingUrl);
            if (pingFrom<pingTo) {
               pingFrom++;
               if (bNetworking) scanNet();
               else {
                  // result
                  tvResult.setText("press to scan\n"+subnetUrl);
                  // button and indeterminate progress bar
                  if (bAutoscan) btnConnect.setText(R.string.btn_scan);
                  else btnConnect.setText(R.string.btn_connect);
                  setProgressBarIndeterminateVisibility(bNetworking);
               }
            }
         }

      });
   }
   
   // called when the activity resumes
   @Override
   public void onResume() {
      super.onResume();
      // update vars when returning from preferences
      if (bFromPref) {
         bFromPref = false;
         setupVars();
         if (bAutoscan) btnConnect.setText(R.string.btn_scan);
         else btnConnect.setText(R.string.btn_connect);
      }
   }
   
   // option menu is base on /res/menu/main.xml and placed in action bar
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.main,menu);
      return true;
   }

   // called when a menu item is selected
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      
      // menu SETTINGS clicked : show Preference activity
      case R.id.action_settings:
         bFromPref = true;
         startActivity(new Intent(this,CantuinaPrefActivity.class));
         return true;
      }
      return super.onOptionsItemSelected(item);
   }
   
   // setup activity local variables from preferences
   private void setupVars() {
      if (prefs==null) return;
      exactUrl = prefs.getString("prefkey_network_address","");
      iScanTimeout = Integer.parseInt(prefs.getString("prefkey_scan_timeout","2000"));
      iContInterval = Integer.parseInt(prefs.getString("prefkey_cont_interval","2000"));
      bAutoscan = prefs.getBoolean("prefkey_autoscan",true);
      if (bOnline) subnetUrl = getSubnet() + "/cantuina";
   }
   
   // initialize views references
   private void getViews() {
      lineGraph = (LineGraph)findViewById(R.id.tempGraph);
      btnSingle = (Button)findViewById(R.id.btnSingle);
      btnHistory = (Button)findViewById(R.id.btnHistory);
      btnContinuous = (Button)findViewById(R.id.btnContinuous);
      btnConnect = (Button)findViewById(R.id.btnConnect);
      tvResult = (TextView)findViewById(R.id.tvResult);
      tvTemp = (TextView)findViewById(R.id.tvTemp);
      tvHumi = (TextView)findViewById(R.id.tvHumi);
      tvTempMax = (TextView)findViewById(R.id.tvTempMax);
      tvHumiMax = (TextView)findViewById(R.id.tvHumiMax);
      tvTempMin = (TextView)findViewById(R.id.tvTempMin);
      tvHumiMin = (TextView)findViewById(R.id.tvHumiMin);
   }
   
   // ISNETWORKPRESENT : check for network connection
   private boolean isNetworkPresent() {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      //NetworkInfo activeWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      NetworkInfo activeWifi = cm.getActiveNetworkInfo();
      return ((activeWifi!=null)&&(activeWifi.isConnected()));
   }
   
   // GETSUBNET : identify subnet address
   public String getSubnet() {
      String subnet = null;
      try {
         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = (NetworkInterface) en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
               InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
               if (!inetAddress.isLoopbackAddress()) {
                  subnet = inetAddress.getHostAddress().toString();
                  if (InetAddressUtils.isIPv4Address(subnet)) {
                     int idx1 = subnet.lastIndexOf(".");
                     subnet = "http://"+subnet.substring(0,idx1)+".*";
                     return subnet;
                  }
               }
            }
         }
      } catch (SocketException e) {
         LOGE(e.toString());
      }
      return null;
   }
}
