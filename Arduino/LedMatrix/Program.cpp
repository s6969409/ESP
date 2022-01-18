#include "Program.h"



//class TimerDelay implement***********************
TimerDelay::TimerDelay(){
  setTime = 0;
  startTime = 0;
}
//private........
//public........
void TimerDelay::start(int millisSec){
  if (setTime == 0){
    setTime = millisSec;
    startTime = millis();
    //Serial.print("delay millisSec = ");Serial.println(millisSec);
  }
}

void TimerDelay::reset(){
  setTime = 0;
}

bool TimerDelay::isOnTime(){
  if (millis() - startTime >= setTime){
    setTime = 0;
    return true;
  }
  return false;
}

//class Connection implement***********************
Connection::Connection(){

}

void Connection::setup(SdData sdD){
  sdData = sdD;
  server = new WiFiServer(23);//port defaut set 23
  connetionStatus = 0;
}

bool Connection::startConnectAp(){
  Serial.println("setupWifiServer...");
  //get ssid & passward from SD card
  String apSettingPath = "deviceConfig/apSetting.txt";
  String src = sdData.read(apSettingPath);
  JSONVar objSrc;
  if(src==""){//default setting
    objSrc["ssid"] = "DukeAp";//"defaultAP";"CHT5879"
    objSrc["passward"] = "ddd54679";//"00000000";"076995912"
    Serial.println(sdData.makeDir("deviceConfig"));
    Serial.println(sdData.write(apSettingPath,JSON.stringify(objSrc)));
  }else{
    objSrc = JSON.parse(src);
  }
  String ssid = (const char*)objSrc["ssid"];
  String password = (const char*)objSrc["passward"];

  // Connect to WiFi network
  Serial.print("Mac address: ");
  Serial.println(WiFi.macAddress());
  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  Serial.println("...End");
  return true;
}

bool Connection::connectingAp(){
  if (WiFi.status() == WL_CONNECTED){
    Serial.println("... connect ok!");
    return true;
  }
  static TimerDelay cnT;
  cnT.start(500);
  if (cnT.isOnTime()){
    Serial.println("connectingAp...");
  }
  return false;
}

bool Connection::connectedAp(){
  // Start the server
  server->begin();
  Serial.println("Server started");

  // Print the IP address
  Serial.println(WiFi.localIP());
  Serial.println("...End");
}

WiFiClient Connection::getClient(){
  return server->available();
}

void Connection::processCycle(){
  if (connetionStatus == 0 && startConnectAp()){
    connetionStatus = 1;
  }
  if (connetionStatus == 1 && connectingAp()){
    connetionStatus = 2;
  }
  if (connetionStatus == 2 && connectedAp()){
    connetionStatus = 3;
  }
}

//class RGB_ledsController implement***********************
RGB_ledsController::RGB_ledsController(){
  
}

void RGB_ledsController::setup(SdData sdD){
  Serial.println("setupFastLed...");
  sdData = sdD;
  
  String apSettingPath = "deviceConfig/deviceInfo.txt";
  String src = sdData.read(apSettingPath);
  JSONVar objSrc = JSON.parse(src);
  int ledNums = 8;//objSrc["ledMatrix"]["ledNums"];// 取得定義燈數

  const int pin = 0;//GPIO;定義燈板 Data in 控制腳位
  
  leds = new CRGB[ledNums];
  FastLED.addLeds<NEOPIXEL, pin>(leds, ledNums);  //--OK
  for (int i = 0; i < ledNums; i++) leds[i] = CRGB::Black; //燈暗
  FastLED.show();
  Serial.println("...End");
}

void RGB_ledsController::set(CRGB* ledDatas){
  leds = ledDatas;
  FastLED.show();
}

CRGB* RGB_ledsController::getCRGB(){
  return FastLED.leds();
}

//class Program implement***********************
Program::Program(){
  
}

//private........
bool Program::actionParse(JSONVar block){
  String type = (const char*)block["type"];

  if (type == "delay") {
    return delay((int)block["timeMillis"]);
  } 
  if (type == "readLedFile") {
    return readLedFile((const char*)block["ledsData"], (int)block["ledOffset"]);
  } 
  
}

bool Program::delay(int timeMillis){
  timerDelay.start(timeMillis);
  return timerDelay.isOnTime();
}

//bool Program::readLedFile(String filePath, int ledOffset){}
//-->at main to implement

//public........
void Program::setup(SdData sdD){
  sdData = sdD;
  blockIndex = 0;
  actionStop = false;
}

String Program::readMainProgramContent(){
  //read mainProgram
  String programPath = sdData.read(FolderPath::DEVICE_CONFIG_MAIN_PROGRAM);
  JSONVar blocks = JSON.parse(sdData.read(programPath));

  if (JSON.typeof(blocks) != "array") {
    Serial.print((String)JSON.typeof(blocks));
    Serial.println("!= array --> Parsing input failed!");
    actionStop = true;
    return "";
  }
  
  JSONVar objSrc;
  for (int index = 0; index < blocks.length(); index++){
    JSONVar block = blocks[index];
    String type = (const char*)block["type"];

    if (type == "delay") {
      objSrc[index] = block;
    } 
    if (type == "readLedFile") {
      String path = (const char*)block["filePath"];
      JSONVar readLed;
      readLed["type"] = (const char*)block["type"];
      readLed["ledsData"] = sdData.read(path);
      readLed["ledOffset"] = (int)block["ledOffset"];

      objSrc[index] = readLed;
    }
  }
  blockIndex = 0;
  timerDelay.start(0);
  actionStop = false;

  mainProgramContent = JSON.stringify(objSrc);
  return mainProgramContent;
}

void Program::processCycle(){
  if (actionStop)return;

  JSONVar blocks = JSON.parse(mainProgramContent);
  if (actionParse(blocks[blockIndex])){
    blockIndex++;
    if (blockIndex >= blocks.length()){
      blockIndex = 0;
      if (blocks.length() <= 1){
        actionStop = true;
      }
    }
  }
}
