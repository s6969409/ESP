package com.example.ledmatrix.local;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileProcess {
    public static final String FOLDER_deviceBackup = "deviceBackup";
    public static final String FOLDER_dataBackup = "backup";
    public static final String PATH_APP_GLOBAL_TEMP = "appGlobalTemp.txt";

    public static boolean pathExists(Context context, String path){
        return new File(context.getExternalFilesDir(null),path).exists();
    }

    public static String readFile(Context context, String path){
        StringBuilder stringBuilder = new StringBuilder();
        try {
            File file = new File(context.getExternalFilesDir(null),path);
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fileInputStream.read(buffer)) > 0) {
                stringBuilder.append(new String(buffer, 0 ,len));
            }
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context,"File not found!",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
        return stringBuilder.toString();
    }

    public static void writeFile(Context context, String path, String data){
        try {
            File file = new File(context.getExternalFilesDir(null),path);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data.getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context,"File not found!",Toast.LENGTH_SHORT).show();
        } catch (IOException e){
            e.printStackTrace();
            Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean removeFile(Context context, String path){
        File file = new File(context.getExternalFilesDir(null),path);
        if (file.isFile()){
            return file.delete();
        }
        return false;
    }

    public static boolean makeDir(Context context, String path){
        File file = new File(context.getExternalFilesDir(null),path);
        return file.exists() || file.mkdir();
    }

    public static boolean removeDir(Context context, String path){
        File file = new File(context.getExternalFilesDir(null),path);
        return removeDir(file);
    }

    private static boolean removeDir(File file){
        boolean status = true;
        if (file.exists()){
            if (file.isDirectory()){
                for (File f : file.listFiles()){
                    status &= removeDir(f);
                }
            }
            status &= file.delete();
        }
        return status;
    }

    public static List<String> listItems(Context context, String path){
        List<String> items = new ArrayList<>();
        File file = new File(context.getExternalFilesDir(null),path);
        for (File f : file.listFiles()){
            items.add(f.getName());
        }
        return items;
    }

    public static String CheckFileName(String src, int length){
        String isOkStr = "[^a-zA-Z0-9_ ]";
        Pattern p = Pattern.compile(isOkStr);
        Matcher m = p.matcher(src);
        String str = m.replaceAll("").trim();
        if(!src.equals(str)){
            return "has illegal char!";
        }
        if(src.length() > length){
            return "Name is over "+ String.valueOf(length) +" chars!";
        }
        return "";
    }

    public static String CheckNumberStr(String src,int minValue ,int maxValue){
        String isOkStr = "[^0-9.]";
        Pattern p = Pattern.compile(isOkStr);
        Matcher m = p.matcher(src);
        String str = m.replaceAll("").trim();
        if(!src.equals(str)){
            return "illegal input!";
        }

        int value;
        try {
            value = Integer.valueOf(src);
        }catch (NumberFormatException e){
            return "illegal input!";
        }

        if(value < minValue){
            return src + " is less than " + String.valueOf(minValue) + "!";
        }

        if(value > maxValue){
            return src + " is over than " + String.valueOf(maxValue) + "!";
        }
        return "";
    }

    public static List<WifiConnectedInfo> GetConnectedInfo(boolean removeTittle){
        List<WifiConnectedInfo> list = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader("/proc/net/arp"));
            String line = "";
            while ((line = bufferedReader.readLine()) != null){
                String[] splitted = line.split("  +");
                if (removeTittle && splitted[0].equals("IP address"))continue;
                list.add(new WifiConnectedInfo(
                        splitted[0],splitted[1],splitted[2],splitted[3],splitted[4],splitted[5]
                ));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    //data struct
    public static class WifiConnectedInfo{
        public final String ip,HwType,flags,mac,mask,device;

        public WifiConnectedInfo(String ip, String hwType, String flags, String mac, String mask, String device) {
            this.ip = ip;
            HwType = hwType;
            this.flags = flags;
            this.mac = mac;
            this.mask = mask;
            this.device = device;
        }
    }
}