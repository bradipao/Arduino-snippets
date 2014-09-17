package it.bradipao.podval;

import java.io.Serializable;
import java.util.Date;

public class ArduinoItem implements Serializable {

   private static final long serialVersionUID = 1L;
   public static final int HISTSIZE = 24;
   public String sName,sHwid,sIp;
   public boolean bOk;

   public long lastfetch = 0;
   public Date lastdate;
   public boolean s1,s2,s3,s4,s5;
   public ArduinoData data = new ArduinoData();
   public ArduinoData[] hist = new ArduinoData[HISTSIZE];
   public ArduinoData min = new ArduinoData();
   public ArduinoData max = new ArduinoData();
   
   // constructor
   public ArduinoItem(String sName,String sHwid,String sIp) {
      super();
      this.sName = sName;
      this.sHwid = sHwid;
      this.sIp = sIp;
      this.bOk = false;
      s1 = s2 = s3 = s4 = s5 = true;
   }

   // constructor
   public ArduinoItem(String sName,String sHwid,String sIp,boolean bOk) {
      super();
      this.sName = sName;
      this.sHwid = sHwid;
      this.sIp = sIp;
      this.bOk = bOk;
      s1 = s2 = s3 = s4 = s5 = true;
   }
   
   // calculate min/max stats
   public void minmax() {
      float t1min,t2min,t3min,t4min,t5min;
      float t1max,t2max,t3max,t4max,t5max;
      float h1min,h2min,h3min,h4min,p5min;
      float h1max,h2max,h3max,h4max,p5max;
      t1min = t2min = t3min = t4min = t5min = 9999;
      t1max = t2max = t3max = t4max = t5max = -9999;
      h1min = h2min = h3min = h4min = p5min = 9999;
      h1max = h2max = h3max = h4max = p5max = -9999;
      for (int i=0;i<HISTSIZE;i++) {
         if (hist[i].ft1<t1min) t1min = hist[i].ft1;
         if (hist[i].ft1>t1max) t1max = hist[i].ft1;
         if (hist[i].ft2<t2min) t2min = hist[i].ft2;
         if (hist[i].ft2>t2max) t2max = hist[i].ft2;
         if (hist[i].ft3<t3min) t3min = hist[i].ft3;
         if (hist[i].ft3>t3max) t3max = hist[i].ft3;
         if (hist[i].ft4<t4min) t4min = hist[i].ft4;
         if (hist[i].ft4>t4max) t4max = hist[i].ft4;
         if (hist[i].ft5<t5min) t5min = hist[i].ft5;
         if (hist[i].ft5>t5max) t5max = hist[i].ft5;
         if (hist[i].fh1<h1min) h1min = hist[i].fh1;
         if (hist[i].fh1>h1max) h1max = hist[i].fh1;
         if (hist[i].fh2<h2min) h2min = hist[i].fh2;
         if (hist[i].fh2>h2max) h2max = hist[i].fh2;
         if (hist[i].fh3<h3min) h3min = hist[i].fh3;
         if (hist[i].fh3>h3max) h3max = hist[i].fh3;
         if (hist[i].fh4<h4min) h4min = hist[i].fh4;
         if (hist[i].fh4>h4max) h4max = hist[i].fh4;
         if (hist[i].fp5<p5min) p5min = hist[i].fp5;
         if (hist[i].fp5>p5max) p5max = hist[i].fp5;
      }
      min = new ArduinoData(t1min,t2min,t3min,t4min,t5min,h1min,h2min,h3min,h4min,p5min);
      max = new ArduinoData(t1max,t2max,t3max,t4max,t5max,h1max,h2max,h3max,h4max,p5max);
   }
   
}