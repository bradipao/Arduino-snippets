// TODO
//  - togliere http:// dal getsubnet

package it.bradipao.podval;

import java.lang.reflect.Field;
import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends Activity {

   // constants
   private static final String LOGTAG = "PODVAL";
   private static final boolean LOGENABLE = true;
   private static void LOGD(String msg) { if (LOGENABLE) Log.d(LOGTAG,msg); }
   private static void LOGE(String msg) { if (LOGENABLE) Log.e(LOGTAG,msg); }
   
   // instances
   PodvalApp app;
   
   // views
   View vEmpty;
   ListView lvArdu;
   ArduinoAdapter adapter;
   
   // vars
   boolean bRun = false;
   boolean bCont = false;
   String sSubnet,sUrl;
   int iTimeout,ipFrom,ipTo,iPos;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
      setContentView(R.layout.ac_main);
      
      // setup
      getOverflowMenu();
      getViews();
      app = PodvalApp.getInstance();
      app.prefs = PreferenceManager.getDefaultSharedPreferences(this);
      app.prefs_editor = app.prefs.edit();
      app.netclient = new AsyncHttpClient();
      
      // load data from pref to ardb
      app.pref_toArdbList();

      // on click for listview
      lvArdu.setOnItemClickListener(new OnItemClickListener() {
         public void onItemClick(AdapterView<?> adapter,View view,int position,long id) {
            if (app.getArduinoItemDb().get(position).bOk) launchArduinoActivity(position);
            else if ((app.net_isPresent())&&(app.netclient!=null)) {
               iPos = position;
               bCont = false;
               setProgressBarIndeterminateVisibility(true);
               net_ArduPing();
            }
            //else net_CheckOnoff(position);
         }
      });
      
      // on longclick
      lvArdu.setOnItemLongClickListener(new OnItemLongClickListener() {
         public boolean onItemLongClick(AdapterView<?> adapter,View view,int position,long id) {
            launchEditnameActivity(position);
            return false;
         }
      });

   }

   // called when the activity STARTS
   @Override
   public void onStart() {
      super.onStart();
      // update arduino list
      adapter = new ArduinoAdapter(this,app.getArduinoItemDb());
      lvArdu.setAdapter(adapter);
      // setup net client
      iTimeout = Integer.parseInt(app.prefs.getString("prefkey_scan_timeout","2000"));
      app.netclient.setTimeout(iTimeout);
      // check for network
      if (!app.net_isPresent())
         Toast.makeText(getApplicationContext(),"NETWORK not found"+sUrl,Toast.LENGTH_LONG).show();
      // start check on-off cycles
      if ((app.net_isPresent())&&(app.netclient!=null)&&(app.getArduinoItemDb().size()>0)) {
         iPos = 0;
         bCont = true;
         setProgressBarIndeterminateVisibility(true);
         net_ArduPing();
      }
      
   }
   
   // called when the activity RESUMES
   @Override
   public void onResume() {
      super.onResume();
      // update vars when returning from preferences
      if (app.bLoadPref) {
         app.bLoadPref = false;
         setupVars();
      }      
   }
   
   // called when the activity DESTROYED
   @Override
   public void onDestroy() {
      super.onDestroy();
      app.pref_fromArdbList();
   }
   
   // actionbar MENU management
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.ac_main,menu);
      return true;
   }
   
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      // menu SETTINGS clicked
      case R.id.action_settings:
         startActivity(new Intent(this,PrefActivity.class));
         return true;
      // menu SCAN NETWORK clicked
      case R.id.action_netscan:
         net_StartScan();
         return true;
      // menu DEBUG ardu clear clicked
      case R.id.debug_arduclear:
         app.ardb_clearArdu();
         adapter.notifyDataSetChanged();
         return true;
      // menu DEBUG ardu test data clicked
      /*
      case R.id.debug_ardutest:
         app.ardb_addupdArdu(new ArduinoItem("FIRST arduino","11:22:33:44:55:01","192.168.1.3"));
         app.ardb_addupdArdu(new ArduinoItem("SECOND arduino","11:22:33:44:55:02","192.168.1.5"));
         app.ardb_addupdArdu(new ArduinoItem("THIRD arduino","11:22:33:44:55:03","192.168.1.11"));
         adapter.notifyDataSetChanged();
         return true;
      */
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
   
   private void net_ArduPing() {
      // build URL and start request 
      sUrl = app.getArduinoItemDb().get(iPos).sIp+"/podval";
      app.netclient.get(sUrl,new AsyncHttpResponseHandler() {
         
         // callback called on success
         @Override
         public void onSuccess(String response) {
            // this arduino responded, setting in database
            app.getArduinoItemDb().get(iPos).bOk = true;
            adapter.notifyDataSetChanged();
            // restart if requested and there are other arduino items in list
            iPos++;
            if (bCont&&(iPos<app.getArduinoItemDb().size())) {
               net_ArduPing();
            } else {
               setProgressBarIndeterminateVisibility(false);
               if (!bCont) launchArduinoActivity(iPos-1);
               iPos = 0;
               bCont = false;
            }
         }
         
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            // this arduino responded, setting in database
            app.getArduinoItemDb().get(iPos).bOk = false;
            adapter.notifyDataSetChanged();
            // restart if requested and there are other arduino items in list
            iPos++;
            if (bCont&&(iPos<app.getArduinoItemDb().size())) {
               net_ArduPing();
            } else {
               setProgressBarIndeterminateVisibility(false);
               iPos = 0;
               bCont = false;
            }
         }

      });

   }
   
   // Check on-off of a single arduino
   /*
   private void net_CheckOnoff(int position) {
      // check for net presence
      if ((app.net_isPresent())&&(app.netclient!=null)&&(iPos==0)) {
         // store current scan
         iPos = position;
         // setup client
         iTimeout = Integer.parseInt(app.prefs.getString("prefkey_scan_timeout","2000"));
         sUrl = app.getArduinoItemDb().get(iPos).sIp+"/podval";
         app.netclient.setTimeout(iTimeout);
         app.netclient.get(sUrl,new AsyncHttpResponseHandler() {
            
            // callback called on success
            @Override
            public void onSuccess(String response) {
               setProgressBarIndeterminateVisibility(false);
               app.getArduinoItemDb().get(iPos).bOk = true;
               adapter.notifyDataSetChanged();
               Toast.makeText(getApplicationContext(),sUrl+" ON",Toast.LENGTH_SHORT).show();
               iPos = 0;
            }
            
            // callback called on failure
            @Override
            public void onFailure(Throwable e,String response) {
               setProgressBarIndeterminateVisibility(false);
               app.getArduinoItemDb().get(iPos).bOk = false;
               adapter.notifyDataSetChanged();
               Toast.makeText(getApplicationContext(),sUrl+" OFF",Toast.LENGTH_SHORT).show();
               iPos = 0;
            }

         });
      } else {
         Toast.makeText(getApplicationContext(),"Not connected to network",Toast.LENGTH_LONG).show();
      }
   }
   */
   
   // network start scan
   private void net_StartScan() {
      // bRun = !bRun;
      setProgressBarIndeterminateVisibility(true);
      // get subnet,ipFrom,ipTo
      sSubnet = app.net_getSubnet();
      iTimeout = Integer.parseInt(app.prefs.getString("prefkey_scan_timeout","2000"));
      ipFrom = Integer.parseInt(app.prefs.getString("prefkey_autoscan_fromip","1"));
      ipTo = Integer.parseInt(app.prefs.getString("prefkey_autoscan_toip","30"));
      // debug
      if (app.net_isPresent()) {
         Toast.makeText(getApplicationContext(),"scanning subnet = "+app.net_getSubnet()+" from:"+ipFrom+" to:"+ipTo,Toast.LENGTH_SHORT).show();
      }
      // launch scan
      if ((app.netclient!=null)&(ipFrom<=ipTo)) net_IpScan();
   }
  
   // network scan function
   private void net_IpScan() {
      // build url
      sUrl = sSubnet.replace("*",Integer.toString(ipFrom));
      sUrl += "/podval";
      // setup net client
      app.netclient.setTimeout(iTimeout);
      app.netclient.get(sUrl,new AsyncHttpResponseHandler() {
         
         // callback called on success : url found
         @Override
         public void onSuccess(String response) {
            // stop progress bar
            if (ipFrom==ipTo) setProgressBarIndeterminateVisibility(false);
            // result
            Toast.makeText(getApplicationContext(),"FOUND at "+sUrl,Toast.LENGTH_SHORT).show();
            app.ardb_addupdArdu(new ArduinoItem("Arduino."+ipFrom,json_getHwid(response),sSubnet.replace("*",Integer.toString(ipFrom))));
            adapter.notifyDataSetChanged();
            // if not finished, start again
            if (ipFrom<ipTo) {
               ipFrom++;
               net_IpScan();
            }
         }
         
         // callback called on failure : continue scanning
         @Override
         public void onFailure(Throwable e,String response) {
            // stop progress bar
            if (ipFrom==ipTo) setProgressBarIndeterminateVisibility(false);
            // if not finished, start again
            if (ipFrom<ipTo) {
               ipFrom++;
               net_IpScan();
            }
         }

      });

   }
   
   // helper function to retrieve HWID from json
   private String json_getHwid(String r) {
      try {
         JSONObject jso = new JSONObject(r);
         return jso.getString("hwid");
      } catch (JSONException e) {
         LOGE("Error in parsing JSON data");
         return null;
      }
   }
   
   // get views
   private void getViews() {
      vEmpty = findViewById(R.id.emptyView);
      lvArdu = (ListView) findViewById(R.id.lvArduitems);
      lvArdu.setEmptyView(vEmpty);
   }

   // setup activity local variables from preferences
   private void setupVars() {
      if (app.prefs==null) return;
   }
   
   // start activity
   private void launchArduinoActivity(int position) {
      Intent i = new Intent(this,ArduinoActivity.class);
      i.putExtra("position",position);
      startActivity(i);
   }
   
   // start activity
   private void launchEditnameActivity(int position) {
      Intent i = new Intent(this,EditnameActivity.class);
      i.putExtra("position",position);
      startActivity(i);
   }
   
}
