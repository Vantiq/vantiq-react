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
import com.facebook.react.bridge.WritableMap;
import com.google.gson.JsonObject;
import com.vantiqinterfacelibrary.misc.*;
import io.vantiq.androidlib.Utilities;
import io.vantiq.androidlib.VantiqAndroidLibrary;
import io.vantiq.androidlib.misc.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
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
}

