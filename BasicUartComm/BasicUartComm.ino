/*
   BasicUartComm
   
   basic communication through UART interface, receive/echo/execute commands
 */

// include
#include <SPI.h>

// vars
char cmd;
String sReportCmd = "CMD : received command #";


// the setup routine runs once when you press reset:
void setup() {                 
  // open serial communications and wait for port to open:
  Serial.begin(115200);
  while (!Serial) {
    ; // wait for serial port to connect
  }
  Serial.println("-- BOARD RESET --");
}


// loop routine
void loop() {
  
  // Serial debug management
  while (Serial.available()>0) {
    cmd = Serial.read();
    Serial.println(sReportCmd+cmd);
    
    //----CMD-0------------------------------------------------------------
    if (cmd=='0') {

    }

    //----CMD-1------------------------------------------------------------
    if (cmd=='1') {

    }  
      
  }  // end : while (Serial.available()>0)

} // end : loop()

