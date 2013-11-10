/*
   WebserverHttpgetJsonres
   
   - connect to ethernet in DHCP mode
   - send IP on serial output
   - wait for incoming http request with format http://aaa.bbb.ccc.ddd/droittle
   - parse "int" commands from predefined set : loco,power,dir,f1,f2,f3,f4,f5,f6,f7,f8
   - answer command status in json format
   
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
// command status
int tmp,loco,power,dir,f1,f2,f3,f4,f5,f6,f7,f8;

boolean isreqline = true;
String req = String();
String par = String();

// Ethernet server
EthernetServer server(80);

// the setup routine runs once when you press reset:
void setup() {
  // init command status
  tmp=-1;       loco=0;       dir=0;        power = 0;
  f1=0;  f2=0;  f3=0;  f4=0;  f5=0;  f6=0;  f7=0;  f8=0;
  
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
            if (req.indexOf("droittle")==-1) send_404(client);
            // else apply command and send JSON response
            else {
              // trim request and logging url
              req = req.substring(req.indexOf("/"),req.lastIndexOf(" HTTP"));
              Serial.println(req);
              // parsing request, update commands only if detected
              tmp = parseReq("loco");
              if (tmp!=-1) loco = tmp;
              tmp = parseReq("dir");
              if (tmp!=-1) dir = tmp;
              tmp = parseReq("power");
              if (tmp!=-1) power = tmp;
              tmp = parseReq("f1");
              if (tmp!=-1) f1 = tmp;
              tmp = parseReq("f2");
              if (tmp!=-1) f2 = tmp;
              tmp = parseReq("f3");
              if (tmp!=-1) f3 = tmp;
              tmp = parseReq("f4");
              if (tmp!=-1) f4 = tmp;
              tmp = parseReq("f5");
              if (tmp!=-1) f5 = tmp;
              tmp = parseReq("f6");
              if (tmp!=-1) f6 = tmp;
              tmp = parseReq("f7");
              if (tmp!=-1) f7 = tmp;
              tmp = parseReq("f8");
              if (tmp!=-1) f8 = tmp;
              
              // send JSON response
              client.println("HTTP/1.1 200 OK");
              client.println("Content-Type: application/json");  // JSON response type
              client.println("Connection: close");               // close connection after response
              client.println();
              // open JSON
              client.print("{");
              // result
              client.print("\n\"res\":\"OK\"");
              client.print(",\n\"hwid\":\"aa55aa55\"");
              // loco
              client.print(",\n\"loco\":\"");     client.print(loco);     client.print("\"");
              client.print(",\n\"dir\":\"");      client.print(dir);      client.print("\"");
              client.print(",\n\"power\":\"");    client.print(power);    client.print("\"");
              client.print(",\n\"f1\":\"");       client.print(f1);       client.print("\"");
              client.print(",\n\"f2\":\"");       client.print(f2);       client.print("\"");
              client.print(",\n\"f3\":\"");       client.print(f3);       client.print("\"");
              client.print(",\n\"f4\":\"");       client.print(f4);       client.print("\"");
              client.print(",\n\"f5\":\"");       client.print(f5);       client.print("\"");
              client.print(",\n\"f6\":\"");       client.print(f6);       client.print("\"");
              client.print(",\n\"f7\":\"");       client.print(f7);       client.print("\"");
              client.print(",\n\"f8\":\"");       client.print(f8);       client.print("\"");
              // close json
              client.println("\n}");              
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

int parseReq(String param) {
  par = "";
  int ind = req.indexOf(param);
  if (ind==-1) return -1;
  int ind2 = 1+req.indexOf("=",ind);
  int ind3 = req.indexOf("&",ind);
  if (ind3==-1) ind3 = req.length();
  par = req.substring(ind2,ind3);
  return par.toInt();
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
