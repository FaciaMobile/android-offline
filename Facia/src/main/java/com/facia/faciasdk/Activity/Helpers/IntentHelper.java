package com.facia.faciasdk.Activity.Helpers;

import java.util.HashMap;

public class IntentHelper {

    private static IntentHelper intentHelper = null;
    private HashMap<String,Object> hashMap;

    private IntentHelper(){
        hashMap = new HashMap<>();
    }

    /**
     * to return instance of Request Model
     */
    public static IntentHelper getInstance(){
        if(intentHelper == null){
            intentHelper = new IntentHelper();
        }
        return intentHelper;
    }

    /**
     * will add/insert object value
     */
    public void insertObject(String key, Object data){
        if(hashMap != null){
            hashMap.put(key,data);
        }
    }

    /**
     * will return the object
     */
    public Object getObject(String key){
        if(hashMap.containsKey(key)){
            return hashMap.get(key);
        }
        return null;
    }
}
