/*
   WebserverParserequest
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/index?dir=-1&power=63&f1=0&f2=1&f3=0&f4=1&f5=0&f6=1&f7=0&f8=1
   - parse request parameters and output to html
   
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
            
            // send a standard http response header
            client.println("HTTP/1.1 200 OK");
            client.println("Content-Type: text/html");
            client.println("Connection: close");   // the connection will be closed after completion of the response
            client.println();
            client.println("<!DOCTYPE HTML>");
            client.println("<html>");
            client.println("<br />HTTP REQUEST IS : "+req);
            // trim request string
            req = req.substring(req.indexOf("/"),req.lastIndexOf(" HTTP"));
            client.print("<br />CALLING URL : http://");
            client.print(_buf);
            client.println(req);
            // extract params
            fromReq("dir");
            client.println("<br /><br />DIR = "+par);
            fromReq("power");
            client.println("<br />POWER = "+par);
            fromReq("f1");
            client.println("<br />F1 = "+par);
            fromReq("f2");
            client.println("<br />F2 = "+par);
            fromReq("f3");
            client.println("<br />F3 = "+par);
            fromReq("f4");
            client.println("<br />F4 = "+par);
            fromReq("f5");
            client.println("<br />F5 = "+par);
            fromReq("f6");
            client.println("<br />F6 = "+par);
            fromReq("f7");
            client.println("<br />F7 = "+par);
            fromReq("f8");
            client.println("<br />F8 = "+par);
            client.println("</html>");

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

void fromReq(String param) {
  par = "";
  int ind = req.indexOf(param);
  if (ind==-1) return;
  int ind2 = 1+req.indexOf("=",ind);
  int ind3 = req.indexOf("&",ind);
  if (ind3==-1) ind3 = req.length();
  par = req.substring(ind2,ind3);
}
