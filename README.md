CCNPingAndroid
==============

A naive ping server client application for Android and CCNx

### Prerequisites ###
* CCNx apps on Android should be installed and running
* Android SDK with ant build

### Configuration ###
* Edit first three lines of the Makefile with the locations of the jar files CCNx Android lib, CCNx Java source and Bouncy Castle (generally found in libs for CCNx Android Services app)
* Verify file names of jars in the Makefile
* Run
	1. make setup
	1. make
* Install apk on device using adb install
* Alternatively, use run.sh to install on all connected devices

### Usage ###
* Ensure that CCNx Services are running on the device
* Open app on device
* Enter prefix
* Start server or client

This ping app was made to test NDNBlue and is compatible with ccnping on desktops.