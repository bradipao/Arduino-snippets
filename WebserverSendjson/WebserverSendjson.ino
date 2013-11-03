/*
   WebserverSendjson
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/cantuina
   - answer in json format
   
 */

// include
#include <SPI.h>
#include <Ethernet.h>

// globals
char cmd;
char _buf[50];
int ethok=0;
byte mac[] = { 0x00,0xAA,0xBB,0xCC,0xDE,0x02 };
IPAddress ip;

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
  Serial.println("-- UART READY");
  
  // ethernet setup (using DHCP can be blocking for 60 seconds)
  ethok = Ethernet.begin(mac);
  if (ethok==0) Serial.println("ERROR : failed to configure Ethernet");
  else {
    ip = Ethernet.localIP();
    formatIP(ip);
    Serial.print("-- IP ADDRESS is ");
    Serial.println(_buf);
  }
}

// loop routine
void loop() {
 
  // web server
  if (ip[0]!=0) {
    EthernetClient client = server.available();   // listen to client connecting
    if (client) {
      Serial.println("ETHERNET : new client http request");
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
              client.println("HTTP/1.1 200 OK");
              client.println("Content-Type: application/json");  // JSON response type
              client.println("Connection: close");               // close connection after response
              client.println();
              // open JSON
              client.print("{");
              // result
              client.print("\"res\":\"OK\"");
              // temperature
              client.print(",\"temp\":\"");
              client.print(15.5+random(0,20));
              client.print("\"");
              // pressure
              client.print(",\"pres\":\"");
              client.print(980+random(0,50));
              client.print("\"");
              //humidity
              client.print(",\"humi\":\"");
              client.print(25+random(0,50));
              client.print("\"");
              // close json
              client.println("}");              
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

static void send_404(EthernetClient client) {
  client.println("HTTP/1.1 404 Not Found");
  client.println("Content-Type: text/html");
  client.println();
  client.println("404 NOT FOUND");
}
