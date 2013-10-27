/*
   BasicWebserver
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request and return hello world
   
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
    Serial.println(_buf);
  }
}

// loop routine
void loop() {
 
  // web server
  if (ip[0]!=0) {
    EthernetClient client = server.available();   // listen to client connecting
    if (client) {
      Serial.println("ETHERNET : new web client");
      boolean currentLineIsBlank = true;   // an http request ends with a blank line
      while (client.connected()) {
        if (client.available()) {
          char c = client.read();
          Serial.write(c);
          // if you've gotten to the end of the line (received a newline character) and the line is blank,
          // the http request has ended, so you can send a reply
          if (c == '\n' && currentLineIsBlank) {
            // send a standard http response header
            client.println("HTTP/1.1 200 OK");
            client.println("Content-Type: text/html");
            client.println("Connection: close");   // the connection will be closed after completion of the response
  	    //client.println("Refresh: 5");          // refresh the page automatically every 5 sec
            client.println();
            client.println("<!DOCTYPE HTML>");
            client.println("<html>");
            client.println("Hello world, this is an Arduino web server !!!");
            client.println("</html>");
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
      Serial.println("ETHERNET : client disonnected");
    }
  }
}

void formatIP(IPAddress ii) {
  sprintf(_buf,"-- IP ADDRESS is %d.%d.%d.%d",ii[0],ii[1],ii[2],ii[3]);
}

