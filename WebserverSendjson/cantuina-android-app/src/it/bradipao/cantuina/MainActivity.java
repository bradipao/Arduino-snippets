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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.echo.holographlibrary.Line;
import com.echo.holographlibrary.LineGraph;
import com.echo.holographlibrary.LineGraph.OnPointClickedListener;
import com.echo.holographlibrary.LinePoint;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class MainActivity extends Activity {

   // views
   Button btnAdd,btnShift,btnScan,btnFetch;
   TextView tvNetresult,tvTemp,tvPres,tvHumi;
   
   // graph vars
   LineGraph li;
   Line l;
   LinePoint p;
   int dx = 15;

   // net vars
   String starturl = "";    // starting URL with * on the 4th IP number
   String pingurl = "";     // URL being pinged during scanning
   String serverurl = "";   // final URL after successful scanning
   int pingFrom,pingTo;     // 4th IP number scanned From and To
   long mTime = 0;          // Time and DeltaTime for measuring network latency
   long mDeltaTime = 0;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.ac_main);
      
      // get views
      getViews();
      
      // line example
      l = new Line();
      l.addPoint(new LinePoint(0,5));
      l.addPoint(new LinePoint(8,8));
      l.addPoint(new LinePoint(10,4));
      l.setColor(Color.parseColor("#6699CC"));
      l.setStrokeWidth(2);
      
      // set graph
      li.addLine(l);
      li.setRangeY(0,40);
      li.setOnPointClickedListener(new OnPointClickedListener(){
         @Override
         public void onClick(int lineIndex,int pointIndex) {           
         }
      });
      
      // add button listener
      btnAdd.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {     
            li.addPoint(0,new LinePoint(dx,(dx%11)));
            dx += 1;
         }
      });
      
      // shift button listener
      btnShift.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            li.shiftPoint(0,new LinePoint(dx,(dx%11)),10);
            dx += 3;
         }
      });
      
      // scan button listener
      btnScan.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            starturl = "http://10.0.0.*/cantuina";
            pingFrom = 1;
            pingTo = 20;
            scanNet();
         }
      });
      
      // fetch button listener
      btnFetch.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            if (!serverurl.isEmpty()) fetchValues();
         }
      });
      
   }

   // SCANNET : scan addresses in local subnet
   // if starturl = "http://10.0.0.*" and pingFrom=1 pingTo=20
   // scans from 10.0.0.1 to 10.0.0.19 until someone answers
   private void scanNet() {
      // http client instance
      AsyncHttpClient client = new AsyncHttpClient();
      client.setTimeout(1000);
      // calculating url to ping
      pingurl = starturl.replace("*",Integer.toString(pingFrom));
      mTime = System.currentTimeMillis();
      // ping url
      client.get(pingurl,new AsyncHttpResponseHandler() {
         // callback called on success
         @Override
         public void onSuccess(String response) {
            mDeltaTime = System.currentTimeMillis() - mTime;
            String txt = pingurl.substring(0,pingurl.indexOf("/",8));
            tvNetresult.setText(txt+" OK in "+mDeltaTime+" ms");
            // save server url on success
            serverurl = pingurl;
         }
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            mDeltaTime = System.currentTimeMillis() - mTime;
            String txt = pingurl.substring(0,pingurl.indexOf("/",8));
            tvNetresult.setText(txt+" FAILED in "+mDeltaTime+" ms");
            // continue scanning on failure
            if (pingFrom<pingTo) {
               pingFrom++;
               scanNet();
            }
         }
      });
   }
   
   // FETCHVALUES : fetch values from serverurl
   private void fetchValues() {
      // http client instance
      AsyncHttpClient client = new AsyncHttpClient();
      client.setTimeout(1000);
      // fecth values
      client.get(serverurl,new AsyncHttpResponseHandler() {
         // callback called on success
         @Override
         public void onSuccess(String response) {

            // parse json from server
            // example : {"res":"OK","temp":"25.0","pres":"1020","humi":"45"}
            try {
               JSONObject jso = new JSONObject(response);
               float iTemp = (float) jso.getDouble("temp");
               int iPres = jso.getInt("pres");
               int iHumi = jso.getInt("humi");
               tvTemp.setText(iTemp+" °C");
               tvPres.setText(iPres+" mbar");
               tvHumi.setText(iHumi+" %");
               li.shiftPoint(0,new LinePoint(dx,iTemp),20);
               dx++;
            } catch (JSONException e) {
            }
            
         }
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            tvTemp.setText("ERROR fetching values");
         }
      });
   }
   
   // [UNUSED] PINGADDRESS : ping given address
   private void pingAddress(String url) {
      AsyncHttpClient client = new AsyncHttpClient();
      client.setTimeout(1000);
      pingurl = url;
      mTime = System.currentTimeMillis();
      client.get(pingurl,new AsyncHttpResponseHandler() {
         // callback called on success
         @Override
         public void onSuccess(String response) {
            mDeltaTime = System.currentTimeMillis() - mTime;
            tvNetresult.setText(pingurl+" OK in "+mDeltaTime+" ms");
         }
         // callback called on failure
         @Override
         public void onFailure(Throwable e,String response) {
            mDeltaTime = System.currentTimeMillis() - mTime;
            tvNetresult.setText(pingurl+" FAILED in "+mDeltaTime+" ms");
         }
      });
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.main, menu);
      return true;
   }

   // initialize views references
   private void getViews() {
      li = (LineGraph)findViewById(R.id.linegraph);
      btnAdd = (Button)findViewById(R.id.btnAdd);
      btnShift = (Button)findViewById(R.id.btnShift);
      btnScan = (Button)findViewById(R.id.btnScan);
      tvNetresult = (TextView)findViewById(R.id.tvNetresult);
      btnFetch = (Button)findViewById(R.id.btnFetch);
      tvTemp = (TextView)findViewById(R.id.tvTemp);
      tvPres = (TextView)findViewById(R.id.tvPres);
      tvHumi = (TextView)findViewById(R.id.tvHumi);
   }
}
