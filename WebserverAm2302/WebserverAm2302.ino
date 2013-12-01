/*
   WebserverAm2302
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/cantuina
   - read DHT22 am2302 sensor with DHTlib http://playground.arduino.cc/Main/DHTLib
   - answer in json format
   
   v.20131201
      - added define UARTDEBUG to enable/disable Serial debug output
      - wait ethernet forevere on setup()
      - hwid is now the MAC address, for multi-arduino support on the same network
      - time from millis() info added
 */

// include
#include <SPI.h>
#include <Ethernet.h>
#include <dht.h>

// debug ON-OFF
#define UARTDEBUG 1

// define
#define DHT22_PIN 6

// globals
unsigned long time,delta;
char cmd;
char _buf[50];
int ethok=0;
//byte mac[] = { 0x00,0xAA,0xBB,0xCC,0xDE,0x02 };
byte mac[] = { 0xDE,0xAA,0xBB,0xCC,0xDD,0x01 };
IPAddress ip;
dht DHT;

boolean isreqline = true;
String req = String();
String par = String();

// Ethernet server
EthernetServer server(80);

// the setup routine runs once when you press reset:
void setup() {
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
      if (UARTDEBUG) Serial.println("-- DHT22 checksum error"); 
      break;
    case DHTLIB_ERROR_TIMEOUT: 
      if (UARTDEBUG) Serial.println("-- DHT22 time out error");
      break;
    default: 
      if (UARTDEBUG) Serial.println("-- DHT22 unknown error"); 
      break;
  }
  
  // ethernet setup (using DHCP can be blocking for 60 seconds)
  while (ethok==0) restartEthernetShield();

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
 
  // web server
  if (ip[0]!=0) {
    EthernetClient client = server.available();   // listen to client connecting
    if (client) {
      // new client http request
      time = millis();
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
            if (req.indexOf("cantuina")==-1) send_404(client);
            // else send JSON response
            else {
              // start sensor reading
              DHT.read22(DHT22_PIN);
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
              client.print(time);
              client.print("\"");
              // temperature
              client.print(",\"temp\":\"");
              client.print(DHT.temperature,1);
              client.print("\"");
              // humidity
              client.print(",\"humi\":\"");
              client.print(DHT.humidity,1);
              client.print("\"");
              // pressure
              client.print(",\"pres\":\"");
              client.print(0);
              client.print("\"");
              // close json
              client.println("}");
              // serial logging
              if (UARTDEBUG) {
                delta = millis() - time;
                Serial.print("-- ETHERNET req / time: ");
                Serial.print(time);
                Serial.print("+");
                Serial.print(delta);
                Serial.print(" / temp: ");
                Serial.print(DHT.temperature,1);
                Serial.print(" C / humi: ");
                Serial.print(DHT.humidity,1);
                Serial.println(" %");
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
      // give the web browser time to receive the data
      delay(1);
      // close the connection:
      client.stop();
    }
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
