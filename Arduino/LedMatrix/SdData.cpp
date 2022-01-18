#include "SdData.h"



//class CmdMode implement***********************
const String CmdMode::readFile = "readFile";
const String CmdMode::writeFile = "writeFile";
const String CmdMode::removeFile = "removeFile";
const String CmdMode::getFileList = "getFileList";
const String CmdMode::makeDir = "makeDir";
const String CmdMode::removeDir = "removeDir";

//class FolderPath implement***********************
const String FolderPath::DEVICE_CONFIG_AP_SETTING = "deviceConfig/apSetting.txt";
const String FolderPath::DEVICE_CONFIG_DEVICE_INFO = "deviceConfig/deviceInfo.txt";
const String FolderPath::DEVICE_CONFIG_MAIN_PROGRAM = "deviceConfig/mainProgram.txt";
const String FolderPath::RES_LEDS = "resource/LEDs";
const String FolderPath::RES_PROGRAM = "resource/program";

//class JsonKeys implement***********************
const String JsonKey::COMMAND_MODE = "commandMode";
const String JsonKey::FILE_PATH = "filePath";
const String JsonKey::DATA = "data";

//class SdData implement***********************
bool SdData::SDisBegin = false;
const size_t SdData::bufferSize = 100;//use for get file name data size

SdData::SdData(){
  if(SDisBegin){
    Serial.println();
    Serial.println("SD is Initialized!");
    return;
  }
  Serial.println();
  Serial.print("Initializing SD card...");

  while (!sdFat.begin(SS)) {//SS=GPIO15
    Serial.print("SD.begin(SS) = ");
    Serial.println(sdFat.begin(SS));
    Serial.println("initialization failed!");
    delay(1000);
  }
  SDisBegin = true;
  Serial.println("initialization done.");
}

String SdData::getHardwareType(){
  SdSpiCard card = *sdFat.card();
  switch (card.type()) {
    case SD_CARD_TYPE_SD1://1:
      return "SD1";
      break;
    case SD_CARD_TYPE_SD2://2:
      return "SD2";
      break;
    case SD_CARD_TYPE_SDHC://3:
      return "SDHC";
      break;
    default:
      return "Unknown";
  }
}

String SdData::getSize(){
  uint32_t blocksPerCluster = sdFat.vol()->blocksPerCluster();
  uint32_t clusterCount = sdFat.vol()->clusterCount();
  uint32_t freeClusterCount = sdFat.vol()->freeClusterCount();
  
  JSONVar objSrc;
  unsigned long notUsed = blocksPerCluster*freeClusterCount/2;//not used capacity (unit:KB)
  unsigned long total = blocksPerCluster*clusterCount/2;//total capacity (unit:KB)
  objSrc["notUsed"] = notUsed;
  objSrc["total"] = total;
  
  return JSON.stringify(objSrc);
}

String SdData::read(String fileName){
  String data = "";
  File file = sdFat.open(fileName);
  if(file){
    while(file.available()) {
      char temp = file.read();
      data += temp;
    }
    file.close();
  }
  return data;
}

bool SdData::write(String path, String data){
  remove(path);
   
  File file = sdFat.open(path, FILE_WRITE);
  if(file){
    file.print(data);
    file.close();
    Serial.println("write OK");
    return true;
  }
  Serial.println("w-error opening file");
  return false;
}

bool SdData::remove(String path){
  if(sdFat.exists(path.c_str())){
    Serial.print(path);
    Serial.println(" is existed");
    return sdFat.remove(path.c_str());
  }
  Serial.println("fileName isn't existed!");
  return false;
}

bool SdData::makeDir(String path){
  Serial.print("mkdir:");
  return sdFat.mkdir(path.c_str());
}

bool SdData::removeDir(String path){
  bool status = true;
  
  if(!sdFat.exists(path.c_str())){
    Serial.println(path);
    Serial.println("It's not existed! So not remove anything");
  }
  
  File file = sdFat.open(path);
  File entry;
  while(true){
    entry = file.openNextFile();
    if(!entry){
      break;
    }
    
    char buffer[bufferSize];
    entry.getName(buffer,bufferSize);
    String filePath =path + "/" + buffer;
    
    if(entry.isDirectory()){
      Serial.println("d-->"+filePath);
      status &= removeDir(filePath);
    }else{
      Serial.println("f-->"+filePath);
      status &= sdFat.remove(filePath.c_str());
    }
  }
  return status && sdFat.rmdir(path.c_str());
}

String SdData::getAllFileName(String path){
  String nodes = "";
  int pathSize = path.length();
  if(path == "" | path[pathSize-1] != '/'){
    path = path + "/";
  }
  
  File file = sdFat.open(path);
  while(true){
    File entry = file.openNextFile();
    if(!entry){//no more file, break this while
      //Serial.println("break");
      break;
    }

    if(nodes != ""){//has obj, add ","
      nodes += ",";
    }

    char buffer[bufferSize];
    entry.getName(buffer,bufferSize);
    String fileName = buffer;
    //Serial.println(buffer.length());

    if(fileName == "System Volume Information"){//Ignore "System Volume Information" folder
      continue;
    }
    nodes += fileName;
    if(entry.isDirectory()){
      nodes += "<";
      //Serial.print("folder:");
      //Serial.println(path + fileName + "/");
      nodes += getAllFileName(path + fileName + "/");
      //....
      nodes += ">";
    }
    entry.close();
  }
  return nodes;
}

String SdData::appDataProcess(String src){

  JSONVar objSrc = JSON.parse(src);
  if(objSrc.length()!=-1){//Check obj isn't an array
    return ".length():" + ((String)objSrc.length()) + " != -1 ";
  }

  String commandMode = (const char*)objSrc[JsonKey::COMMAND_MODE];
  String filePath = (const char*)objSrc[JsonKey::FILE_PATH];
  String data = (const char*)objSrc[JsonKey::DATA];

  if(commandMode == CmdMode::readFile){
    objSrc[JsonKey::DATA] = read(filePath);
    Serial.println(objSrc[JsonKey::DATA]);
    return JSON.stringify(objSrc);
  }
  if(commandMode == CmdMode::writeFile){
    objSrc[JsonKey::DATA] = write(filePath,data);
    Serial.println(objSrc[JsonKey::DATA]);
    return JSON.stringify(objSrc);
  }
  if(commandMode == CmdMode::removeFile){
    objSrc[JsonKey::DATA] = remove(filePath);
    Serial.println(objSrc[JsonKey::DATA]);
    return JSON.stringify(objSrc);
  }
  if(commandMode == CmdMode::getFileList){
    objSrc[JsonKey::DATA] = getAllFileName(filePath);
    Serial.println(objSrc[JsonKey::DATA]);
    return JSON.stringify(objSrc);
  }
  if(commandMode == CmdMode::makeDir){
    objSrc[JsonKey::DATA] = makeDir(filePath);
    Serial.println(objSrc[JsonKey::DATA]);
    return JSON.stringify(objSrc);
  }
  if(commandMode == CmdMode::removeDir){
    objSrc[JsonKey::DATA] = removeDir(filePath);
    Serial.println(objSrc[JsonKey::DATA]);
    return JSON.stringify(objSrc);
  }
  
  return "commamdMode:" + commandMode + " error!";
}
  
