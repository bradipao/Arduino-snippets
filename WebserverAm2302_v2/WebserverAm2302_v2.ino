/*
   WebserverAm2302_v2
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/cantuina
   - sned history on http://aaa.bbb.ccc.ddd/cantuina?hist=1
   - read DHT22 am2302 sensor with DHTlib http://playground.arduino.cc/Main/DHTLib
   - answer in json format

 */

// include
#include <SPI.h>
#include <Ethernet.h>
#include <dht.h>

// defines
#define UARTDEBUG            1  // to enable and disable debug on UART
#define DHT22_PIN            6  // DHT22 connected on pin 6
#define DHT22_MINTIME     2000  // minimum ms between DHT22 readings
#define SAMPLETIME     1800000  // sampling interval 30m*60s*1000ms = 1800000 ms
#define CHECKTIME        60000  // sampling interval     60s*1000ms =   60000 ms

// increment last byte of MAC for multi-arduino in the same subnet
byte mac[] = { 0xDE,0xAA,0xBB,0xCC,0xDD,0x01 };

// globals
unsigned long tnow,thist,tcheck,delta;
char cmd;
char _buf[50];
int ethok=0;
dht DHT;
boolean isreqline = true;
boolean iscantuina,ishistory;
String req = String();
String scantuina = String("cantuina");
String shist = String("hist");
IPAddress ip(0,0,0,0);
EthernetServer server(80);

// history data
int kk;
int idx = 0;
int idprev = 0;
double temp[48];
double humi[48];
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
   if (UARTDEBUG) Serial.println("-- UART READY");
  
   // DHT22 test
   int chk = DHT.read22(DHT22_PIN);
   switch (chk) {
      case DHTLIB_OK:  
         if (UARTDEBUG) Serial.println("-- DHT22 READY"); 
         break;
      case DHTLIB_ERROR_CHECKSUM: 
         if (UARTDEBUG) Serial.println("-- ERROR : DHT22 checksum"); 
         break;
      case DHTLIB_ERROR_TIMEOUT: 
         if (UARTDEBUG) Serial.println("-- ERROR : DHT22 time out");
         break;
      default: 
         if (UARTDEBUG) Serial.println("-- ERROR : DHT22 unknown"); 
         break;
   }
  
   // ethernet setup (using DHCP can be blocking for 60 seconds)
   while ((ethok==0)&&(ip[0]==0))
      restartEthernetShield();
}

void restartEthernetShield() {  
  delay(100);
  if (UARTDEBUG) Serial.println("-- RESTART ETHERNET SHIELD");
  ethok = Ethernet.begin(mac);
  if (ethok==0) {
    if (UARTDEBUG) Serial.println("-- ERROR : failed to configure Ethernet");
  } else {
    // extract ip
    ip = Ethernet.localIP();
    // start server
    server.begin();
    // start listening for clients
    if (UARTDEBUG) {
      formatIP(ip);
      Serial.print("-- IP ADDRESS is ");
      Serial.println(_buf);
    }
  }
  delay(100);
}

// loop routine
void loop() {
 
   // SAMPLING DATA and storing history at SAMPLETIME rate
   tnow = millis();
   if ((tnow - thist)>=SAMPLETIME) {
      // TODO: don't read if just read by http req
      DHT.read22(DHT22_PIN);
      // store in circular buffers
      temp[idx] = DHT.temperature;
      humi[idx] = DHT.humidity;
      histime[idx] = tnow;
      idprev = idx;
      idx = (idx+1) % 48;
      thist = tnow;
      tcheck = tnow;
      // serial logging
      if (UARTDEBUG) {
         Serial.print("-- SCHEDULED acq time: ");
         Serial.print(tnow);
         Serial.print(" / index: ");
         Serial.print(idx);
         Serial.print(" / temp: ");
         Serial.print(DHT.temperature,1);
         Serial.print(" C / humi: ");
         Serial.print(DHT.humidity,1);
         Serial.println(" %");
      }
   }
   // CHECK SAMPLED DATA
   else if ((tnow - tcheck)>=CHECKTIME) {
      // if last history has errors, resample and update
      if ((temp[idprev]==-999) || (temp[idprev]==-999)) {
         DHT.read22(DHT22_PIN);
         // store in circular buffers
         temp[idprev] = DHT.temperature;
         humi[idprev] = DHT.humidity;
         histime[idprev] = tnow;
         // serial logging
         if (UARTDEBUG) {
            Serial.print("-- CHECK acq time: ");
            Serial.print(tnow);
            Serial.print(" / index: ");
            Serial.print(idx);
            Serial.print(" / temp: ");
            Serial.print(DHT.temperature,1);
            Serial.print(" C / humi: ");
            Serial.print(DHT.humidity,1);
            Serial.println(" %");
         }
      }      
      tcheck = tnow;
   }

   // WEB SERVER
   EthernetClient client = server.available();   // listen to client connecting
   if (client) {
      // new client http request
      boolean currentLineIsBlank = true;   // an http request ends with a blank line
      while (client.connected()) {
         if (client.available()) {
            // read char from client
            char c = client.read();
            // append to request string
            if ((isreqline)&&(req.length()<127)) req += c; 
            // stop parsing after first line
            if (c=='\n') isreqline = false;

            // if you've gotten to the end of the line (received a newline character) and the line is blank,
            // the http request has ended, so you can send a reply
            if ((c=='\n') && currentLineIsBlank) {
               
               // if request does not contain "cantuina" keyword send 404
               if (req.indexOf(scantuina)==-1) {
                  send_404(client);
               }

               // else send JSON history if requested by "hist=1" param
               else if (req.indexOf(shist)!=-1) {
                  // response header
                  client.println("HTTP/1.1 200 OK");
                  client.println("Content-Type: application/json");
                  client.println("Connection: close");
                  client.println();
                  // open JSON
                  client.print("{");
                  // result header
                  client.print("\"res\":\"OK\"");
                  formatMAC();
                  client.print(",\"hwid\":\"");
                  client.print(_buf);
                  client.print("\"");
                  // time
                  client.print(",\"time\":\"");
                  client.print(tnow);
                  client.print("\"");
                  // send out circular buffer starting from oldest (idx)
                  client.print(",\"history\":[");
                  for (kk=0;kk<48;kk++) {
                     // start json array item
                     if (kk!=0) client.print(",{");
                     else client.print("{");
                     // time
                     client.print("\"time\":\"");
                     client.print(histime[(kk+idx)%48]);
                     client.print("\"");
                     // temperature
                     client.print(",\"temp\":\"");
                     client.print(temp[(kk+idx)%48],1);
                     client.print("\"");
                     // humidity
                     client.print(",\"humi\":\"");
                     client.print(humi[(kk+idx)%48],1);
                     client.print("\"");
                     // end json array item
                     client.print("}");
                  }
                  client.print("]");
                  // close json
                  client.println("}");
                  // serial logging
                  if (UARTDEBUG) {
                     Serial.print("-- ETHERNET req HISTORY / time: ");
                     Serial.println(tnow);
                     Serial.println(req);
                  }

               }

               // else send JSON response with instant data
               else {
                  // start sensor reading if enough time elapsed since last read
                  if ((tnow - thist)>DHT22_MINTIME) DHT.read22(DHT22_PIN);
                  // response header
                  client.println("HTTP/1.1 200 OK");
                  client.println("Content-Type: application/json");  // JSON response type
                  client.println("Connection: close");               // close connection after response
                  client.println();
                  // open JSON
                  client.print("{");
                  // result header
                  client.print("\"res\":\"OK\"");
                  formatMAC();
                  client.print(",\"hwid\":\"");
                  client.print(_buf);
                  client.print("\"");
                  // time
                  client.print(",\"time\":\"");
                  client.print(tnow);
                  client.print("\"");
                  // temperature
                  client.print(",\"temp\":\"");
                  client.print(DHT.temperature,1);
                  client.print("\"");
                  // humidity
                  client.print(",\"humi\":\"");
                  client.print(DHT.humidity,1);
                  client.print("\"");
                  // close json
                  client.println("}");
                  // serial logging
                  if (UARTDEBUG) {
                     Serial.print("-- ETHERNET req / time: ");
                     Serial.print(tnow);
                     Serial.print(" / temp: ");
                     Serial.print(DHT.temperature,1);
                     Serial.print(" C / humi: ");
                     Serial.print(DHT.humidity,1);
                     Serial.println(" %");
                     Serial.println(req);
                  }
               }

               // prepare for next request
               req = "";
               isreqline = true;
               // exit
               break;
            }
            if (c == '\n') {
               // you're starting a new line
               currentLineIsBlank = true;
            } 
            else if (c != '\r') {
               // you've gotten a character on the current line
               currentLineIsBlank = false;
            }
         }
      }
      // give the web browser time to receive the data and close the connection
      delay(1);
      client.stop();
   }   

}

void formatIP(IPAddress ii) {
   sprintf(_buf,"%d.%d.%d.%d",ii[0],ii[1],ii[2],ii[3]);
}

void formatMAC() {
   sprintf(_buf,"%02x:%02x:%02x:%02x:%02x:%02x",mac[0],mac[1],mac[2],mac[3],mac[4],mac[5]);
}

static void send_404(EthernetClient client) {
   client.println("HTTP/1.1 404 Not Found");
   client.println("Content-Type: text/html");
   client.println();
   client.println("404 NOT FOUND");
}
