#ifndef SdData_h
#define SdData_h

#include <SPI.h>
//#include <SD.h>
#include "SdFat.h"
using namespace sdfat;

#include <Arduino_JSON.h>

class CmdMode {
public:
  static const String readFile;
  static const String writeFile;
  static const String removeFile;
  static const String getFileList;
  static const String makeDir;
  static const String removeDir;
};

class FolderPath {
public:
  static const String DEVICE_CONFIG_AP_SETTING;
  static const String DEVICE_CONFIG_DEVICE_INFO;
  static const String DEVICE_CONFIG_MAIN_PROGRAM;
  static const String RES_LEDS;
  static const String RES_PROGRAM;
};

class JsonKey {
public:
  static const String COMMAND_MODE;
  static const String FILE_PATH;
  static const String DATA;
};

class SdData {
  //https://frank1025.pixnet.net/blog/post/348607129-%5Barduino%5D012-sd%E5%8D%A1%E8%AE%80%E5%8F%96%E6%93%8D%E4%BD%9C
private:
  static bool SDisBegin;
  static const size_t bufferSize;
  SdFat sdFat;
public:
  SdData();
  String getHardwareType();
  String getSize();
  String read(String fileName);
  bool write(String fileName, String data);
  bool remove(String fileName);
  bool makeDir(String path);
  bool removeDir(String path);
  String getAllFileName(String path);
  String appDataProcess(String src);
};

#endif
