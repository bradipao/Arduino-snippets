/*
   WebserverAm2302_v4
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/podval
   - send history on http://aaa.bbb.ccc.ddd/podval?hist=1
   - read four DHT22 am2302 sensor with DHTlib http://playground.arduino.cc/Main/DHTLib
   - answer in json format
   - temperature and pressure from BMP085 I2C sensor
   - BMP085 Code by Jim Lindblom, SparkFun Electronics, date: 1/18/11, updated: 2/26/13
   - license: CC BY-SA v3.0 - http://creativecommons.org/licenses/by-sa/3.0/

 */

// include
#include <Wire.h>
#include <SPI.h>
#include <Ethernet.h>
#include <dht.h>

// defines
#define HISTSIZE            12  // History size in number of samples
#define BMP085_ADDRESS    0x77  // I2C address of BMP085
#define DHT22_PIN            6  // DHT22 connected on pin 6
#define DHT22_PIN1           7  // DHT22-1 connected on pin 7
#define DHT22_PIN2           6  // DHT22-2 connected on pin 6
#define DHT22_PIN3           5  // DHT22-3 connected on pin 5
#define DHT22_PIN4           8  // DHT22-4 connected on pin 8
#define DHT22_MINTIME     2000  // minimum ms between DHT22 readings
#define SAMPLETIME     1800000  // sampling interval 30m*60s*1000ms = 1800000 ms
#define CHECKTIME        60000  // sampling interval     60s*1000ms =   60000 ms

// increment last byte of MAC for multi-arduino in the same subnet
byte mac[] = { 0xDE,0xAA,0xBB,0xCC,0xDD,0x02 };

// pressure at sea level
const float p0 = 101325;

// globals
unsigned long tnow,thist,tcheck,delta;
char cmd;
char _buf[128];
int ethok=0;
dht DHT;
boolean isreqline = true;
boolean ishistory;
String spodval = String("podval");
String shist = String("hist");
String req = String();
IPAddress ip(0,0,0,0);
EthernetServer server(80);

// bmp085 vars
// Calibration values
int ac1,ac2,ac3; 
unsigned int ac4,ac5,ac6;
int b1,b2,mb,mc,md;
long b5;  // so ...Temperature(...) must be called before ...Pressure(...).

// history data
int kk;
int idx = 0;
int idprev = 0;
int t1,h1,t2,h2,t3,h3,t4,h4,t5;
long p5;
int t1h[HISTSIZE],h1h[HISTSIZE];
int t2h[HISTSIZE],h2h[HISTSIZE];
int t3h[HISTSIZE],h3h[HISTSIZE];
int t4h[HISTSIZE],h4h[HISTSIZE];
int t5h[HISTSIZE];
long p5h[HISTSIZE];
unsigned long histime[HISTSIZE];

// the setup routine runs once when you press reset:
void setup() {  
   // init vars 
   thist=0;
   tcheck=0;
   
   // open serial communications and wait for port to open:
   Serial.begin(115200);
   while (!Serial) {
      ; // wait for serial port to connect
   }
   Serial.println(F("UART OK"));
  
   // free ram
   Serial.print(F("FREE : "));
   Serial.println(freeRam());
 
   // I2C init
   Wire.begin();
   Serial.println(F("I2C OK"));
 
   // DHT22 test
   checkDHT22(DHT22_PIN1);
   checkDHT22(DHT22_PIN2);
   checkDHT22(DHT22_PIN3);
   checkDHT22(DHT22_PIN4);
   
   // BMP085 calibration
   bmp085Calibration();
   
   // ethernet setup (using DHCP can be blocking for 60 seconds)
   while ((ethok==0)&&(ip[0]==0))
      restartEthernetShield();
      
   // preset history
   read_sensors();
   fill_history();   
}

void restartEthernetShield() {  
  delay(100);
  Serial.println(F("ETH RESET"));
  ethok = Ethernet.begin(mac);
  if (ethok==0) {
    Serial.println(F("ETH ERROR"));
  } else {
    // extract ip
    ip = Ethernet.localIP();
    // start server
    server.begin();
    // start listening for clients
    formatIP(ip);
    Serial.print(F("IP "));
    Serial.println(_buf);
  }
  delay(100);
}

// loop routine
void loop() {
 
   // SAMPLING DATA and storing history at SAMPLETIME rate
   tnow = millis();
   if ((tnow - thist)>=SAMPLETIME) {
      // read and store in circular buffers
      read_sensors();
      t1h[idx] = t1;
      h1h[idx] = h1;
      t2h[idx] = t2;
      h2h[idx] = h2;
      t3h[idx] = t3;
      h3h[idx] = h3;
      t4h[idx] = t4;
      h4h[idx] = h4;
      t5h[idx] = t5;
      p5h[idx] = p5;
      histime[idx] = tnow;
      idprev = idx;
      idx = (idx+1) % HISTSIZE;
      thist = tnow;
      tcheck = tnow;
   }
   
   // CHECK FOR ERROR IN LAST SAMPLED DATA
   else if ((tnow - tcheck)>=CHECKTIME) {
      // if last history has errors, resample and update
      if ((t1h[idprev]<=-999) || (h1h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN1);
         t1h[idprev] = 10*DHT.temperature;
         h1h[idprev] = 10*DHT.humidity;
      }
      if ((t2h[idprev]<=-999) || (h2h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN2);
         t2h[idprev] = 10*DHT.temperature;
         h2h[idprev] = 10*DHT.humidity;;
      }
      if ((t3h[idprev]<=-999) || (h3h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN3);
         t3h[idprev] = 10*DHT.temperature;
         h3h[idprev] = 10*DHT.humidity;
      }
      if ((t4h[idprev]<=-999) || (h4h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN4);
         t4h[idprev] = 10*DHT.temperature;
         h4h[idprev] = 10*DHT.humidity;
      }
      tcheck = tnow;
   }

   // WEB SERVER
   EthernetClient client = server.available();   // listen to client connecting
   if (client) {
      // new client http request
      boolean currentLineIsBlank = true;   // an http request ends with a blank line
      uint8_t pos = 0;                     // buffer index
      while (client.connected()) {
         if (client.available()) {
            // append to request string
            char c = client.read();
            if ((isreqline)&&(pos<127)) _buf[pos++] = c;
            // stop parsing after first line
            if ((isreqline)&&(c=='\n')) {
               isreqline = false;
               _buf[pos-2] = 0x00;
            }

            // if you've gotten to the end of the line (received a newline character) and the line is blank,
            // the http request has ended, so you can send a reply
            if ((!isreqline) && currentLineIsBlank) {
               
               //Serial.print(F("HTTP "));
               req += _buf;
               //Serial.println(req);
               
               // if request does not contain "podval" keyword send 404
               if (req.indexOf(spodval)==-1) {
                  send_404(client);
               }

               // else send JSON history if requested by "hist=1" param
               else if (req.indexOf(shist)!=-1) {
                  // response header
                  client.println(F("HTTP/1.1 200 OK"));
                  client.println(F("Content-Type: application/json"));
                  client.println(F("Connection: close"));
                  client.println();
                  // read sensors
                  read_sensors();
                  // open JSON
                  client.print(F("{"));
                  // result header
                  client.print(F("\"res\":\"OK\""));
                  formatMAC();
                  client.print(F(",\"hwid\":\""));
                  client.print(_buf);
                  client.print(F("\""));
                  // time
                  client.print(F(",\"time\":\""));
                  client.print(tnow);
                  client.print(F("\""));
                  // temp+humi DHT22/1
                  client.print(F(",\"t1\":\""));
                  client.print(t1,1);
                  client.print(F("\""));
                  client.print(F(",\"h1\":\""));
                  client.print(h1,1);
                  client.print(F("\""));
                  // temp+humi DHT22/2
                  client.print(F(",\"t2\":\""));
                  client.print(t2,1);
                  client.print(F("\""));
                  client.print(F(",\"h2\":\""));
                  client.print(h2,1);
                  client.print(F("\""));
                  // temp+humi DHT22/3
                  client.print(F(",\"t3\":\""));
                  client.print(t3,1);
                  client.print(F("\""));
                  client.print(F(",\"h3\":\""));
                  client.print(h3,1);
                  client.print(F("\""));
                  // temp+humi DHT22/4
                  client.print(F(",\"t4\":\""));
                  client.print(t4,1);
                  client.print(F("\""));
                  client.print(F(",\"h4\":\""));
                  client.print(h4,1);
                  client.print(F("\""));
                  // temp+pres BMP085
                  client.print(F(",\"t5\":\""));
                  client.print(t5,1);
                  client.print(F("\""));
                  client.print(F(",\"p5\":\""));
                  client.print(p5,1);
                  client.print(F("\""));
                  // send out circular buffer 1 starting from oldest (idx)
                  client.print(F(",\"hist\":["));
                  for (kk=0;kk<HISTSIZE;kk++) {
                     // start json array item
                     if (kk!=0) client.print(F(",{"));
                     else client.print(F("{"));
                     // time
                     client.print(F("\"time\":\""));
                     client.print(histime[(kk+idx)%HISTSIZE]);
                     client.print(F("\""));
                     // temperature 1
                     client.print(F(",\"t1\":\""));
                     client.print(t1h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // humidity 1
                     client.print(F(",\"h1\":\""));
                     client.print(h1h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // temperature 2
                     client.print(F(",\"t2\":\""));
                     client.print(t2h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // humidity 2
                     client.print(F(",\"h2\":\""));
                     client.print(h2h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // temperature 3
                     client.print(F(",\"t3\":\""));
                     client.print(t3h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // humidity 3
                     client.print(F(",\"h3\":\""));
                     client.print(h3h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // temperature 4
                     client.print(F(",\"t4\":\""));
                     client.print(t4h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // humidity 4
                     client.print(F(",\"h4\":\""));
                     client.print(h4h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // temperature 5
                     client.print(F(",\"t5\":\""));
                     client.print(t5h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // pressure 5
                     client.print(F(",\"p5\":\""));
                     client.print(p5h[(kk+idx)%HISTSIZE],1);
                     client.print(F("\""));
                     // end json array item
                     client.print(F("}"));
                  }
                  client.print(F("]"));
                  // close json
                  client.println(F("}"));
                  // serial logging
               }

               // else send JSON response with instant data
               else {
                  // start sensor reading if enough time elapsed since last read
                  //if ((tnow - thist)>DHT22_MINTIME) DHT.read22(DHT22_PIN);
                  read_sensors();
                  // response header
                  client.println(F("HTTP/1.1 200 OK"));
                  client.println(F("Content-Type: application/json"));  // JSON response type
                  client.println(F("Connection: close"));               // close connection after response
                  client.println();
                  // open JSON
                  client.print(F("{"));
                  // result header
                  client.print(F("\"res\":\"OK\""));
                  formatMAC();
                  client.print(F(",\"hwid\":\""));
                  client.print(_buf);
                  client.print(F("\""));
                  // time
                  client.print(F(",\"time\":\""));
                  client.print(tnow);
                  client.print(F("\""));
                  // temp+humi DHT22/1
                  client.print(F(",\"t1\":\""));
                  client.print(t1,1);
                  client.print(F("\""));
                  client.print(F(",\"h1\":\""));
                  client.print(h1,1);
                  client.print(F("\""));
                  // temp+humi DHT22/2
                  client.print(F(",\"t2\":\""));
                  client.print(t2,1);
                  client.print(F("\""));
                  client.print(F(",\"h2\":\""));
                  client.print(h2,1);
                  client.print(F("\""));
                  // temp+humi DHT22/3
                  client.print(F(",\"t3\":\""));
                  client.print(t3,1);
                  client.print(F("\""));
                  client.print(F(",\"h3\":\""));
                  client.print(h3,1);
                  client.print(F("\""));
                  // temp+humi DHT22/4
                  client.print(F(",\"t4\":\""));
                  client.print(t4,1);
                  client.print(F("\""));
                  client.print(F(",\"h4\":\""));
                  client.print(h4,1);
                  client.print(F("\""));
                  // temp+pres BMP085
                  client.print(F(",\"t5\":\""));
                  client.print(t5,1);
                  client.print(F("\""));
                  client.print(F(",\"p5\":\""));
                  client.print(p5,1);
                  client.print(F("\""));
                  // close json
                  client.println(F("}"));
               }

               // prepare for next request
               isreqline = true;
               req = "";
               // exit
               break;
            }
            if (c=='\n') {
               currentLineIsBlank = true;  // you're starting a new line
            } 
            else if (c!='\r') {
               currentLineIsBlank = false; // you've gotten a character on the current line
            }
         }
      }
      // give the web browser time to receive the data and close the connection
      delay(1);
      client.stop();
   }   

}

void fill_history() {
   for (kk=0;kk<HISTSIZE;kk++) {
      t1h[kk] = t1;
      h1h[kk] = h1;
      t2h[kk] = t2;
      h2h[kk] = h2;
      t3h[kk] = t3;
      h3h[kk] = h3;
      t4h[kk] = t4;
      h4h[kk] = h4;
      t5h[kk] = t5;
      p5h[kk] = p5;
   }
}

void read_sensors() {
   DHT.read22(DHT22_PIN1);
   t1 = 10*DHT.temperature;
   h1 = 10*DHT.humidity;
   DHT.read22(DHT22_PIN2);
   t2 = 10*DHT.temperature;
   h2 = 10*DHT.humidity;
   DHT.read22(DHT22_PIN3);
   t3 = 10*DHT.temperature;
   h3 = 10*DHT.humidity;
   DHT.read22(DHT22_PIN4);
   t4 = 10*DHT.temperature;
   h4 = 10*DHT.humidity;   
   t5 = bmp085GetTemperature(bmp085ReadUT());
   p5 = bmp085GetPressure(bmp085ReadUP());
}

int checkDHT22(int dht22pin) {
   int chk = DHT.read22(dht22pin);
   Serial.print(F("DHT22-pin"));
   Serial.print(dht22pin);
   switch (chk) {
      case DHTLIB_OK:  
         Serial.println(F(" OK")); 
         break;
      default: 
         Serial.println(F(" ERROR")); 
         break;
   }
}

void formatIP(IPAddress ii) {
   sprintf(_buf,"%d.%d.%d.%d",ii[0],ii[1],ii[2],ii[3]);
}

void formatMAC() {
   sprintf(_buf,"%02x:%02x:%02x:%02x:%02x:%02x",mac[0],mac[1],mac[2],mac[3],mac[4],mac[5]);
}

static void send_404(EthernetClient client) {
   client.println(F("HTTP/1.1 404 Not    Found"));
   client.println(F("Content-Type: text/html"));
   client.println();
   client.println(F("404 ARDUINO NOT FOUND"));
}

int freeRam() {
   extern int __heap_start,*__brkval;
   int v;
   return (int) &v - (__brkval == 0 ? (int) &__heap_start : (int) __brkval);
}

// Stores all of the bmp085's calibration values into global variables
// Calibration values are required to calculate temp and pressure
// This function should be called at the beginning of the program
void bmp085Calibration() {
   ac1 = bmp085ReadInt(0xAA);
   ac2 = bmp085ReadInt(0xAC);
   ac3 = bmp085ReadInt(0xAE);
   ac4 = bmp085ReadInt(0xB0);
   ac5 = bmp085ReadInt(0xB2);
   ac6 = bmp085ReadInt(0xB4);
   b1 = bmp085ReadInt(0xB6);
   b2 = bmp085ReadInt(0xB8);
   mb = bmp085ReadInt(0xBA);
   mc = bmp085ReadInt(0xBC);
   md = bmp085ReadInt(0xBE);
}

// Calculate temperature
// Value returned will be in units of 0.1 deg C
short bmp085GetTemperature(unsigned int ut) {
   long x1,x2;

   x1 = (((long)ut - (long)ac6)*(long)ac5) >> 15;
   x2 = ((long)mc << 11)/(x1 + md);
   b5 = x1 + x2;

   return ((b5 + 8)>>4);  
}

// Calculate pressure, calibration values must be known
// b5 is also required so bmp085GetTemperature(...) must be called first.
// Value returned will be pressure in units of Pa.
long bmp085GetPressure(unsigned long up) {
   long x1,x2,x3,b3,b6,p;
   unsigned long b4,b7;
  
   b6 = b5 - 4000;
   // Calculate B3
   x1 = (b2 * (b6 * b6)>>12)>>11;
   x2 = (ac2 * b6)>>11;
   x3 = x1 + x2;
   b3 = (((((long)ac1)*4 + x3)<<0) + 2)>>2;
  
   // Calculate B4
   x1 = (ac3 * b6)>>13;
   x2 = (b1 * ((b6 * b6)>>12))>>16;
   x3 = ((x1 + x2) + 2)>>2;
   b4 = (ac4 * (unsigned long)(x3 + 32768))>>15;
  
   b7 = ((unsigned long)(up - b3) * (50000>>0));
   if (b7 < 0x80000000)
      p = (b7<<1)/b4;
   else
      p = (b7/b4)<<1;
    
   x1 = (p>>8) * (p>>8);
   x1 = (x1 * 3038)>>16;
   x2 = (-7357 * p)>>16;
   p += (x1 + x2 + 3791)>>4;
  
   return p;
}

// Read 1 byte from the BMP085 at 'address'
char bmp085Read(unsigned char address) {
   unsigned char data;
  
   Wire.beginTransmission(BMP085_ADDRESS);
   Wire.write(address);
   Wire.endTransmission();
  
   Wire.requestFrom(BMP085_ADDRESS,1);
   while(!Wire.available())
      ;
    
   return Wire.read();
}

// Read 2 bytes from the BMP085
// First byte will be from 'address'
// Second byte will be from 'address'+1
int bmp085ReadInt(unsigned char address) {
   unsigned char msb,lsb;
  
   Wire.beginTransmission(BMP085_ADDRESS);
   Wire.write(address);
   Wire.endTransmission();
  
   Wire.requestFrom(BMP085_ADDRESS,2);
   while(Wire.available()<2)
     ;
   msb = Wire.read();
   lsb = Wire.read();
  
   //Serial.print("READ : ");
   //Serial.println((int) msb<<8 | lsb);
   return (int) msb<<8 | lsb;
}

// Read the uncompensated temperature value
unsigned int bmp085ReadUT() {
   unsigned int ut;
  
   // Write 0x2E into Register 0xF4, This requests a temperature reading
   Wire.beginTransmission(BMP085_ADDRESS);
   Wire.write(0xF4);
   Wire.write(0x2E);
   Wire.endTransmission();
  
   // Wait at least 4.5ms
   delay(5);
  
   // Read two bytes from registers 0xF6 and 0xF7
   ut = bmp085ReadInt(0xF6);
   return ut;
}

// Read the uncompensated pressure value
unsigned long bmp085ReadUP() {
   unsigned char msb,lsb,xlsb;
   unsigned long up = 0;
  
   // Write 0x34+(OSS<<6) into register 0xF4
   // Request a pressure reading w/ oversampling setting
   Wire.beginTransmission(BMP085_ADDRESS);
   Wire.write(0xF4);
   Wire.write(0x34 + (0<<6));
   Wire.endTransmission();
  
   // Wait for conversion, delay time dependent on OSS
   delay(2 + (3<<0));
  
   // Read register 0xF6 (MSB), 0xF7 (LSB), and 0xF8 (XLSB)
   Wire.beginTransmission(BMP085_ADDRESS);
   Wire.write(0xF6);
   Wire.endTransmission();
   Wire.requestFrom(BMP085_ADDRESS,3);
  
   // Wait for data to become available
   while(Wire.available() < 3)
     ;
   msb = Wire.read();
   lsb = Wire.read();
   xlsb = Wire.read();
  
   up = (((unsigned long) msb << 16) | ((unsigned long) lsb << 8) | (unsigned long) xlsb) >> (8-0);
  
   return up;
}
