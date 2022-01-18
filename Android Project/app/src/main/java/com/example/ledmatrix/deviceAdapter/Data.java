package com.example.ledmatrix.deviceAdapter;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.example.ledmatrix.FileEditActivity;
import com.google.gson.Gson;

import java.util.StringTokenizer;

public class Data {
    private static Gson gson = new Gson();

    public static final class Command{
        public static final String READ_FILE = "readFile";
        public static final String WRITE_FILE = "writeFile";
        public static final String REMOVE_FILE = "removeFile";
        public static final String GET_FILE_LIST = "getFileList";
        public static final String MAKE_DIR = "makeDir";
        public static final String REMOVE_DIR = "removeDir";
    }

    public static final class FolderPath {
        public static final String DEVICE_CONFIG_AP_SETTING = "deviceConfig/apSetting.txt";
        public static final String DEVICE_CONFIG_DEVICE_INFO = "deviceConfig/deviceInfo.txt";
        public static final String DEVICE_CONFIG_MAIN_PROGRAM = "deviceConfig/mainProgram.txt";
        public static final String RES_LEDS = "resource/LEDs";
        public static final String RES_PROGRAM = "resource/program";
        public static final String UNKNOW = "unknow";
    }

    public static final class ActivityConstant{
        public static final String INTENT_DEVICE_PATH = "devicePath";
        public static final String INTENT_FILE_PATH = "filePath";
        public static final String INTENT_FILE_DATA = "fileData";
        public static final int REQUEST_CODE_showFileContent = 110;
        public static final int RESULT_CODE_writeFileContetnt = REQUEST_CODE_showFileContent + 1;

        public static final String INTENT_PROGRAM_BLOCK = "programBlock";
        public static final String INTENT_PROGRAM_BLOCK_EDITED = "programBlockEdited";
        public static final int REQUEST_CODE_programBlockEdit = 112;
        public static final int RESULT_CODE_programBlockEdited = REQUEST_CODE_programBlockEdit + 1;
    }

    //Connection data format
    public static class Packet {
        private String commandMode;
        private String filePath;
        private String data;

        public Packet(String commandMode, String filePath, String data) {
            this.commandMode = commandMode;
            this.filePath = filePath;
            this.data = data;
        }
        //region ***Member of Setter & Getter***
        public String getCommandMode() {
            return commandMode;
        }

        public void setCommandMode(String commandMode) {
            this.commandMode = commandMode;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
        //endregion
    }

    //WS2812 LED data format
    public static class LedData {
        private int num;
        private int colorR;
        private int colorG;
        private int colorB;

        public LedData(int num, int colorR, int colorG, int colorB) {
            this.num = num;
            this.colorR = colorR;
            this.colorG = colorG;
            this.colorB = colorB;
        }

        //region*** Member of Setter & Getter ***
        public int getNum() {
            return num;
        }

        public void setNum(int num) {
            this.num = num;
        }

        public int getColorR() {
            return colorR;
        }

        public void setColorR(int colorR) {
            this.colorR = colorR;
        }

        public int getColorG() {
            return colorG;
        }

        public void setColorG(int colorG) {
            this.colorG = colorG;
        }

        public int getColorB() {
            return colorB;
        }

        public void setColorB(int colorB) {
            this.colorB = colorB;
        }
        //endregion
    }

    public static String getReadCommand(String path){
        Packet packet = new Packet(Command.READ_FILE,path,"");
        return gson.toJson(packet);
    }

    public static String getWriteCommand(String path, String data){
        Packet packet = new Packet(Command.WRITE_FILE,path,data);
        return gson.toJson(packet);
    }

    public static String getRemoveFileCommand(String path){
        Packet packet = new Packet(Command.REMOVE_FILE,path,"");
        return gson.toJson(packet);
    }

    public static String getFileListCommand(){
        Packet packet = new Packet(Command.GET_FILE_LIST,"","");
        return gson.toJson(packet);
    }

    public static String getMakeDirCommand(String path){
        Packet packet = new Packet(Command.MAKE_DIR,path,"");
        return gson.toJson(packet);
    }

    public static String getRemoveDirCommand(String path){
        Packet packet = new Packet(Command.REMOVE_DIR,path,"");
        return gson.toJson(packet);
    }

    public static String getFileClass(String path){
        int pathSize = path.length();
        //DEVICE_CONFIG_AP_SETTING
        if (pathSize >= FolderPath.DEVICE_CONFIG_AP_SETTING.length()
                && path.substring(0, FolderPath.DEVICE_CONFIG_AP_SETTING.length()).equals(FolderPath.DEVICE_CONFIG_AP_SETTING)
        ){
            return FolderPath.DEVICE_CONFIG_AP_SETTING;
        }
        //DEVICE_CONFIG_DEVICE_INFO
        if (pathSize >= FolderPath.DEVICE_CONFIG_DEVICE_INFO.length()
                && path.substring(0, FolderPath.DEVICE_CONFIG_DEVICE_INFO.length()).equals(FolderPath.DEVICE_CONFIG_DEVICE_INFO)
        ){
            return FolderPath.DEVICE_CONFIG_DEVICE_INFO;
        }
        //DEVICE_CONFIG_MAIN_PROGRAM
        if (pathSize >= FolderPath.DEVICE_CONFIG_MAIN_PROGRAM.length()
                && path.substring(0, FolderPath.DEVICE_CONFIG_MAIN_PROGRAM.length()).equals(FolderPath.DEVICE_CONFIG_MAIN_PROGRAM)
        ){
            return FolderPath.DEVICE_CONFIG_MAIN_PROGRAM;
        }
        //RES_LEDS
        if (pathSize >= FolderPath.RES_LEDS.length()
                && path.substring(0, FolderPath.RES_LEDS.length()).equals(FolderPath.RES_LEDS)
        ){
            return FolderPath.RES_LEDS;
        }
        //RES_PROGRAM
        if (pathSize >= FolderPath.RES_PROGRAM.length()
                && path.substring(0, FolderPath.RES_PROGRAM.length()).equals(FolderPath.RES_PROGRAM)
        ){
            return FolderPath.RES_PROGRAM;
        }
        return FolderPath.UNKNOW;
    }

    public static void startCreateActivity(Activity activity, String path){
        Intent intent = new Intent();
        intent.putExtra(ActivityConstant.INTENT_FILE_PATH, path);
        intent.putExtra(ActivityConstant.INTENT_FILE_DATA, getFileNullData(path));
        startActivityForResult(activity, intent, getFileClass(path));
    }

    public static void startEditActivity(Activity activity,String devicePath, Packet packet, boolean useTxt){
        Intent intent = new Intent();
        intent.putExtra(ActivityConstant.INTENT_DEVICE_PATH, devicePath);
        intent.putExtra(ActivityConstant.INTENT_FILE_PATH, packet.getFilePath());
        intent.putExtra(ActivityConstant.INTENT_FILE_DATA, packet.getData());
        String fileClass = useTxt?FolderPath.UNKNOW:getFileClass(packet.getFilePath());
        startActivityForResult(activity, intent, fileClass);
    }

    private static String getFileNullData(String path){
        if (getFileClass(path).equals(FolderPath.UNKNOW)){
            return "new file";
        }
        if (getFileClass(path).equals(FolderPath.RES_LEDS)
                || getFileClass(path).equals(FolderPath.RES_PROGRAM)){
            return "[]";
        }
        return "";
    }

    private static void startActivityForResult(Activity activity, Intent intent, String fileClass){
        if (fileClass.equals(FolderPath.DEVICE_CONFIG_MAIN_PROGRAM)) {
            // read mainProgram file
            intent.setClass(activity, MainProgramActivity.class);
        }
        if (fileClass.equals(FolderPath.RES_LEDS)) {
            // read LedData file
            intent.setClass(activity, LedMatrixActivity.class);
        }
        if (fileClass.equals(FolderPath.DEVICE_CONFIG_AP_SETTING)
                ||fileClass.equals(FolderPath.DEVICE_CONFIG_DEVICE_INFO)
                ||fileClass.equals(FolderPath.UNKNOW)) {
            // read general file
            intent.setClass(activity, FileEditActivity.class);
        }
        if (fileClass.equals(FolderPath.RES_PROGRAM)){
            // read program file
            intent.setClass(activity, ProgramActivity.class);
        }
        activity.startActivityForResult(intent, ActivityConstant.REQUEST_CODE_showFileContent);
    }

    public static String getItemInFolderPathByStr(
            String src, String folderPathStr, boolean withoutFolderPathStr){
        StringTokenizer stringTokenizer = new StringTokenizer(folderPathStr,"/");
        int pathFolderDepth = stringTokenizer.countTokens();

        StringBuilder ans = new StringBuilder();

        boolean isInFolder = false;
        int folderDepth = 0;
        StringBuilder fileNode = new StringBuilder();
        String compareFolder = stringTokenizer.nextToken();
        boolean breakFor = false;
        for (int i=0; i<src.length(); i++){
            String s = src.substring(i, i+1);
            if (!breakFor) {
                if (isInFolder) {//It's in folder, save all member
                    ans.append(s);
                }
                //check is in folder
                switch (s) {
                    case ","://get file
                        if (fileNode.length() == 0) continue;
                        fileNode.delete(0, fileNode.length());
                        break;
                    case "<"://get folder
                        if (fileNode.toString().equals(compareFolder)) {
                            if (!withoutFolderPathStr) {
                                ans.append(fileNode + "<");
                            }
                            if (stringTokenizer.hasMoreElements()) {
                                compareFolder = stringTokenizer.nextToken();
                            } else {
                                isInFolder = true;
                            }
                        }
                        folderDepth++;
                        fileNode.delete(0, fileNode.length());
                        break;
                    case ">"://end folder
                        folderDepth--;
                        if (isInFolder && folderDepth < pathFolderDepth) {
                            isInFolder = false;
                            breakFor = true;
                        }
                        fileNode.delete(0, fileNode.length());
                        break;
                    default:
                        fileNode.append(s);
                }
            } else {
                for (int i1 = pathFolderDepth; !withoutFolderPathStr && i1 > 0; i1--){
                    ans.append(">");
                }
                break;
            }
        }
        return ans.toString();
    }
}
