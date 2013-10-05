/*
   BasicI2c
   
   basic I2C tests on UART command
    - pin A4 : SDA
    - pin A5 : SCL
    
   first word : AAAAAAAxx
                MSB-LSB
 */

// include
#include <SPI.h>
#include <Wire.h>

// vars
char cmd;
byte res;
char buf[3];
String sReportCmd = "CMD : received command #";


// the setup routine runs once when you press reset:
void setup() {
  // join I2C bus
  Wire.begin();
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
        Wire.beginTransmission(65);   // transmit to device N
        Wire.write(byte(0x55));       // sends generic value byte
        res = Wire.endTransmission(); // end transmission
        itoa(res,buf,10);
        Serial.print("Wire.endTransmission() returned : ");
        Serial.print(buf);
        if (res==0x00) Serial.println(" success");
        if (res==0x01) Serial.println(" data too long");
        if (res==0x02) Serial.println(" received NACK on address");
        if (res==0x03) Serial.println(" received NACK on data");
        if (res==0x04) Serial.println(" other error");
    }

    //----CMD-1------------------------------------------------------------
    if (cmd=='1') {
        Wire.requestFrom(4,1);     // request 6 bytes from slave device #4
        while(Wire.available()) { 
          char c = Wire.read();    // receive a byte as character
        }
    }  
      
  }  // end : while (Serial.available()>0)

} // end : loop()

