# BicycleKaraoke

[![Build Status](https://travis-ci.org/mdicke2s/BicycleKaraoke.svg?branch=master)](https://travis-ci.org/mdicke2s/BicycleKaraoke)
**Project status : released** 

=================================

## Abstract
Goal of this project is to develop a Karaoke machine as a challenging game for a station at http://ironscout.de/. The user will ride on a (fix mounted) bicycle and he is supposed to adjust riding speed, since music and lyrics playback will only work correctly if the user hits a specified range of rotational frequencies. (In the current implementation, the visibility of the karaoke is dependent on the users speed. Audio playback is untouched.)

This repository holds an Android application which includes:
- Measurement of rotation frequency
- Video playback
- Audio playback
- User preferences

## Requirements
- Android device with microphone
- Adapter that provides headphone and microphone jack separately (Android phone jack comes with CTIA pinout order (LRGM))
- Sensor from a usual tacho

## Setup instructions
- Install the apk to your device
- Make your own karaoke video and put it into a folder called **BicycleKaraokeVideo** (on any storage that is available for media index on your device)
- Get a phone jack cable and molder the wires directly to the tacho sensor
- Attach sensor and magnet on your bike
- Start app and have fun :-)

## Troubleshooting
Due to low resistance on the microphone connector, your device may not detect that there is a microphone connected. Therefore you may use the following app and set the property **Media audio** to **Wired headset (with mic)**
https://play.google.com/store/apps/details?id=com.woodslink.android.wiredheadphoneroutingfix
