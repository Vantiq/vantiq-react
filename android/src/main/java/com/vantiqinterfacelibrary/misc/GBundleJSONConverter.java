package com.vantiqinterfacelibrary.misc;

import android.os.Bundle;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A helper class that can round trip between JSON and Bundle objects that contains the types:
 *   Boolean, Integer, Long, Double, String
 * If other types are found, an IllegalArgumentException is thrown.
 */
public class GBundleJSONConverter {
  private static final Map<Class<?>, Setter> SETTERS = new HashMap<Class<?>, Setter>();

  static {

    SETTERS.put(JsonPrimitive.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {

        bundle.putString(key, ((JsonPrimitive)value).getAsString());
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        json.addProperty(key,((JsonPrimitive)value).getAsString());
      }
    });

    SETTERS.put(Boolean.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        bundle.putBoolean(key, (Boolean) value);
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        json.addProperty(key, (Boolean)value);
      }
    });
    SETTERS.put(Integer.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        bundle.putInt(key, (Integer) value);
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        json.addProperty(key, (Integer)value);
      }
    });
    SETTERS.put(Long.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        bundle.putLong(key, (Long) value);
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        json.addProperty(key, (Long)value);
      }
    });
    SETTERS.put(Double.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        bundle.putDouble(key, (Double) value);
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        json.addProperty(key, (Double)value);
      }
    });
    SETTERS.put(String.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        bundle.putString(key, (String) value);
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        json.addProperty(key, (String)value);
      }
    });
    SETTERS.put(String[].class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        throw new IllegalArgumentException("Unexpected type from JSON");
      }

      public void setOnJSON(JsonObject json, String key, Object value)  throws JsonParseException {
        JsonArray jsonArray = new JsonArray();
        for (String stringValue : (String[])value) {
          jsonArray.add(stringValue);
        }
        json.add(key, jsonArray);
      }
    });

    SETTERS.put(JsonArray.class, new Setter() {
      public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException {
        JsonArray jsonArray = (JsonArray)value;
        ArrayList<String> stringArrayList = new ArrayList<String>();
        // Empty list, can't even figure out the type, assume an ArrayList<String>
        if (jsonArray.size() == 0) {
          bundle.putStringArrayList(key, stringArrayList);
          return;
        }

        // Only strings are supported for now
        for (int i = 0; i < jsonArray.size(); i++) {
          Object current = jsonArray.get(i);
          if (current instanceof String) {
            stringArrayList.add((String)current);
          } else {
            throw new IllegalArgumentException("Unexpected type in an array: " + current.getClass());
          }
        }
        bundle.putStringArrayList(key, stringArrayList);
      }

      @Override
      public void setOnJSON(JsonObject json, String key, Object value) throws JsonParseException {
        throw new IllegalArgumentException("JsonArray's are not supported in bundles.");
      }
    });
  }

  public interface Setter {
    public void setOnBundle(Bundle bundle, String key, Object value) throws JsonParseException;
    public void setOnJSON(JsonObject json, String key, Object value) throws JsonParseException;
  }

  public static JsonObject convertToJSON(Bundle bundle) throws JsonParseException {
    JsonObject json = new JsonObject();

    for(String key : bundle.keySet()) {
      Object value = bundle.get(key);
      if (value == null) {
        // Null is not supported.
        continue;
      }

      // Special case List<String> as getClass would not work, since List is an interface
      if (value instanceof List<?>) {
        JsonArray jsonArray = new JsonArray();
        @SuppressWarnings("unchecked")
        List<String> listValue = (List<String>)value;
        for (String stringValue : listValue) {
          jsonArray.add(stringValue);
        }
        json.add(key, jsonArray);
        continue;
      }

      // Special case Bundle as it's one way, on the return it will be JsonObject
      if (value instanceof Bundle) {
        json.add(key, convertToJSON((Bundle)value));
        continue;
      }

      Setter setter = SETTERS.get(value.getClass());
      if (setter == null) {
        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
      }
      setter.setOnJSON(json, key, value);
    }

    return json;
  }

  public static Bundle convertToBundle(JsonObject jsonObject) throws JsonParseException {
    Bundle bundle = new Bundle();
    @SuppressWarnings("unchecked")
    Iterator<String> jsonIterator = jsonObject.keySet().iterator();
    while (jsonIterator.hasNext()) {
      String key = jsonIterator.next();
      Object value = jsonObject.get(key);
      if (value == null) {
        // Null is not supported.
        continue;
      }

      // Special case JsonObject as it's one way, on the return it would be Bundle.
      if (value instanceof JsonObject) {
        bundle.putBundle(key, convertToBundle((JsonObject)value));
        continue;
      }
      else if (value instanceof JsonPrimitive)
      {
        JsonPrimitive jp = (JsonPrimitive)value;

        if (jp.isBoolean())
        {
          bundle.putBoolean(key, jp.getAsBoolean());
        }
        else if (jp.isNumber())
        {
          bundle.putDouble(key, jp.getAsDouble());
        }
        else if (jp.isJsonArray())
        {
          JsonArray jsonArray = (JsonArray)value;

          ArrayList<String> stringArrayList = new ArrayList<String>();
          // Empty list, can't even figure out the type, assume an ArrayList<String>
          if (jsonArray.size() == 0) {
            bundle.putStringArrayList(key, stringArrayList);
          }
          else
          {
            // Only strings are supported for now
            for (int i = 0; i < jsonArray.size(); i++) {
              Object current = jsonArray.get(i);
              if (current instanceof String) {
                stringArrayList.add((String)current);
              } else {
                throw new IllegalArgumentException("Unexpected type in an array: " + current.getClass());
              }
            }
            bundle.putStringArrayList(key, stringArrayList);
          }
        }
        else if (jp.isString())
        {
          bundle.putString(key, jp.getAsString());
        }
        else
        {
          bundle.putString(key, jp.getAsString());
        }
      }
    }

    return bundle;
  }
}
