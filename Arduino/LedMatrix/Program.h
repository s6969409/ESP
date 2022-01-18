#include <SPI.h>
#include "SdData.h"
#include "FastLED.h"
#include <ESP8266WiFi.h>
#include <server.h>

class TimerDelay{
private:
  int setTime;//millis second
  int startTime;
public:
  TimerDelay();
  void start(int millisSec);
  void reset();
  bool isOnTime();
};

class Connection{
private:
  int connetionStatus;
  SdData sdData;
  WiFiServer* server;
  bool startConnectAp();
  bool connectingAp();
  bool connectedAp();
public:
  Connection();
  void setup(SdData sdD);
  WiFiClient getClient();
  void processCycle();
  
};

class RGB_ledsController{
private:
  SdData sdData;
  CRGB* leds;
public:
  RGB_ledsController();
  void setup(SdData sdD);
  void set(CRGB* ledDatas);
  CRGB* getCRGB();
};

class Program{
private:
  SdData sdData;
  int blockIndex;
  String mainProgramContent;
  TimerDelay timerDelay;
  CRGB leds;
  bool actionStop;
  bool actionParse(JSONVar block);
  bool delay(int timeMillis);
  bool readLedFile(String data, int ledOffset);
public:
  Program();
  void setup(SdData sdD);
  String readMainProgramContent();

  void processCycle();
};
