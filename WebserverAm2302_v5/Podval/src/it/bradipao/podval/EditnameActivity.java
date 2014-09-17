package it.bradipao.podval;

import java.lang.reflect.Field;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class EditnameActivity extends Activity {

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
   
   // views
   TextView tvName,tvHwid,tvIp;
   EditText etEditname;
   Button btSavename;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
      setContentView(R.layout.ac_editname);
      
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
      
      // set views
      tvName.setText(aitem.sName);
      tvHwid.setText(aitem.sHwid);
      tvIp.setText(aitem.sIp);
      etEditname.setText(aitem.sName);
      
      // button listener
      btSavename.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            aitem.sName = etEditname.getText().toString();
            finish();
         }
      });
      
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
      etEditname = (EditText) findViewById(R.id.etEditname);
      btSavename = (Button) findViewById(R.id.btSavename);
   }
}
