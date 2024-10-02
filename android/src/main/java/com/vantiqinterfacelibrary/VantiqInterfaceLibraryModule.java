package com.vantiqinterfacelibrary;

import android.app.Activity;
import android.content.Intent;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.vantiqinterfacelibrary.misc.*;
import io.vantiq.androidlib.Utilities;
import io.vantiq.androidlib.VantiqAndroidLibrary;


public class VantiqInterfaceLibraryModule extends ReactContextBaseJavaModule {
    private static final String TAG = "VantiqInterfaceLibraryModule";
    private static final String NAME = "VantiqInterfaceLibrary";

    static public VantiqInterfaceLibraryModule INSTANCE;


    public ReactApplicationContext reactContext;
    public Promise thePromise;

    public VantiqInterfaceLibraryModule(ReactApplicationContext reactContext) {

      super(reactContext);

      this.reactContext = reactContext;

      INSTANCE = this;

      VLog vl = new VLog();

      VLog.i(TAG, "VantiqInterfaceLibraryModule Start");

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
      //promise.reject("Error");
    }

  @ReactMethod
  public void initialize(String server, String namespace, Promise promise)
  {
    VLog.i(TAG, "initialize");

    Utilities.initialize(getReactApplicationContext(),server,namespace,new Utilities.TaskListener() {
      @Override
      public void onComplete(Object obj) {
        promise.resolve(true);
      }
    });
  }

  @ReactMethod
  public void testOne(Promise promise)
  {
    VLog.i(TAG, "testOne");
    Utilities.testOne(this.getActivity(), new Utilities.TaskListener() {
      @Override
      public void onComplete(Object obj)
      {
        Utilities.LoginResponse result = (Utilities.LoginResponse)obj;

        if (result.success)
        {
          promise.resolve(result.token);
        }
        else
        {
          promise.reject(result.errorMessage);
        }
      }
    });
  }
}

