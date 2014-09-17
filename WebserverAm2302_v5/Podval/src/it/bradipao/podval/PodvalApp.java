package it.bradipao.podval;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;

public class PodvalApp extends Application {

   // constants
   private static final String LOGTAG = "PODVAL";
   private static final boolean LOGENABLE = true;
   private static void LOGD(String msg) { if (LOGENABLE) Log.d(LOGTAG,msg); }
   private static void LOGE(String msg) { if (LOGENABLE) Log.e(LOGTAG,msg); }
   
   // application instance 
   private static PodvalApp instance = null;
   
   // objects
   private ArrayList<ArduinoItem> arDB = new ArrayList<ArduinoItem>();
   public SharedPreferences prefs = null;
   public SharedPreferences.Editor prefs_editor = null;
   AsyncHttpClient netclient = null;
   
   // vars
   boolean bLoadPref = true;

   // accessor for ArduinoItem arraylist
   public ArrayList<ArduinoItem> getArduinoItemDb() {
      return arDB;
   }
   
   // ARDB add/upd function
   public void ardb_addupdArdu(ArduinoItem item) {
      ArduinoItem tmp;
      boolean bUpd = false;
      // item is active if added now
      item.bOk = true;
      // search for existing hwid and update if found
      for (int i=0;i<arDB.size();i++) {
         tmp = arDB.get(i);
         if (tmp.sHwid.equals(item.sHwid)) {
            tmp.sName = item.sName;
            tmp.sIp = item.sIp;
            bUpd = true;
         }
      }
      // if not updated add item
      if (!bUpd) arDB.add(item);
   }
   
   // ARDB del function
   public void ardb_delArdu(ArduinoItem item) {
      // search for hwid and remove if found
      for (int i=0;i<arDB.size();i++) {
         if (arDB.get(i).sHwid.equals(item.sHwid)) arDB.remove(i);
      }
   }
   
   // ARDB clear function
   public void ardb_clearArdu() {
      arDB.clear();
   }
   
   // PREF get ardb count
   public int pref_getArdbCount() {
      return arDB.size();
   }
   
   // PREF from list to pref
   public void pref_fromArdbList() {
      // vars
      int num = arDB.size();
      SharedPreferences.Editor editor = prefs.edit();
      // put number of Arduino items
      editor.putInt("ardb_num",num);
      // put each item
      for (int i=0;i<num;i++) {
         editor.putString("ardb_i"+i+"_name",arDB.get(i).sName);
         editor.putString("ardb_i"+i+"_hwid",arDB.get(i).sHwid);
         editor.putString("ardb_i"+i+"_ip",arDB.get(i).sIp);
      }
      // commit insert
      editor.commit();
   }
   
   // PREF from pref to list
   public void pref_toArdbList() {
      // create ARDB if null and clear all
      getArduinoItemDb().clear();
      // get number of Arduino items
      int num = prefs.getInt("ardb_num",0);
      // get each item
      String sName,sHwid,sIp;
      for (int i=0;i<num;i++) {
         sName = prefs.getString("ardb_i"+i+"_name","NOT VALID");
         sHwid = prefs.getString("ardb_i"+i+"_hwid","not valid");
         sIp = prefs.getString("ardb_i"+i+"_ip","bad ip");
         arDB.add(new ArduinoItem(sName,sHwid,sIp));
      }
   }
   
   // easy access to instance, avoid call and cast getApplicationContext() 
   public static PodvalApp getInstance() {
      checkInstance();
      return instance;
   }

   // check app instance
   private static void checkInstance() {
      if (instance==null)
         throw new IllegalStateException("Application not created yet!");
   }

   // oncreate app
   @Override
   public void onCreate() {
      super.onCreate();
      instance = this;   // store instance of app for static access
   }
   
   // ISNETWORKPRESENT : check for network connection
   public boolean net_isPresent() {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      //NetworkInfo activeWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      NetworkInfo activeWifi = cm.getActiveNetworkInfo();
      return ((activeWifi!=null)&&(activeWifi.isConnected()));
   }
   
   // GETSUBNET : identify subnet address
   public String net_getSubnet() {
      String subnet = "10.0.0.1";
      // extract true subnet
      try {
         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = (NetworkInterface) en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
               InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
               if (!inetAddress.isLoopbackAddress()) {
                  // comment next on emulator
                  //subnet = inetAddress.getHostAddress().toString();
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
