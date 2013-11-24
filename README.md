Arduino-snippets
================

This repository contains snippets, photo and notes collected during development on Arduino platform, right from very first basic steps.

**BasicUartComm** : Basic program that receives single characters from UART-RX and echo them to UART-TX. This is a skeleton program used to build other more complex programs which need receiving command from user and providing logging.

**BasicI2c** : Based on the BasicUartComm skeleton, at present implements I2C interface setup through Wire library and initiate a transmission.

**I2C_adt7410** : Read temperature from I2C sensor ADT7410 and print result on UART. Init and read operations are UART controlled.

**BasicWebserver** : Hello-world webserver using Arduino UNO and Ethernet Shield. Board initializes serial port, then enables ethernet and wait for DHCP-assigned IP. Once IP is obtained, it is forwarded to Serial output. Opening IP in a browser, the basic arduino web server provides the Hello World page.

**WebserverParserequest** : Arduino webserver, with DHCP assigned IP provided through serial output. Parse incoming requests of a given format and extract params. This example is valid for specific parameters, but can be easily adapted.

**WebserverSendjson** : An Android app with network support that connects to Arduino+Ethernet board programmed with basic webserver that provides real-time data in JSON format.

  * Youtube demo : [http://youtu.be/0ggJLiD6sDA](http://youtu.be/0ggJLiD6sDA)
  * Wiki page : [https://github.com/bradipao/Arduino-snippets/wiki/WebserverSendjson](https://github.com/bradipao/Arduino-snippets/wiki/WebserverSendjson)
  
**WebserverHttpgetJsonres** : Arduino webserver, with DHCP assigned IP provided through serial output. Parse incoming requests of a given format, extract params and update internal status of params. Response is always the whole internal status in JSON format. Serial output is used basically for debug and demonstration.

**WebserverAm2302** : Arduino+Ethernet board that interfaces with AM2302(DHT22) humidity+temperature sensor and that implements basic webserver that provides real-time temperature and humidity data in JSON format.

Cantuina android app version 0.2 (and source code) that shows temperature and humidity with line graph.

  * Youtube demo : [http://youtu.be/NNndYjs97w4](http://youtu.be/NNndYjs97w4)