/*
   I2C_adt7410
   
   interface ADT7410 temperature sensor through I2C and output on UART
    - pin A4 : SDA
    - pin A5 : SCL
    
   RAW 16-bit, signed data from ADT7410.
   (tempReading / 128) for positive temps in Celcius.
   ((tempReading - 65536) / 128) for negative temps in Celcius.
   
   based on : http://forum.arduino.cc/index.php/topic,22066.0.html
   
 */

// include
#include <SPI.h>
#include <Wire.h>

#define ADT7410address 0x48    // A0=gnd A1=gnd
#define ADT7410tempreg 0x00
#define ADT7410confreg 0x03

// vars
char cmd;
byte res;
char buf[3];
String sReportCmd = "CMD : received command #";
int tempReading = 0;
float temperature = 0;

// initialization of the ADT7410 sets the configuration register, 16-bit resolution selected.
void ADT7410INIT() {
  Wire.beginTransmission(ADT7410address);
  Wire.write(ADT7410confreg);
  Wire.write(B10000000); 
  Wire.endTransmission();
}

// read temperature register
void ADT7410READ() {
  byte MSB;
  byte LSB;
  // send request for temperature register
  Wire.beginTransmission(ADT7410address);
  Wire.write(ADT7410tempreg);
  Wire.endTransmission();
  // listen for and acquire 16-bit register address.
  Wire.requestFrom(ADT7410address,2);
  MSB = Wire.read();
  LSB = Wire.read();
  // assign global 'tempReading' the 16-bit signed value.
  tempReading = ((MSB << 8) | LSB);
  temperature = (float)tempReading/128;
}

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
    
    //----CMD-0------------------------------------------------------------
    if (cmd=='0') {
        Serial.println("Initializing Temperature sensor");
        Wire.beginTransmission(ADT7410address);   // transmit to adt7410
        Wire.write(ADT7410confreg);
        Wire.write(B10000000); 
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
    else if (cmd=='1') {
        ADT7410READ();  // read temperature from adt7410
        Serial.print("Temperature read : ");
        Serial.println(temperature);
    }
    
    else Serial.println(sReportCmd+cmd);
      
  }  // end : while (Serial.available()>0)

} // end : loop()

