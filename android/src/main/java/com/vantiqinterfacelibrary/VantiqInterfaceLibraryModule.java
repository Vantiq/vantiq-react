package com.vantiqinterfacelibrary;

import android.app.Activity;
import android.content.Intent;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.vantiqinterfacelibrary.misc.*;
import io.vantiq.androidlib.Utilities;
import io.vantiq.androidlib.VantiqAndroidLibrary;
import io.vantiq.androidlib.misc.Account;
import io.vantiq.client.SortSpec;
import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class VantiqInterfaceLibraryModule extends ReactContextBaseJavaModule {
    private static final String TAG = "VantiqInterfaceLibraryModule";
    private static final String NAME = "VantiqReact";

    static public VantiqInterfaceLibraryModule INSTANCE;


    public ReactApplicationContext reactContext;
    public Promise thePromise;

    public VantiqInterfaceLibraryModule(ReactApplicationContext reactContext) {

      super(reactContext);

      this.reactContext = reactContext;

      INSTANCE = this;

      VLog vl = new VLog();

      VLog.i(TAG, "VantiqInterfaceLibraryModule Start: " + NAME);

      ///VantiqAndroidLibrary val = VantiqAndroidLibrary.initialize(reactContext);
    }



    public Activity getActivity()
    {
        return this.getCurrentActivity();
    }

    @Override
    public String getName() {
        return NAME;
    }


    @ReactMethod
    public void multiply(double a, double b, Promise promise)
    {
      VLog.i(TAG, "multiply");
      double product = Utilities.multiply(a,b);
      promise.resolve(product);
    }

    @ReactMethod
    public void add(double a, double b, Promise promise)
    {
      VLog.i(TAG, "add");
      double sum = Utilities.add(a,b);
      promise.resolve(sum);
    }

  @ReactMethod
  public void init(String server, String namespace, Promise promise)
  {
    VLog.i(TAG, "initialize");

    Utilities.initialize(getReactApplicationContext(),server,namespace,new Utilities.TaskListener() {
      public void onComplete(Object obj)
      {
          Account a = (Account)obj;
          JsonObject jo = a.getAsJsonObject();

          VLog.i(TAG, "onComplete: " + jo.toString());
          WritableMap map = Arguments.createMap();
          map.putString("server", a.getServer());
          map.putString("userId", a.getHRusername());
          map.putString("username", a.getUsername());
          map.putString("serverType", a.getAuthType());
          map.putString("errorStr", a.getErrorMessage());
          map.putBoolean("authValid", (a.getAccessToken() == null ? false: true));
          map.putInt("httpStatus", 0);
          promise.resolve(map);
      }
    });
  }


  @ReactMethod
  public void authWithInternal(String username, String password, Promise promise)
  {
    VLog.i(TAG, "authWithInternal");

    Utilities.authWithInternal(username,password, new Utilities.TaskListener() {
      @Override
      public void onComplete(Object obj)
      {
        Account a = (Account)obj;

        if (a.getAccessToken() != null)
        {
          WritableMap map = Arguments.createMap();
          map.putString("server", a.getServer());
          map.putString("userId", a.getHRusername());
          map.putString("username", a.getUsername());
          map.putString("serverType", a.getAuthType());
          map.putString("errorStr", a.getErrorMessage());
          map.putBoolean("authValid", (a.getAccessToken() == null ? false: true));
          map.putInt("httpStatus", 0);
          promise.resolve(map);
        }
        else
        {
          WritableMap map = Arguments.createMap();
          map.putString("server", a.getServer());
          map.putString("userId", a.getHRusername());
          map.putString("username", a.getUsername());
          map.putString("serverType", a.getAuthType());
          map.putString("errorStr", a.getErrorMessage());
          map.putBoolean("authValid", (a.getAccessToken() == null ? false: true));
          map.putInt("httpStatus", 0);
          promise.reject("LOGINFAILED",a.getErrorMessage(),map);
        }
      }
    });
  }

  @ReactMethod
  public void authWithOAuth(String redirectUrl, String clientId, Promise promise)
  {
    VLog.i(TAG, "authWithOAUth");
    Utilities.authWithOAuth(this.getActivity(), redirectUrl,clientId, new Utilities.TaskListener() {
      @Override
      public void onComplete(Object obj)
      {
        Account a = (Account)obj;

        if (a.getAccessToken() != null)
        {
          WritableMap map = Arguments.createMap();
          map.putString("server", a.getServer());
          map.putString("userId", a.getHRusername());
          map.putString("username", a.getUsername());
          map.putString("serverType", a.getAuthType());
          map.putString("errorStr", a.getErrorMessage());
          map.putBoolean("authValid", (a.getAccessToken() == null ? false: true));
          map.putInt("httpStatus", 0);
          promise.resolve(map);
        }
        else
        {
          WritableMap map = Arguments.createMap();
          map.putString("server", a.getServer());
          map.putString("userId", a.getHRusername());
          map.putString("username", a.getUsername());
          map.putString("serverType", a.getAuthType());
          map.putString("errorStr", a.getErrorMessage());
          map.putBoolean("authValid", (a.getAccessToken() == null ? false: true));
          map.putInt("httpStatus", 0);
          promise.reject("LOGINFAILED", "Canceled", map);
        }
      }
    });


  }

  @ReactMethod
  public void select(String type, ReadableArray props, String where, String sort, double limit, Promise promise)
  {
    VLog.i(TAG, "select");

    VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
    Account a = val.account;

    VLog.i(TAG, "type=" + type);
    VLog.i(TAG, "props=" + (props != null ? props.toString() : "EMPTY"));
    VLog.i(TAG, "where=" + where);
    VLog.i(TAG, "sort=" + sort);
    VLog.i(TAG, "limit=" + limit);

    Vantiq vantiqSDK = new Vantiq(a.getServer());
    vantiqSDK.setAccessToken(a.getAccessToken());
    vantiqSDK.setUsername(a.getUsername());

    ArrayList propsArray = null;

    if (props != null)
    {
      propsArray = props.toArrayList();
    }

    VantiqResponse vr = null;
    String exMessage = null;

    SortSpec sortSpec = new SortSpec("ars_createdAt",true);

    try
    {
      vr = vantiqSDK.select(type,propsArray, where, sortSpec, (long)limit);
    }
    catch (Exception ex)
    {
      exMessage = ex.getLocalizedMessage();
    }

    VLog.i(TAG, "returned");

    if (exMessage != null)
    {
      VLog.e(TAG,"select failed with exception: " + exMessage);
      WritableMap map = Arguments.createMap();
      map.putString("errorMsg", exMessage);
      promise.reject("SELECTFAILED", exMessage, map);
      return;
    }
    else if (vr.hasException())
    {
      Throwable ex = vr.getException();
      VLog.e(TAG,"select failed with throwable: " + ex.getLocalizedMessage());
      WritableMap map = Arguments.createMap();
      map.putString("errorMsg", ex.getLocalizedMessage());
      promise.reject("SELECTFAILED", ex.getLocalizedMessage(), map);
      return;
    }
    //
    //  We don't really know or care why this request failed, just that it did. Keep trying with
    //  other users in the same namespace until we find one that works.
    //
    else if (vr.isSuccess())
    {
      Object body = vr.getBody();
      List<JsonObject> results = (List<JsonObject>) body;

      WritableArray ary = Arguments.createArray();

      for (int i = 0; i < results.size(); i++)
      {
        JsonObject jo = results.get(i);

        //String s = jo.toString();
        //JSONObject gjo = null;
        try {
          /*
          gjo = new JSONObject(s);
          BundleJSONConverter bjc = new BundleJSONConverter();
          Bundle bundle = bjc.convertToBundle(gjo);
          WritableMap map = Arguments.fromBundle(bundle);
          ary.pushMap(map);
          */

          GBundleJSONConverter bjc = new GBundleJSONConverter();
          Bundle bundle = bjc.convertToBundle(jo);
          WritableMap map = Arguments.fromBundle(bundle);
          ary.pushMap(map);

        } catch (JsonParseException ex) {
          WritableMap map = Arguments.createMap();
          map.putString("errorMsg", ex.getLocalizedMessage());
          promise.reject("SELECTFAILED", ex.getLocalizedMessage(), map);
          return;
        }

      }


      promise.resolve(ary);
    }
    else if (vr.getStatusCode() == 401)
    {
      WritableMap map = Arguments.createMap();
      map.putString("errorMsg", "Access Token invalid");
      promise.reject("SELECT401", "Access Token invalid", map);
      return;
    }
    else
    {
      List<VantiqError> ves = vr.getErrors();
      for (int k=0; k<ves.size(); k++)
      {
        VantiqError ve = ves.get(k);
        VLog.e(TAG,ve.getCode() + " - " + ve.getMessage());

        String err = ve.getCode() + " - " + ve.getMessage();

        WritableMap map = Arguments.createMap();
        map.putString("errorMsg", err);
        promise.reject(ve.getCode(), ve.getMessage(), map);
        return;
      }
    }

  }
}

