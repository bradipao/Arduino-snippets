/*
   WebserverAm2302_v3
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/podval
   - send history on http://aaa.bbb.ccc.ddd/podval?hist=1
   - read four DHT22 am2302 sensor with DHTlib http://playground.arduino.cc/Main/DHTLib
   - answer in json format

 */

// include
#include <SPI.h>
#include <Ethernet.h>
#include <dht.h>

// defines
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

// history data
int kk;
int idx = 0;
int idprev = 0;
int t1,h1,t2,h2,t3,h3,t4,h4;
int t1h[48],h1h[48];
int t2h[48],h2h[48];
int t3h[48],h3h[48];
int t4h[48],h4h[48];
unsigned long histime[48];

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
  
   // DHT22 test
   checkDHT22(DHT22_PIN1);
   checkDHT22(DHT22_PIN2);
   checkDHT22(DHT22_PIN3);
   checkDHT22(DHT22_PIN4);
   
   // ethernet setup (using DHCP can be blocking for 60 seconds)
   while ((ethok==0)&&(ip[0]==0))
      restartEthernetShield();
      
   // preset history
   read_sensors();
   fill_history();
   
   // free ram
   Serial.print(F("FREERAM : "));
   Serial.println(freeRam());

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
      /*
      DHT.read22(DHT22_PIN1);
      t1h[idx] = 10*DHT.temperature;
      h1h[idx] = 10*DHT.humidity;
      DHT.read22(DHT22_PIN2);
      t2h[idx] = 10*DHT.temperature;
      h2h[idx] = 10*DHT.humidity;
      DHT.read22(DHT22_PIN3);
      t3h[idx] = 10*DHT.temperature;
      h3h[idx] = 10*DHT.humidity;
      DHT.read22(DHT22_PIN4);
      t4h[idx] = 10*DHT.temperature;
      h4h[idx] = 10*DHT.humidity;
      */
      histime[idx] = tnow;
      idprev = idx;
      idx = (idx+1) % 48;
      thist = tnow;
      tcheck = tnow;
      // serial logging
      Serial.print(F("HIS time:"));
      Serial.print(tnow);
      Serial.print(F(" / idx:"));
      Serial.print(idprev);
      Serial.print(F(" / t1h:"));
      Serial.print(t1,1);
      Serial.print(F(" / h1h:"));
      Serial.print(h1,1);
      Serial.print(F(" / t2h:"));
      Serial.print(t2,1);
      Serial.print(F(" / h2h:"));
      Serial.print(h2,1);
      Serial.print(F(" / t3h:"));
      Serial.print(t3,1);
      Serial.print(F(" / h3h:"));
      Serial.print(h3,1);
      Serial.print(F(" / t4h:"));
      Serial.print(t4,1);
      Serial.print(F(" / h4h:"));
      Serial.println(h4,1);
   }
   
   // CHECK FOR ERROR IN LAST SAMPLED DATA
   else if ((tnow - tcheck)>=CHECKTIME) {
      // if last history has errors, resample and update
      if ((t1h[idprev]<=-999) || (h1h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN1);
         t1h[idprev] = 10*DHT.temperature;
         h1h[idprev] = 10*DHT.humidity;
         Serial.println(F("FIX t1h,h1h"));
      }
      if ((t2h[idprev]<=-999) || (h2h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN2);
         t2h[idprev] = 10*DHT.temperature;
         h2h[idprev] = 10*DHT.humidity;
         Serial.println(F("FIX t2h,h2h"));
      }
      if ((t3h[idprev]<=-999) || (h3h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN3);
         t3h[idprev] = 10*DHT.temperature;
         h3h[idprev] = 10*DHT.humidity;
         Serial.println(F("FIX t3h,h3h"));
      }
      if ((t4h[idprev]<=-999) || (h4h[idprev]<=-999)) {
         DHT.read22(DHT22_PIN4);
         t4h[idprev] = 10*DHT.temperature;
         h4h[idprev] = 10*DHT.humidity;
         Serial.println(F("FIX t4h,h4h"));
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
               
               Serial.print(F("HTTP "));
               req += _buf;
               Serial.println(req);
               
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
                  // send out circular buffer 1 starting from oldest (idx)
                  client.print(F(",\"hist\":["));
                  for (kk=0;kk<48;kk++) {
                     // start json array item
                     if (kk!=0) client.print(F(",{"));
                     else client.print(F("{"));
                     // time
                     client.print(F("\"time\":\""));
                     client.print(histime[(kk+idx)%48]);
                     client.print(F("\""));
                     // temperature 1
                     client.print(F(",\"t1\":\""));
                     client.print(t1h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // humidity 1
                     client.print(F(",\"h1\":\""));
                     client.print(h1h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // temperature 2
                     client.print(F(",\"t2\":\""));
                     client.print(t2h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // humidity 2
                     client.print(F(",\"h2\":\""));
                     client.print(h2h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // temperature 3
                     client.print(F(",\"t3\":\""));
                     client.print(t3h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // humidity 3
                     client.print(F(",\"h3\":\""));
                     client.print(h3h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // temperature 4
                     client.print(F(",\"t4\":\""));
                     client.print(t4h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // humidity 4
                     client.print(F(",\"h4\":\""));
                     client.print(h4h[(kk+idx)%48],1);
                     client.print(F("\""));
                     // end json array item
                     client.print(F("}"));
                  }
                  client.print(F("]"));
                  // close json
                  client.println(F("}"));
                  // serial logging
                  // Serial.print(F("ETH time: "));
                  // Serial.println(tnow);
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
                  // close json
                  client.println(F("}"));
                  // serial logging
                  Serial.print(F("ETH time:"));
                  Serial.print(tnow);
                  Serial.print(F(" / t1:"));
                  Serial.print(t1,1);
                  Serial.print(F(" h1:"));
                  Serial.print(h1,1);
                  Serial.print(F(" / t2:"));
                  Serial.print(t2,1);
                  Serial.print(F(" h2:"));
                  Serial.print(h2,1);
                  Serial.print(F(" / t3:"));
                  Serial.print(t3,1);
                  Serial.print(F(" h3:"));
                  Serial.print(h3,1);
                  Serial.print(F(" / t4:"));
                  Serial.print(t4,1);
                  Serial.print(F(" h4:"));
                  Serial.println(h4,1);

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
   for (kk=0;kk<48;kk++) {
      t1h[kk] = t1;
      h1h[kk] = h1;
      t2h[kk] = t2;
      h2h[kk] = h2;
      t3h[kk] = t3;
      h3h[kk] = h3;
      t4h[kk] = t4;
      h4h[kk] = h4;
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
