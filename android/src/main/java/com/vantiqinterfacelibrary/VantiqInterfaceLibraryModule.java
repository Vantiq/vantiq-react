package com.vantiqinterfacelibrary;

import android.app.Activity;
import android.os.AsyncTask;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.gson.JsonObject;
import com.vantiqinterfacelibrary.misc.VLog;
import io.vantiq.androidlib.Utilities;
import io.vantiq.androidlib.misc.Account;


public class VantiqInterfaceLibraryModule extends ReactContextBaseJavaModule
{
    private static final String TAG = "VantiqInterfaceLibraryModule";
    private static final String NAME = "VantiqReact";

    static public VantiqInterfaceLibraryModule INSTANCE;

    public ReactApplicationContext reactContext;
    public Promise thePromise;

    private Database db;

    public VantiqInterfaceLibraryModule(ReactApplicationContext reactContext)
    {

        super(reactContext);

        this.reactContext = reactContext;

        INSTANCE = this;

        this.db = new Database(this);

        VLog vl = new VLog();

        VLog.i(TAG, "VantiqInterfaceLibraryModule Start: " + NAME);
    }


    public Activity getActivity()
    {
        return this.getCurrentActivity();
    }

    @Override
    public String getName()
    {
        return NAME;
    }




    @ReactMethod
    public void init(String server, String namespace, Promise promise)
    {
        VLog.i(TAG, "initialize");

        Utilities.initialize(getReactApplicationContext(), server, namespace, new Utilities.TaskListener()
        {
            public void onComplete(Object obj)
            {
                Account a = (Account) obj;
                JsonObject jo = a.getAsJsonObject();

                VLog.i(TAG, "onComplete: " + jo.toString());
                WritableMap map = Arguments.createMap();
                map.putString("server", a.getServer());
                map.putString("userId", a.getHRusername());
                map.putString("username", a.getUsername());
                map.putString("serverType", a.getAuthType());
                map.putString("errorStr", a.getErrorMessage());
                map.putBoolean("authValid", (a.getAccessToken() == null ? false : true));
                map.putInt("httpStatus", 0);
                promise.resolve(map);
            }
        });
    }


    @ReactMethod
    public void multiply(double a, double b, Promise promise)
    {
        VLog.i(TAG, "multiply");
        double product = Utilities.multiply(a, b);
        promise.resolve(product);
    }

    @ReactMethod
    public void add(double a, double b, Promise promise)
    {
        VLog.i(TAG, "add");
        double sum = Utilities.add(a, b);
        promise.resolve(sum);
    }

    @ReactMethod
    public void authWithInternal(String username, String password, Promise promise)
    {
        VLog.i(TAG, "authWithInternal");

        Utilities.authWithInternal(username, password, new Utilities.TaskListener()
        {
            @Override
            public void onComplete(Object obj)
            {
                Account a = (Account) obj;

                if (a.getAccessToken() != null)
                {
                    WritableMap map = Arguments.createMap();
                    map.putString("server", a.getServer());
                    map.putString("userId", a.getHRusername());
                    map.putString("username", a.getUsername());
                    map.putString("serverType", a.getAuthType());
                    map.putString("errorStr", a.getErrorMessage());
                    map.putBoolean("authValid", (a.getAccessToken() == null ? false : true));
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
                    map.putBoolean("authValid", (a.getAccessToken() == null ? false : true));
                    map.putInt("httpStatus", 0);
                    promise.reject("LOGINFAILED", a.getErrorMessage(), map);
                }
            }
        });
    }

    @ReactMethod
    public void authWithOAuth(String redirectUrl, String clientId, Promise promise)
    {
        VLog.i(TAG, "authWithOAUth");
        Utilities.authWithOAuth(this.getActivity(), redirectUrl, clientId, new Utilities.TaskListener()
        {
            @Override
            public void onComplete(Object obj)
            {
                Account a = (Account) obj;

                if (a.getAccessToken() != null)
                {
                    WritableMap map = Arguments.createMap();
                    map.putString("server", a.getServer());
                    map.putString("userId", a.getHRusername());
                    map.putString("username", a.getUsername());
                    map.putString("serverType", a.getAuthType());
                    map.putString("errorStr", a.getErrorMessage());
                    map.putBoolean("authValid", (a.getAccessToken() == null ? false : true));
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
                    map.putBoolean("authValid", (a.getAccessToken() == null ? false : true));
                    map.putInt("httpStatus", 0);
                    promise.reject("LOGINFAILED", "Canceled", map);
                }
            }
        });
    }

    @ReactMethod
    public void select(String type, ReadableArray props, String where, String sort, double limit, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "select: " + Thread.currentThread().getName());
            db.select(type, props, where, sort, limit, promise);
        }).start();
    }

    @ReactMethod
    public void selectOne(String type, String id, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "selectOne: " + Thread.currentThread().getName());
            db.selectOne(type, id, promise);
        }).start();
    }

    @ReactMethod
    public void count(String type, String where, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "count: " + Thread.currentThread().getName());
            db.count(type, where, promise);
        }).start();
    }

    @ReactMethod
    public void insert(String type, String object, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "insert: " + Thread.currentThread().getName());
            db.insert(type, object, promise);
        }).start();
    }

    @ReactMethod
    public void update(String type, String id, String object, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "update: " + Thread.currentThread().getName());
            db.update(type, id, object, promise);
        }).start();
    }

    @ReactMethod
    public void upsert(String type, String object, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "upsert: " + Thread.currentThread().getName());
            db.upsert(type, object, promise);
        }).start();
    }

    @ReactMethod
    public void delete(String type, String where, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "delete: " + Thread.currentThread().getName());
            db.delete(type, where, promise);
        }).start();
    }

    @ReactMethod
    public void deleteOne(String type, String id, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "deleteOne: " + Thread.currentThread().getName());
            db.deleteOne(type, id, promise);
        }).start();
    }

    @ReactMethod
    public void execute(String procedureName, String params, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "execute: " + Thread.currentThread().getName());
            db.execute(procedureName, params, promise);
        }).start();
    }


    @ReactMethod
    public void publish(String topic, String message, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "publish: " + Thread.currentThread().getName());
            db.publish(topic, message, promise);
        }).start();
    }

    @ReactMethod
    public void publishEvent(String resource, String event, String resourceId, String message, Promise promise)
    {
        new Thread(() -> {
            VLog.i(TAG, "publish: " + Thread.currentThread().getName());
            db.publishEvent(resource, event, resourceId, message, promise);
        }).start();
    }

}

