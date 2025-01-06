package com.vantiqreact;

import android.app.Activity;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.*;
import com.vantiqreact.misc.VLog;
import io.vantiq.androidlib.VantiqAndroidLibrary;
import io.vantiq.androidlib.misc.Account;
import io.vantiq.client.SortSpec;
import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Database
{
    private static final String TAG = "Database";

    VantiqReactModule vilm;

    public Database(VantiqReactModule _vilm)
    {
        vilm = _vilm;
    }

    private JsonObject convertToObject(String op, String propertyName, ReadableMap rm,Promise promise)
    {
        String rmAsString = rm.toString();
        JsonObject jo = null;

        //
        //  This should never actually fail because what we are parsing is the output of the "toString" above,
        //  and that should always be valid JSON. Handle that just in case.
        //
        try
        {
            jo = new JsonParser().parse(rmAsString).getAsJsonObject();
        }
        catch (Exception ex)
        {
            this.rejectParse(op,propertyName,ex,promise);
        }


        return jo;
    }

    private JsonArray convertToArray(String op, String propertyName, ReadableArray ra,Promise promise)
    {
        String raAsString = ra.toString();
        JsonArray ja = null;

        //
        //  This should never actually fail because what we are parsing is the output of the "toString" above,
        //  and that should always be valid JSON. Handle that just in case.
        //
        try
        {
            ja = new JsonParser().parse(raAsString).getAsJsonArray();
        }
        catch (Exception ex)
        {
            this.rejectParse(op,propertyName,ex,promise);
        }

        return ja;
    }

    public void reject(String op, String errorCode, String errorMsg, Promise promise)
    {
        VLog.e(TAG, "REJECT: " + op + " code=" + errorCode + " msg=" + errorMsg);
        WritableMap map = computeError(errorCode,errorMsg,0,null);
        promise.reject(errorCode, errorMsg, map);
    }

    public void rejectException(String op, Throwable ex, Promise promise)
    {
        String errorMessage = ex.getLocalizedMessage();

        VLog.e(TAG, "REJECTEXCEPTION: " + op + "  msg=" + errorMessage);

        WritableMap map = computeError("vantiq.exception",errorMessage,0,null);

        promise.reject(Error.veOSError, errorMessage, map);
    }

    public void rejectParse(String op, String propertyName, Exception e, Promise promise)
    {
        String errorMessage = e.getLocalizedMessage();
        errorMessage = "Invalid JSON in '" + propertyName + "': " + errorMessage;

        VLog.e(TAG, "REJECTPARSE: op=" + op + " msg=" + errorMessage);

        WritableMap map = computeError("vantiq.exception",errorMessage,0,null);

        promise.reject(Error.veJsonParseError, errorMessage, map);
    }

    public void rejectVantiqError(String op, VantiqError ve, int statusCode, Promise promise)
    {
        String errorCode = ve.getCode();
        String errorMessage = ve.getMessage();

        VLog.e(TAG, "REJECTVANTIQERROR: op=" + op + " code=" + errorCode + " msg=" + errorMessage);

        WritableMap map = computeError(errorCode,errorMessage,statusCode,null);

        promise.reject(errorCode, errorMessage, map);
    }

    public void reject401(String op, Promise promise)
    {
        VLog.e(TAG, "REJECT401: op=" + op);

        String errorMessage = "Access Token Invalid";
        WritableMap map = computeError("vantiq.auth.invalid",errorMessage,401,null);

        promise.reject(Error.veNotAuthorized, errorMessage, map);
    }

    public void rejectErrorObject(String op, JsonObject error, Promise promise)
    {
        VLog.e(TAG, "REJECTERROROBJECT: op=" + op);
        String errorMsg;
        String errorCode;
        int httpStatus;

        if (error.isJsonNull())
        {
            errorMsg =  "Null Error";
            errorCode =  "com.vantiq.null";
            httpStatus = 0;
        }
        else
        {
            errorMsg =  error.get("errorMsg").getAsString();
            errorCode =  error.get("errorCode").getAsString();
            httpStatus = error.get("httpStatus").getAsInt();
        }

        WritableMap wm = Arguments.createMap();

        wm.putString("errorCode", errorCode);
        wm.putString("errorMsg", errorMsg);
        wm.putInt("httpStatus", httpStatus);

        promise.reject(errorCode, errorMsg, wm);
    }

    //
    //  Take a JsonObject and convert it to a WritableMap, which can be returned to ReactNative
    //
    private WritableMap convertToWritableMap(JsonObject jsonObject)
    {
        WritableMap wm = Arguments.createMap();

        Iterator<String> jsonIterator = jsonObject.keySet().iterator();
        while (jsonIterator.hasNext())
        {
            String key = jsonIterator.next();
            JsonElement je = jsonObject.get(key);

            if (je instanceof JsonPrimitive)
            {
                JsonPrimitive jp = (JsonPrimitive) je;

                if (jp.isBoolean())
                {
                    wm.putBoolean(key,jp.getAsBoolean());
                }
                else if (jp.isNumber())
                {
                    wm.putDouble(key,jp.getAsDouble());
                }
                else if (jp.isString())
                {
                    wm.putString(key, jp.getAsString());
                }
                else
                {
                    wm.putString(key, jp.getAsString());
                }
            }
            else if (je instanceof JsonObject)
            {
                wm.putMap(key,this.convertToWritableMap((JsonObject) je));
            }
            else if (je instanceof JsonArray)
            {
                wm.putArray(key,this.convertToWritableArray((JsonArray) je));
            }
        }

        return wm;
    }

    //
    //  Take a JsonArray and convert it to a WritableMap, which can be returned to ReactNative
    //
    private WritableArray convertToWritableArray(JsonArray jsonArray)
    {
        WritableArray wa = Arguments.createArray();

        // Empty list, can't even figure out the type
        if (jsonArray.size() == 0)
        {
        }
        else
        {
            for (int i = 0; i < jsonArray.size(); i++)
            {
                JsonElement jsonElement = jsonArray.get(i);

                if (jsonElement instanceof JsonPrimitive)
                {
                    JsonPrimitive jp = (JsonPrimitive) jsonElement;

                    if (jp.isBoolean())
                    {
                        wa.pushBoolean(jp.getAsBoolean());
                    }
                    else if (jp.isNumber())
                    {
                        wa.pushDouble(jp.getAsDouble());
                    }
                    else if (jp.isString())
                    {
                        wa.pushString(jp.getAsString());
                    }
                    else
                    {
                        wa.pushString(jp.getAsString());
                    }
                }
                else if (jsonElement instanceof JsonObject)
                {
                    wa.pushMap(this.convertToWritableMap((JsonObject) jsonElement));
                }
                else if (jsonElement instanceof JsonArray)
                {
                    wa.pushArray(this.convertToWritableArray((JsonArray) jsonElement));
                }
            }
        }

        return wa;
    }

    private Object convertJsonElementToReturnable(JsonElement je)
    {
        if (je == null)
        {
            return null;
        }
        else if (je instanceof JsonPrimitive)
        {
            JsonPrimitive jp = (JsonPrimitive) je;

            if (jp.isBoolean())
            {
                return jp.getAsBoolean();
            }
            else if (jp.isNumber())
            {
                return jp.getAsNumber();
            }
            else if (jp.isString())
            {
                return jp.getAsString();
            }
            else
            {
                return jp.getAsString();
            }
        }
        else if (je instanceof JsonObject)
        {
            return this.convertToWritableMap((JsonObject) je);
        }
        else if (je instanceof JsonArray)
        {
            return this.convertToWritableArray((JsonArray) je);
        }

        return null;
    }

    static private WritableMap computeError(String code, String message, int statusCode, String errorBodyAsString)
    {
        String fullMessage = message;

        if (errorBodyAsString != null)
        {
            try
            {
                JsonElement je = new JsonParser().parse(errorBodyAsString);

                if (je.isJsonArray())
                {
                    JsonArray ja = (JsonArray) je;

                    if (ja.size() > 0)
                    {
                        JsonElement errJE = ja.get(0);

                        if (errJE.isJsonObject())
                        {
                            JsonObject errJO = errJE.getAsJsonObject();

                            code = errJO.get("code").getAsString();
                            fullMessage += " - " + errJO.get("message").getAsString();
                        }
                    }
                }
            }
            catch (JsonSyntaxException e)
            {
                throw new RuntimeException(e);
            }
        }
        WritableMap wm = Arguments.createMap();

        wm.putString("errorCode", code);
        wm.putString("errorMsg", fullMessage);
        wm.putInt("httpStatus", statusCode);

        return wm;
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    //
    //
    //  Public methods start here
    //
    public void count(String type, ReadableMap where, Promise promise)
    {
        JsonObject whereObject = null;

        //
        //  Turn the "where" parameter into a JsonObject
        //
        if (where != null)
        {
            whereObject = this.convertToObject("count","where",where,promise);

            if (whereObject == null)
            {
                return;
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        val.count(type, whereObject, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                Integer count = (Integer) result;
                VLog.i(TAG, "Items returned: " + count.intValue());
                promise.resolve(count.doubleValue());
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("count",error,promise);
            }
        });

    }






    public void createInternalUser(String username, String password, String email, String firstName, String lastName, String phone, Promise promise)
    {
        String procedureName = "Registration.createInternalUser";

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        final Account a = val.account;

        //
        //  Turn the "params" parameter into a JsonObject
        //;
        JsonObject paramsAsObject = new JsonObject();
        JsonObject obj = new JsonObject();
        paramsAsObject.add("obj", obj);
        if (username != null) obj.addProperty("username", username);
        if (password != null) obj.addProperty("password", password);
        if (email != null) obj.addProperty("email", email);
        if (firstName != null) obj.addProperty("firstName", firstName);
        if (lastName != null) obj.addProperty("lastName", lastName);
        if (phone != null) obj.addProperty("phone", phone);

        val.executePublic(a.getNamespace(), procedureName, paramsAsObject, new VantiqAndroidLibrary.ResponseListener()  {
            @Override
            public void resolve(Object obj)
            {
                JsonObject jo = (JsonObject) obj;

                if (jo.get("insertSuccessful").getAsBoolean())
                {
                    promise.resolve(true);
                }
                else
                {
                    JsonObject errorObject = jo.get("error").getAsJsonObject();
                    String errorCode = errorObject.get("code").getAsString();
                    String errorMessage = errorObject.get("message").getAsString();

                    VLog.e(TAG, "REJECT ERROR: op=" + "createInternalUser" + " code=" + errorCode + " msg=" + errorMessage);

                    WritableMap map = computeError(errorCode, errorMessage, 0, null);
                    promise.reject(errorCode, errorMessage, map);

                    promise.reject(Error.veRESTError, errorMessage, map);
                }
            }

            @Override
            public void reject(JsonObject jo)
            {
                JsonObject errorObject = jo;
                String errorCode = errorObject.get("errorCode").getAsString();
                String errorMessage = errorObject.get("errorMsg").getAsString();

                VLog.e(TAG, "REJECT ERROR: op=" + "createInternalUser" + " code=" + errorCode + " msg=" + errorMessage);

                WritableMap map = computeError(errorCode, errorMessage, 0, null);
                promise.reject(errorCode, errorMessage, map);
            }
        });
    }


    public void createOAuthUser(String redirectUrl, String clientId, Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        String procedureName = "com.vantiq.ReactUtilities.createDRPCode";

        val.createOAuthUser(VantiqReactModule.INSTANCE.getActivity(), procedureName, redirectUrl, clientId, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object obj)
            {
                Account a = VantiqAndroidLibrary.INSTANCE.account;

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

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("createOAuthUser",error,promise);
            }
        });
    }

    public void delete(String type, ReadableMap where, Promise promise)
    {
        JsonObject whereObject = null;
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        //
        //  Turn the "where" parameter into a JsonObject
        //
        if (where != null)
        {
            whereObject = this.convertToObject("delete","where",where,promise);

            if (whereObject == null)
            {
                return;
            }
        }

        val.delete(type, whereObject, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                promise.resolve(true);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("delete",error,promise);
            }
        });
   }

    public void deleteOne(String type, String id, Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        val.deleteOne(type, id, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                promise.resolve(true);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("delete",error,promise);
            }
        });
    }

    private void execute(String procedureName, JsonElement params, Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.execute(procedureName, params, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonElement je = (JsonElement) result;

                if (je instanceof JsonPrimitive)
                {
                    JsonPrimitive jp = (JsonPrimitive) je;

                    if (jp.isBoolean())
                    {
                        promise.resolve(jp.getAsBoolean());
                    }
                    else if (jp.isNumber())
                    {
                        Number n = jp.getAsNumber();
                        promise.resolve(n.doubleValue());
                    }
                    else if (jp.isString())
                    {
                        promise.resolve(jp.getAsString());
                    }
                    else
                    {
                        promise.resolve(jp.getAsString());
                    }
                }
                else  if (je instanceof JsonObject)
                {
                    WritableMap wm = (WritableMap) convertJsonElementToReturnable(je);
                    promise.resolve(wm);
                }
                else if (je instanceof JsonArray)
                {
                    WritableArray wa = (WritableArray) convertJsonElementToReturnable(je);
                    promise.resolve(wa);
                }
                else
                {
                    promise.resolve(null);
                }
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("execute",error,promise);
            }
        });
    }

    public void executeByName(String procedureName, ReadableMap paramsMap, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonObject
        //;
        JsonObject paramsAsObject = this.convertToObject("executeByName","params",paramsMap,promise);

        this.execute(procedureName, paramsAsObject, promise);
    }

    public void executeByPosition(String procedureName, ReadableArray paramsArray, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonArray
        //
        JsonArray paramsAsArray =  this.convertToArray("executeByPosition", "params", paramsArray, promise);

        this.execute(procedureName, paramsAsArray, promise);
    }

    private void executePublic(String namespace, String procedureName, JsonElement params, Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.executePublic(namespace, procedureName, params, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonElement je = (JsonElement) result;

                if (je instanceof JsonPrimitive)
                {
                    JsonPrimitive jp = (JsonPrimitive) je;

                    if (jp.isBoolean())
                    {
                        promise.resolve(jp.getAsBoolean());
                    }
                    else if (jp.isNumber())
                    {
                        Number n = jp.getAsNumber();
                        promise.resolve(n.doubleValue());
                    }
                    else if (jp.isString())
                    {
                        promise.resolve(jp.getAsString());
                    }
                    else
                    {
                        promise.resolve(jp.getAsString());
                    }
                }
                else  if (je instanceof JsonObject)
                {
                    WritableMap wm = (WritableMap) convertJsonElementToReturnable(je);
                    promise.resolve(wm);
                }
                else if (je instanceof JsonArray)
                {
                    WritableArray wa = (WritableArray) convertJsonElementToReturnable(je);
                    promise.resolve(wa);
                }
                else
                {
                    promise.resolve(null);
                }
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("execute",error,promise);
            }
        });
    }

    public void executePublicByName(String namespace, String procedureName, ReadableMap paramsMap, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonObject
        //;
        JsonObject paramsAsObject = this.convertToObject("executeByName","params",paramsMap,promise);

        this.executePublic(namespace, procedureName, paramsAsObject, promise);
    }

    public void executePublicByPosition(String namespace, String procedureName, ReadableArray paramsArray, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonArray
        //
        JsonArray paramsAsArray =  this.convertToArray("executePublicByPosition", "params", paramsArray, promise);

        this.executePublic(namespace,procedureName, paramsAsArray, promise);
    }

    public void executeStreamedByName(String procedureName, ReadableMap paramsMap, String progressEvent, Double maxBufferSize, Double maxFlushInterval, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonObject
        //;
        JsonObject paramsAsObject = this.convertToObject("executeByName","params",paramsMap,promise);

        Integer effMmaxBufferSize = null;
        Long effMaxFlushInterval = null;;

        if (maxBufferSize != null)
        {
            effMmaxBufferSize = Integer.valueOf(maxBufferSize.intValue());
        }
        if (maxFlushInterval != null)
        {
            effMaxFlushInterval = Long.valueOf(maxFlushInterval.longValue());
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.executeStreamed(procedureName, paramsAsObject, effMmaxBufferSize,effMaxFlushInterval, new VantiqAndroidLibrary.GenericCallback()
        {
            @Override
            public void invoke(Object obj)
            {
                if (progressEvent != null)
                {
                    //VLog.i(TAG, "PROGRESS EVENT: " + progressEvent);
                    JsonElement je = (JsonElement) obj;
                    sendEvent(VantiqReactModule.INSTANCE.reactContext, progressEvent, (WritableMap) convertJsonElementToReturnable(je));
                }
            }
        }, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object obj)
            {
                //VLog.i(TAG, "DONE");
                JsonElement je = (JsonElement) obj;
                promise.resolve(convertJsonElementToReturnable(je));
            }

            @Override
            public void reject(JsonObject jsonObject)
            {
                JsonObject error = jsonObject.getAsJsonObject("error");
                rejectErrorObject("executeStreamedByName",error,promise);

            }
        });
    }

    public void executeStreamedByPosition(String procedureName, ReadableArray paramsArray, String progressEvent, Double maxBufferSize, Double maxFlushInterval, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonArray
        //
        JsonArray paramsAsArray =  this.convertToArray("executeByPosition", "params", paramsArray, promise);

        Integer effMmaxBufferSize = null;
        Long effMaxFlushInterval = null;;

        if (maxBufferSize != null)
        {
            effMmaxBufferSize = Integer.valueOf(maxBufferSize.intValue());
        }
        if (maxFlushInterval != null)
        {
            effMaxFlushInterval = Long.valueOf(maxFlushInterval.longValue());
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.executeStreamed(procedureName, paramsAsArray, effMmaxBufferSize, effMaxFlushInterval, new VantiqAndroidLibrary.GenericCallback()
        {
            @Override
            public void invoke(Object obj)
            {
                if (progressEvent != null)
                {
                    //VLog.i(TAG, "PROGRESS EVENT: " + progressEvent);
                    JsonElement je = (JsonElement) obj;
                    sendEvent(VantiqReactModule.INSTANCE.reactContext, progressEvent, (WritableMap) convertJsonElementToReturnable(je));
                }
            }
        },
        new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object obj)
            {
                //VLog.i(TAG, "DONE");
                JsonElement je = (JsonElement) obj;
                promise.resolve(convertJsonElementToReturnable(je));
            }

            @Override
            public void reject(JsonObject jsonObject)
            {
                JsonObject error = jsonObject.getAsJsonObject("error");
                rejectErrorObject("executeStreamedByPosition",error,promise);
            }
        });
    }


    public void insert(String type, ReadableMap object, Promise promise)
    {
        JsonObject objectToInsert = null;

        //
        //  Turn the "object" parameter into a JsonObject
        //
        if (object != null)
        {
            objectToInsert = this.convertToObject("insert","object",object,promise);

            if (objectToInsert == null)
            {
                return;
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.insert(type, objectToInsert, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonObject jo = (JsonObject) result;
                promise.resolve((WritableMap) convertJsonElementToReturnable(jo));
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("insert",error,promise);
            }
        });
    }

    public void logout(Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.logout(new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                promise.resolve(true);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("logout",error,promise);
            }
        });
    }

    public void publish(String topic, ReadableMap message, Promise promise)
    {
        JsonObject messageObject = null;

        //
        //  Turn the "object" parameter into a JsonObject
        //
        if (message != null)
        {
            messageObject = this.convertToObject("publish","message",message,promise);

            if (messageObject == null)
            {
                return;
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.publish(topic, messageObject, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                promise.resolve(true);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("publish",error,promise);
            }
        });

    }

    public void publishEvent(String resource, String resourceId, ReadableMap message, Promise promise)
    {
        JsonObject messageObject = null;

        //
        //  Turn the "object" parameter into a JsonObject
        //
        if (message != null)
        {
            messageObject = this.convertToObject("publishEvent","message",message,promise);

            if (messageObject == null)
            {
                return;
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.publishEvent(resource, resourceId, messageObject, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                promise.resolve(true);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("publishEvent",error,promise);
            }
        });
    }


    public void registerForPushNotifications(Activity activity, Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.registerForPushNotifications(activity, new VantiqAndroidLibrary.GenericCallback()
        {
            @Override
            public void invoke(Object obj)
            {
                JsonElement je = (JsonElement) obj;
                sendEvent(VantiqReactModule.INSTANCE.reactContext, "pushNotification", (WritableMap) convertJsonElementToReturnable(je));
            }
        },
        new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                //JsonArray ja = (JsonArray) result;
                promise.resolve(true);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("registerForPushNotifications",error,promise);
            }
        });
    }

    public void registerSupportedEvents(ReadableArray eventNames)
    {
        ArrayList eventNamesArray = null;

        if (eventNames != null)
        {
            eventNamesArray = eventNames.toArrayList();
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.registerSupportedEvents(eventNamesArray);
    }


    public void select(String type, ReadableArray props, ReadableMap where, ReadableMap sort, double limit, Promise promise)
    {
        ArrayList propsArray = null;

        if (props != null)
        {
            propsArray = props.toArrayList();
        }

        JsonObject whereObject = null;

        //
        //  Turn the "where" parameter into a JsonObject
        //
        if (where != null)
        {
            whereObject = this.convertToObject("select","where",where,promise);

            if (whereObject == null)
            {
                return;
            }
        }

        SortSpec sortSpec = null;

        //
        //  Turn the "sort" parameter into a SortSpec
        //
        if (sort != null)
        {
            JsonObject sortJsonObject = this.convertToObject("select","sort",sort,promise);

            if (sortJsonObject == null)
            {
                return;
            }

            Iterator<String> jsonIterator = sortJsonObject.keySet().iterator();

            while (jsonIterator.hasNext())
            {
                String property = jsonIterator.next();
                Object value = sortJsonObject.get(property);

                if (value instanceof JsonPrimitive)
                {
                    int descending = ((JsonPrimitive) value).getAsInt();
                    sortSpec = new SortSpec(property, (descending < 0 ? true : false));
                    break;
                }
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.select(type, propsArray, whereObject, sortSpec, limit, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonArray ja = (JsonArray) result;

                WritableArray wa = (WritableArray)convertJsonElementToReturnable(ja);
                promise.resolve(wa);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("select",error,promise);
            }
        });
    }

    public void selectOne(String type, String id, Promise promise)
    {
        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.selectOne(type, id, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonArray ja = (JsonArray) result;

                WritableArray wa = (WritableArray) convertJsonElementToReturnable(ja);
                promise.resolve(wa);
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("selectOne", error, promise);
            }
        });
    }

    public void update(String type, String id, ReadableMap object, Promise promise)
    {
        JsonObject objectToUpdate = null;

        //
        //  Turn the "object" parameter into a JsonObject
        //
        if (object != null)
        {
            objectToUpdate = this.convertToObject("update","object",object,promise);

            if (objectToUpdate == null)
            {
                return;
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.update(type, id, objectToUpdate, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonObject jo = (JsonObject) result;
                promise.resolve((WritableMap) convertJsonElementToReturnable(jo));
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("update",error,promise);
            }
        });
    }

    public void upsert(String type, ReadableMap object, Promise promise)
    {
        JsonObject objectToUpdate = null;

        //
        //  Turn the "object" parameter into a JsonObject
        //
        if (object != null)
        {
            objectToUpdate = this.convertToObject("update","object",object,promise);

            if (objectToUpdate == null)
            {
                return;
            }
        }

        final VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;

        val.upsert(type, objectToUpdate, new VantiqAndroidLibrary.ResponseListener()
        {
            @Override
            public void resolve(Object result)
            {
                JsonObject jo = (JsonObject) result;
                promise.resolve((WritableMap) convertJsonElementToReturnable(jo));
            }

            @Override
            public void reject(JsonObject error)
            {
                rejectErrorObject("upsert",error,promise);
            }
        });
    }








}
