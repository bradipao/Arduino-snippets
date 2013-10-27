Arduino-snippets
================

This repository contains snippets, photo and notes collected during development on Arduino platform, right from very first basic steps.

**BasicUartComm** : Basic program that receives single characters from UART-RX and echo them to UART-TX. This is a skeleton program used to build other more complex programs which need receiving command from user and providing logging.

**BasicI2c** : Based on the BasicUartComm skeleton, at present implements I2C interface setup through Wire library and initiate a transmission.

**I2C_adt7410** : Read temperature from I2C sensor ADT7410 and print result on UART. Init and read operations are UART controlled.

**BasicWebserver** : Hello-world webserver using Arduino UNO and Ethernet Shield. Board initializes serial port, then enables ethernet and wait for DHCP-assigned IP. Once IP is obtained, it is forwarded to Serial output. Opening IP in a browser, the basic arduino web server provides the Hello World page.
