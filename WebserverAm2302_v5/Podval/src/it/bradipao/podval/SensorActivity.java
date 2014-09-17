package it.bradipao.podval;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class SensorActivity extends Activity {
   
   // constants
   private static final String LOGTAG = "PODVAL";
   private static final boolean LOGENABLE = true;
   private static void LOGD(String msg) { if (LOGENABLE) Log.d(LOGTAG,msg); }
   private static void LOGE(String msg) { if (LOGENABLE) Log.e(LOGTAG,msg); }
   
   // vars
   PodvalApp app;
   ArduinoItem aitem;
   ActionBar actionBar;
   int position,sensor;
   SimpleAdapter adapter;
   ArrayList<HashMap<String,String>> sensordata;
   SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
   
   // views
   TextView tvName,tvHwid,tvIp;
   ListView lvSensordata;
   
   // called when the activity is first created
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ac_sensor);
      
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
      sensor = (int) extras.getInt("sensor",-1);
      if (position==-1) finish();
      aitem = app.getArduinoItemDb().get(position);

      // create listview
      sensordata = new ArrayList<HashMap<String,String>>();
      updateSensordata(sensor);
      String[] from={"time","temp","humi"};
      int[] to={R.id.tvTime,R.id.tvTemp,R.id.tvHumi};
      adapter = new SimpleAdapter(getApplicationContext(),sensordata,R.layout.lv_sensordata,from,to);
      lvSensordata.setAdapter(adapter);
      
   }
   
   // called when the activity RESUMES
   @Override
   public void onResume() {
      super.onResume();
      // update views
      tvName.setText(aitem.sName);
      tvHwid.setText(aitem.sHwid);
      tvIp.setText(aitem.sIp);
   }
   
   private void updateSensordata(int sensor) {
      // vars
      HashMap<String,String> map;
      String sTime,sTemp,sHumi;
      long delta;
      Date tmpdate;
      // fill arraylist
      for (int ii=0;ii<ArduinoItem.HISTSIZE;ii++) {
         delta = aitem.hist[ii].time - aitem.data.time;
         tmpdate = new Date(aitem.lastdate.getTime()+delta);
         sTime = sdf.format(tmpdate);
         if (sensor==1) {
            sTemp = String.format("%.1f °C",aitem.hist[ii].ft1);
            sHumi = String.format("%.1f %%",aitem.hist[ii].fh1);
         } else if (sensor==2) {
            sTemp = String.format("%.1f °C",aitem.hist[ii].ft2);
            sHumi = String.format("%.1f %%",aitem.hist[ii].fh2);
         } else if (sensor==3) {
            sTemp = String.format("%.1f °C",aitem.hist[ii].ft3);
            sHumi = String.format("%.1f %%",aitem.hist[ii].fh3);
         } else if (sensor==4) {
            sTemp = String.format("%.1f °C",aitem.hist[ii].ft4);
            sHumi = String.format("%.1f %%",aitem.hist[ii].fh4);
         } else {
            sTemp = String.format("%.1f °C",aitem.hist[ii].ft5);
            sHumi = String.format("%.1f mbar",aitem.hist[ii].fp5);
         } 
         if (sTemp.contains("999.")) sTemp = "--.-";
         if (sHumi.contains("999.")) sHumi = "--.-";
         map = new HashMap<String,String>();
         map.put("time",sTime);
         map.put("temp",sTemp);
         map.put("humi",sHumi);
         sensordata.add(map);
      }
   }
   
   // called when a menu item is selected
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      // respond to the action bar's UP/HOME button
      case android.R.id.home:
         finish();
         return true;
      }
      return super.onOptionsItemSelected(item);
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
      tvName = (TextView) findViewById(R.id.tvName);
      tvHwid = (TextView) findViewById(R.id.tvHwid);
      tvIp = (TextView) findViewById(R.id.tvIp);
      lvSensordata = (ListView) findViewById(R.id.lvSensordata);
   }
   
}
