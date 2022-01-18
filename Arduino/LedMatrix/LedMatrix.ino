/*
   ESP-12E
*/

#include <ESP8266WiFi.h>
#include <server.h>

#include "SdData.h"
SdData sdData;
#include "Program.h"
#include "FastLED.h"
Connection connection;
RGB_ledsController led;
Program program;

#include <Arduino_JSON.h>

void setup() {
  Serial.begin (115200);

  setupDeviceInfoFile();
  setupCreateResDir();
  setupMainProgramFile();
  connection.setup(sdData);
  led.setup(sdData);
  program.setup(sdData);
  
  //Serial.println(sdData.write("deviceConfig/apSetting.txt", "{\"ssid\":\"DukeAp\",\"passward\":\"ddd54679\"}"));
  //DukeAp ddd54679
  delay(3000);

  Serial.println(sdData.getHardwareType());
  //Serial.println(sdData.getSize());
  Serial.println(SPI_FLASH_SEC_SIZE);

  Serial.println(program.readMainProgramContent());
}

void loop() {
  //connectionProcessCycle
  connection.processCycle();
  
  WiFiClient client = connection.getClient();
  if (client) {
    //get clietIp
    IPAddress clientIPAdress = client.remoteIP();
    String clientIp = String(clientIPAdress[0]) + String(".")
                      + String(clientIPAdress[1]) + String(".")
                      + String(clientIPAdress[2]) + String(".")
                      + String(clientIPAdress[3]);


    Serial.print(clientIp);
    Serial.println(" connected");
    while (client.connected()) {
      //read form client data process
      String dataFromClient = "";
      while (client.available()) {
        char ch = client.read();//static_cast<char>(client.read())
        dataFromClient += ch;
      }
      if (dataFromClient != "") {
        Serial.println(dataFromClient);
        
        //update LED
        //-------------------------

        //data process, send to client data feedback
        String temp = sdData.appDataProcess(dataFromClient);
        client.write(temp.c_str());
        Serial.println("FB OK");
        client.println();
        client.flush();

        program.readMainProgramContent();
      }

      //Serial send msg to client
      bool needFlush = false;
      while (Serial.available()) {
        char writeData = Serial.read();//static_cast<char>(Serial.read())
        client.write(writeData);
        Serial.println(writeData);
        needFlush = true;
      }
      if (needFlush) {
        client.println();
        client.flush();
      }

      program.processCycle();
    }
    Serial.print(clientIp);
    Serial.println(" disconnected");
  }

  //ledCycle
  program.processCycle();
}

/************************
   setup system methods
 ************************/

void setupCreateResDir(){
  Serial.println("setupCreateResDir...");
  Serial.println(sdData.makeDir("deviceConfig"));
  Serial.println(sdData.makeDir("resource"));
  Serial.println(sdData.makeDir("resource/LEDs"));
  Serial.println(sdData.makeDir("resource/program"));
  Serial.println("...End");
}

void setupDeviceInfoFile(){
  Serial.println("setupDeviceInfoFile...");
  String path = "deviceConfig/deviceInfo.txt";
  String src = sdData.read(path);
  JSONVar objSrc;
  if(src==""){//default setting
    objSrc["types"] = {"ledMatrix"};//"deviceType"
    objSrc["ledMatrix"] = "ledNums";//"deviceType"
    objSrc["ledNums"] = 0;
    Serial.println(sdData.write(path,JSON.stringify(objSrc)));
  }
  Serial.println("...End");
}

void setupMainProgramFile(){
  Serial.println("setupMainProgramFile...");
  String path = "deviceConfig/mainProgram.txt";
  String src = sdData.read(path);
  if(src==""){
    Serial.println(sdData.write(path,""));
  }
  Serial.println("...End");
}

//program auto call
bool Program::readLedFile(String data, int ledOffset){
  Serial.print("ledOffset = ");
  Serial.println(ledOffset);

  JSONVar ledsData = JSON.parse(data);
  Serial.print("ledsData = ");
  Serial.print(data);
  Serial.print(", length = ");
  Serial.println(ledsData.length());

  
  CRGB* leds = led.getCRGB();
  
  for (int i=0; i<ledsData.length(); i++){
    int num = ledsData[i]["num"];
    byte colorR = (int)ledsData[i]["colorR"];
    byte colorG = (int)ledsData[i]["colorG"];
    byte colorB = (int)ledsData[i]["colorB"];
    if(colorR>10)colorR = 10;
    if(colorG>10)colorR = 10;
    if(colorB>10)colorR = 10;
    leds[num+ledOffset] = CRGB(colorR, colorG, colorB);
  }
  led.set(leds);
  return true;
}
