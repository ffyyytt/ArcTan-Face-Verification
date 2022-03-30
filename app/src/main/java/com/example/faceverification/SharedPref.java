package com.example.faceverification;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SharedPref {
    SharedPreferences pref;
    private static final String TAG = "Shared Reference";
    private HashMap<String, Object > registered = new HashMap<>(); //saved Faces
    int OUTPUT_SIZE=512; //Output size of model


    public SharedPref(Context context){
        String prefName = context.getString(R.string.preference_file_key);
        this.pref = context.getSharedPreferences(prefName, MODE_PRIVATE);
    }

    public String getString(String name){
        String result = this.pref.getString(name, "None");
        return result;
    }

    public void setString(String name, String value){
        SharedPreferences.Editor editor = this.pref.edit();
        editor.putString(name, value);
        editor.apply();
    }

    public void storeJSONObject(JSONObject jsonObject){
        Iterator<String> iter = jsonObject.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                Object value = jsonObject.get(key);
                setString(key, value.toString());
            } catch (JSONException e) {
                // Something went wrong!
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Shared reference saved");
    }

    //Save Features to Shared Preferences - Float array convert to json string
    private void insertToSP(HashMap<String, Object> jsonMap, boolean clear) {
        if(clear)
            jsonMap.clear();
        else
            jsonMap.putAll(readFromSP());
        String jsonString = new Gson().toJson(jsonMap);

        setString("HashMap", jsonString);
//        Toast.makeText(context, "Features Saved", Toast.LENGTH_SHORT).show();
    }

    //Load features from Shared Preferences - Json String convert to float array
    private HashMap<String, Object> readFromSP(){
        String json= getString("HashMap");

        TypeToken<HashMap<String, Object >> token = new TypeToken<HashMap<String, Object >>() {};
        HashMap<String, Object> retrievedMap=new Gson().fromJson(json,token.getType());

        //During type conversion and save/load procedure,format changes(eg float converted to double).
        //So embeddings need to be extracted from it in required format(eg.double to float).
        for (Map.Entry<String, Object> entry : retrievedMap.entrySet())
        {
            float[][] output= new float[1][OUTPUT_SIZE];
            ArrayList arrayList= (ArrayList) entry.getValue();
            arrayList = (ArrayList) arrayList.get(0);
            for (int counter = 0; counter < arrayList.size(); counter++) {
                output[0][counter]= ((Double) arrayList.get(counter)).floatValue();
            }

            entry.setValue(output);
        }
        return retrievedMap;
    }
}
