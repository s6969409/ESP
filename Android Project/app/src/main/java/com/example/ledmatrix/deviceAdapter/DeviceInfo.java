package com.example.ledmatrix.deviceAdapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DeviceInfo {
    private Gson gson = new Gson();
    private String[] types;
    private LedMatrix ledMatrix;

    private static final String KEY_types = "types";
    private static final String KEY_ledMatrix = "ledMatrix";

    public DeviceInfo(String data){
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();

        String typesContent = jsonObject.getAsJsonArray(KEY_types).toString();
        types = gson.fromJson(typesContent,String[].class);
        for(String s:types) {
            if (s.equals(KEY_ledMatrix)){
                //parse "ledMatrix"
                ledMatrix = gson.fromJson(
                        jsonObject.getAsJsonObject(s).toString(),
                        LedMatrix.class
                );
            }
        }
    }

    public String[] getTypes() {
        return types;
    }

    public LedMatrix getLedMatrix() {
        return ledMatrix;
    }

    //data struct....
    public static class LedMatrix {
        private int ledNums;

        public LedMatrix(int ledNums) {
            this.ledNums = ledNums;
        }

        public int getLedNums() {
            return ledNums;
        }
    }
}
