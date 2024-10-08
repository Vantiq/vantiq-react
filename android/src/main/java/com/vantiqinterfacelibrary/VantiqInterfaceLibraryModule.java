package com.vantiqinterfacelibrary;

import android.app.Activity;
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
import io.vantiq.androidlib.VantiqAndroidLibrary;
import io.vantiq.androidlib.misc.Account;

import java.util.Date;


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

        db = new Database(this);

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
                map.putString("password", a.getPassword());
                map.putDouble("expiresAt", a.getExpiresAt());
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
                    map.putDouble("expiresAt", (double) a.getExpiresAt());
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
                    map.putLong("expiresAt", a.getExpiresAt());
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

    interface OnReadyToRunListener {
        void onReadyToRun();
    }

    //
    //  Before running a server operation we make sure that there is an access token and it has not expired
    //
    private void runServerOperation(String name, Promise promise,OnReadyToRunListener rtrListener)
    {
        new Thread(() -> {
            VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
            Account a = val.account;

            if (a.getAccessToken() == null)
            {
                VLog.e(TAG, "No Access Token");
                db.reject("AUTHINVALID", "The Authentication is invalid",promise);
                return;
            }

            long expiresAt = a.getExpiresAt();
            boolean hasExpired = true;
            if (expiresAt > 0)
            {
                Date expirationDate = new Date(expiresAt);
                Date now = new Date();
                if (expirationDate.after(now))
                {
                    hasExpired = false;
                }
            }

            //
            //  If the expiration date hasn't been reached yet we can just continue
            //
            if (!hasExpired)
            {
                VLog.i(TAG, "Running '" + name + "' on " + Thread.currentThread().getName());
                rtrListener.onReadyToRun();
            }
            else
            {
                String authType = a.getAuthType();

                if (VantiqAndroidLibrary.AT_INTERNAL.equals(authType))
                {
                    VLog.i(TAG, "Revalidating with Internal '" + name + "' on " + Thread.currentThread().getName());

                    Utilities.authWithInternal(a.getUsername(),a.getPassword(), new Utilities.TaskListener()
                    {
                        @Override
                        public void onComplete(Object obj)
                        {
                            Account a = (Account) obj;

                            if (a.getAccessToken() != null)
                            {
                                VLog.i(TAG, "Running '" + name + "' on " + Thread.currentThread().getName());
                                rtrListener.onReadyToRun();
                            }
                            else
                            {
                                VLog.e(TAG, "Access Token Invalid");
                                db.reject("AUTHINVALID", "The Authentication is invalid",promise);
                            }
                        }
                    });
                }
                else if (VantiqAndroidLibrary.AT_OAUTH.equals(authType))
                {
                    VLog.i(TAG, "Revalidating with OAuth '" + name + "' on " + Thread.currentThread().getName());
                }
                else
                {
                    throw new IllegalStateException("authType invalid " + authType);
                }
            }
        }).start();
    }


    @ReactMethod
    public void select(String type, ReadableArray props, String where, String sort, double limit, Promise promise)
    {
        this.runServerOperation("select", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.select(type, props, where, sort, limit, promise);
            }
        });
    }

    @ReactMethod
    public void selectOne(String type, String id, Promise promise)
    {
        this.runServerOperation("selectOne", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.selectOne(type, id, promise);
            }
        });
    }

    @ReactMethod
    public void count(String type, String where, Promise promise)
    {
        this.runServerOperation("count", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.count(type, where, promise);
            }
        });
    }

    @ReactMethod
    public void insert(String type, String object, Promise promise)
    {
        this.runServerOperation("insert", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.insert(type, object, promise);
            }
        });
    }

    @ReactMethod
    public void update(String type, String id, String object, Promise promise)
    {
        this.runServerOperation("update", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.update(type, id, object, promise);
            }
        });
    }

    @ReactMethod
    public void upsert(String type, String object, Promise promise)
    {
        this.runServerOperation("upsert", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.upsert(type, object, promise);
            }
        });
    }

    @ReactMethod
    public void delete(String type, String where, Promise promise)
    {
        this.runServerOperation("delete", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.delete(type, where, promise);
            }
        });
    }

    @ReactMethod
    public void deleteOne(String type, String id, Promise promise)
    {
        this.runServerOperation("deleteOne", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.deleteOne(type, id, promise);
            }
        });
    }

    @ReactMethod
    public void execute(String procedureName, String params, Promise promise)
    {
        this.runServerOperation("execute", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.execute(procedureName, params, promise);
            }
        });
    }

    @ReactMethod
    public void publish(String topic, String message, Promise promise)
    {
        this.runServerOperation("publish", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.publish(topic, message, promise);
            }
        });
    }

    //
    //  "resource" is one of 'topics', 'services', 'types' or 'sources'
    //  "resourceId" is the name of thre resource

    @ReactMethod
    public void publishEvent(String resource, String event, String resourceId, String message, Promise promise)
    {
        this.runServerOperation("publishEvent", promise, new OnReadyToRunListener()
        {
            @Override
            public void onReadyToRun()
            {
                db.publishEvent(resource, event, resourceId, message, promise);
            }
        });
    }

}

