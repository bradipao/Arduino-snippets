package it.bradipao.podval;

import java.io.Serializable;

public class ArduinoData implements Serializable {

   private static final long serialVersionUID = 1L;
   public long time;
   public float ft1,ft2,ft3,ft4;
   public float fh1,fh2,fh3,fh4;
   public float ft5,fp5;

   public ArduinoData() {
      super();
      this.time = 0;
      this.ft1 = -999;
      this.ft2 = -999;
      this.ft3 = -999;
      this.ft4 = -999;
      this.ft5 = -999;
      this.fh1 = -999;
      this.fh2 = -999;
      this.fh3 = -999;
      this.fh4 = -999;
      this.fp5 = -999;
   }
   
   public ArduinoData(float ft1,float ft2,float ft3,float ft4,float ft5,float fh1,float fh2,float fh3,float fh4,float fp5) {
      super();
      this.time = 0;
      this.ft1 = ft1;
      this.ft2 = ft2;
      this.ft3 = ft3;
      this.ft4 = ft4;
      this.ft5 = ft5;
      this.fh1 = fh1;
      this.fh2 = fh2;
      this.fh3 = fh3;
      this.fh4 = fh4;
      this.fp5 = fp5;
   }
   
   public ArduinoData(long time,float ft1,float ft2,float ft3,float ft4,float ft5,float fh1,float fh2,float fh3,float fh4,float fp5) {
      super();
      this.time = time;
      this.ft1 = ft1;
      this.ft2 = ft2;
      this.ft3 = ft3;
      this.ft4 = ft4;
      this.ft5 = ft5;
      this.fh1 = fh1;
      this.fh2 = fh2;
      this.fh3 = fh3;
      this.fh4 = fh4;
      this.fp5 = fp5;
   }
   
}