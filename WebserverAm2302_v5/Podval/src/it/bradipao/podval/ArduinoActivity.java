package it.bradipao.podval;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

public class ArduinoActivity extends Activity {

   // constants
   private static final String LOGTAG = "PODVAL";
   private static final boolean LOGENABLE = true;
   private static void LOGD(String msg) { if (LOGENABLE) Log.d(LOGTAG,msg); }
   private static void LOGE(String msg) { if (LOGENABLE) Log.e(LOGTAG,msg); }
   
   // vars
   PodvalApp app;
   ArduinoItem aitem;
   ActionBar actionBar;
   int position;
   long now,lastfetch;
   
   // views
   RelativeLayout rlArduitem;
   LinearLayout lySensor1,lySensor2,lySensor3,lySensor4,lySensor5;
   TextView tvName,tvHwid,tvIp;
   TextView tvTemp1,tvTempMin1,tvTempMax1,tvHumi1,tvHumiMin1,tvHumiMax1;
   TextView tvTemp2,tvTempMin2,tvTempMax2,tvHumi2,tvHumiMin2,tvHumiMax2;
   TextView tvTemp3,tvTempMin3,tvTempMax3,tvHumi3,tvHumiMin3,tvHumiMax3;
   TextView tvTemp4,tvTempMin4,tvTempMax4,tvHumi4,tvHumiMin4,tvHumiMax4;
   TextView tvTemp5,tvTempMin5,tvTempMax5,tvPres5,tvPresMin5,tvPresMax5;
   
   // called when the activity is first created
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ac_arduino);
      
      // setup
      getOverflowMenu();
      getViews();
      app = PodvalApp.getInstance();

      // action bar UP button
      actionBar = getActionBar();
      actionBar.setDisplayHomeAsUpEnabled(true);
      
      // get arduino data
      Bundle extras = getIntent().getExtras();
      position = (int) extras.getInt("position",-1);
      if (position==-1) finish();
      aitem = app.getArduinoItemDb().get(position);

      // click listener
      rlArduitem.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            fetchArduino();
         }
      });
      
      lySensor1.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            launchSensorActivity(position,1);
         }
      });
      lySensor2.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            launchSensorActivity(position,2);
         }
      });
      lySensor3.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            launchSensorActivity(position,3);
         }
      });
      lySensor4.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            launchSensorActivity(position,4);
         }
      });
      lySensor5.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            launchSensorActivity(position,5);
         }
      });
      
   }
   
   // called when the activity RESUMES
   @Override
   public void onResume() {
      super.onResume();
      // update views
      tvName.setText(aitem.sName);
      tvHwid.setText(aitem.sHwid);
      tvIp.setText(aitem.sIp);
      // start fetching
      now = Calendar.getInstance().getTimeInMillis();
      if (now-aitem.lastfetch > 10*60*1000) fetchArduino();
      else updateArduinoUI();
   }
   
   // actionbar MENU management
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.ac_arduino,menu);
      return true;
   }
   
   // called when a menu item is selected
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      // respond to the action bar's UP/HOME button
      case android.R.id.home:
         finish();
         return true;
      // launch activity for EDIT NAME
      case R.id.action_editname:
         launchEditnameActivity(position);
         return true;
      // query arduino for data and history
      case R.id.action_readall:
         fetchArduino();
         return true;
      }
      return super.onOptionsItemSelected(item);
   }
   
   // FETCHARDUINO reads data and history from arduino device
   private void fetchArduino() {
      app.netclient.get(aitem.sIp+"/podval",new AsyncHttpResponseHandler() {
         
         // callback called on success : data received
         @Override
         public void onSuccess(String response) {
            // parse data
            parseArduino(response);
            // store last time in sharedpreferences
            aitem.lastfetch = now; 
            // stop progress bar
            setProgressBarIndeterminateVisibility(false);
         }
         
         // callback called on failure : toast for error
         @Override
         public void onFailure(Throwable e,String response) {
            // result
            Toast.makeText(getApplicationContext(),"ERROR fetching data from network",Toast.LENGTH_LONG).show();
            // stop progress bar
            setProgressBarIndeterminateVisibility(false);
         }

      });
      
   }
   
   // PARSEJSONDATA parses json data from arduino device
   private void parseArduino(String response) {
      long tt;
      float ft1,ft2,ft3,ft4,ft5,fh1,fh2,fh3,fh4,fp5;
      try {
         // parse realtime data
         JSONObject jso = new JSONObject(response);
         tt = Long.parseLong(jso.getString("time"));
         ft1 = Float.parseFloat(jso.getString("t1"))/10;
         ft2 = Float.parseFloat(jso.getString("t2"))/10;
         ft3 = Float.parseFloat(jso.getString("t3"))/10;
         ft4 = Float.parseFloat(jso.getString("t4"))/10;
         ft5 = Float.parseFloat(jso.getString("t5"))/10;
         fh1 = Float.parseFloat(jso.getString("h1"))/10;
         fh2 = Float.parseFloat(jso.getString("h2"))/10;
         fh3 = Float.parseFloat(jso.getString("h3"))/10;
         fh4 = Float.parseFloat(jso.getString("h4"))/10;
         fp5 = Float.parseFloat(jso.getString("p5"))/100;
         if (ft1>100) aitem.s1 = false;
         if (ft2>100) aitem.s2 = false;
         if (ft3>100) aitem.s3 = false;
         if (ft4>100) aitem.s4 = false;
         if (ft5>100) aitem.s5 = false;
         aitem.data = new ArduinoData(tt,ft1,ft2,ft3,ft4,ft5,fh1,fh2,fh3,fh4,fp5);
         aitem.lastdate = new Date();
         // parse history array
         JSONArray jsa = jso.getJSONArray("hist");
         JSONObject hitem = null;
         for (int i=0;i<jsa.length();i++) {
            // fetch values
            hitem = jsa.getJSONObject(i);
            tt = Long.parseLong(hitem.getString("time"));
            ft1 = Float.parseFloat(hitem.getString("t1"))/10;
            fh1 = Float.parseFloat(hitem.getString("h1"))/10;
            ft2 = Float.parseFloat(hitem.getString("t2"))/10;
            fh2 = Float.parseFloat(hitem.getString("h2"))/10;
            ft3 = Float.parseFloat(hitem.getString("t3"))/10;
            fh3 = Float.parseFloat(hitem.getString("h3"))/10;
            ft4 = Float.parseFloat(hitem.getString("t4"))/10;
            fh4 = Float.parseFloat(hitem.getString("h4"))/10;
            ft5 = Float.parseFloat(hitem.getString("t5"))/10;
            fp5 = Float.parseFloat(hitem.getString("p5"))/100;
            aitem.hist[i] = new ArduinoData(tt,ft1,ft2,ft3,ft4,ft5,fh1,fh2,fh3,fh4,fp5);
         }
         // update realtime UI
         updateArduinoUI();

      } catch (JSONException e) {
         setProgressBarIndeterminateVisibility(false);
         Toast.makeText(getApplicationContext(),"ERROR in Arduino data",Toast.LENGTH_LONG).show();
         LOGE("Error in parsing JSON data");
      }
   }

   // update UI
   private void updateArduinoUI() {
      // update realtime UI
      if (aitem.s1) {
         tvTemp1.setText(String.format("%.1f °C",aitem.data.ft1));
         tvHumi1.setText(String.format("%.1f %%",aitem.data.fh1));
      }
      if (aitem.s2) {
         tvTemp2.setText(String.format("%.1f °C",aitem.data.ft2));
         tvHumi2.setText(String.format("%.1f %%",aitem.data.fh2));
      }
      if (aitem.s3) {
         tvTemp3.setText(String.format("%.1f °C",aitem.data.ft3));
         tvHumi3.setText(String.format("%.1f %%",aitem.data.fh3));
      }
      if (aitem.s4) {
         tvTemp4.setText(String.format("%.1f °C",aitem.data.ft4));
         tvHumi4.setText(String.format("%.1f %%",aitem.data.fh4));
      }
      if (aitem.s5) {
         tvTemp5.setText(String.format("%.1f °C",aitem.data.ft5));
         tvPres5.setText(String.format("%.1f mbar",aitem.data.fp5));
      }
      // update history UI
      aitem.minmax();
      if (aitem.s1) {
         tvTempMin1.setText(String.format("%.1f",aitem.min.ft1));
         tvTempMax1.setText(String.format("%.1f",aitem.max.ft1));
         tvHumiMin1.setText(String.format("%.1f",aitem.min.fh1));
         tvHumiMax1.setText(String.format("%.1f",aitem.max.fh1));
      }
      if (aitem.s2) {
         tvTempMin2.setText(String.format("%.1f",aitem.min.ft2));
         tvTempMax2.setText(String.format("%.1f",aitem.max.ft2));
         tvHumiMin2.setText(String.format("%.1f",aitem.min.fh2));
         tvHumiMax2.setText(String.format("%.1f",aitem.max.fh2));
      }
      if (aitem.s3) {
         tvTempMin3.setText(String.format("%.1f",aitem.min.ft3));
         tvTempMax3.setText(String.format("%.1f",aitem.max.ft3));
         tvHumiMin3.setText(String.format("%.1f",aitem.min.fh3));
         tvHumiMax3.setText(String.format("%.1f",aitem.max.fh3));
      }
      if (aitem.s4) {
         tvTempMin4.setText(String.format("%.1f",aitem.min.ft4));
         tvTempMax4.setText(String.format("%.1f",aitem.max.ft4));
         tvHumiMin4.setText(String.format("%.1f",aitem.min.fh4));
         tvHumiMax4.setText(String.format("%.1f",aitem.max.fh4));
      }
      if (aitem.s5) {
         tvTempMin5.setText(String.format("%.1f",aitem.min.ft5));
         tvTempMax5.setText(String.format("%.1f",aitem.max.ft5));
         tvPresMin5.setText(String.format("%.1f",aitem.min.fp5));
         tvPresMax5.setText(String.format("%.1f",aitem.max.fp5));
      }
   }
   
   // always show overflow in action bar also when MENU button exists
   private void getOverflowMenu() {
      try {
         ViewConfiguration config = ViewConfiguration.get(this);
         Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
         if(menuKeyField != null) {
             menuKeyField.setAccessible(true);
             menuKeyField.setBoolean(config, false);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   // get views
   private void getViews() {
      rlArduitem = (RelativeLayout) findViewById(R.id.rlArduitem);
      lySensor1 = (LinearLayout) findViewById(R.id.lySensor1);
      lySensor2 = (LinearLayout) findViewById(R.id.lySensor2);
      lySensor3 = (LinearLayout) findViewById(R.id.lySensor3);
      lySensor4 = (LinearLayout) findViewById(R.id.lySensor4);
      lySensor5 = (LinearLayout) findViewById(R.id.lySensor5);
      tvName = (TextView) findViewById(R.id.tvName);
      tvHwid = (TextView) findViewById(R.id.tvHwid);
      tvIp = (TextView) findViewById(R.id.tvIp);
      tvTemp1 = (TextView) findViewById(R.id.tvTemp1);
      tvTempMin1 = (TextView) findViewById(R.id.tvTempMin1);
      tvTempMax1 = (TextView) findViewById(R.id.tvTempMax1);
      tvTemp2 = (TextView) findViewById(R.id.tvTemp2);
      tvTempMin2 = (TextView) findViewById(R.id.tvTempMin2);
      tvTempMax2 = (TextView) findViewById(R.id.tvTempMax2);
      tvTemp3 = (TextView) findViewById(R.id.tvTemp3);
      tvTempMin3 = (TextView) findViewById(R.id.tvTempMin3);
      tvTempMax3 = (TextView) findViewById(R.id.tvTempMax3);
      tvTemp4 = (TextView) findViewById(R.id.tvTemp4);
      tvTempMin4 = (TextView) findViewById(R.id.tvTempMin4);
      tvTempMax4 = (TextView) findViewById(R.id.tvTempMax4);
      tvTemp5 = (TextView) findViewById(R.id.tvTemp5);
      tvTempMin5 = (TextView) findViewById(R.id.tvTempMin5);
      tvTempMax5 = (TextView) findViewById(R.id.tvTempMax5);
      tvHumi1 = (TextView) findViewById(R.id.tvHumi1);
      tvHumiMin1 = (TextView) findViewById(R.id.tvHumiMin1);
      tvHumiMax1 = (TextView) findViewById(R.id.tvHumiMax1);
      tvHumi2 = (TextView) findViewById(R.id.tvHumi2);
      tvHumiMin2 = (TextView) findViewById(R.id.tvHumiMin2);
      tvHumiMax2 = (TextView) findViewById(R.id.tvHumiMax2);
      tvHumi3 = (TextView) findViewById(R.id.tvHumi3);
      tvHumiMin3 = (TextView) findViewById(R.id.tvHumiMin3);
      tvHumiMax3 = (TextView) findViewById(R.id.tvHumiMax3);
      tvHumi4 = (TextView) findViewById(R.id.tvHumi4);
      tvHumiMin4 = (TextView) findViewById(R.id.tvHumiMin4);
      tvHumiMax4 = (TextView) findViewById(R.id.tvHumiMax4);
      tvPres5 = (TextView) findViewById(R.id.tvPres5);
      tvPresMin5 = (TextView) findViewById(R.id.tvPresMin5);
      tvPresMax5 = (TextView) findViewById(R.id.tvPresMax5);
   }
   
   // start activity
   private void launchEditnameActivity(int position) {
      Intent i = new Intent(this,EditnameActivity.class);
      i.putExtra("position",position);
      startActivity(i);
   }
   
   // start activity
   private void launchSensorActivity(int position,int sensor) {
      Intent i = new Intent(this,SensorActivity.class);
      i.putExtra("position",position);
      i.putExtra("sensor",sensor);
      startActivity(i);
   }
   
}
