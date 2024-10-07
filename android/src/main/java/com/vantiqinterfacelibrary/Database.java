package com.vantiqinterfacelibrary;

import android.os.Bundle;
import android.os.Parcelable;
import com.facebook.react.bridge.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.vantiqinterfacelibrary.misc.VLog;
import io.vantiq.androidlib.VantiqAndroidLibrary;
import io.vantiq.androidlib.misc.Account;
import io.vantiq.client.SortSpec;
import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Database
{
    private static final String TAG = "Database";

    VantiqInterfaceLibraryModule vilm;

    public Database(VantiqInterfaceLibraryModule _vilm)
    {
        vilm = _vilm;
    }

    private void reject(String errorCode, String errorMsg, Promise promise)
    {
        VLog.e(TAG, "REJECT: " + errorCode + " " + errorMsg);
        WritableMap map = Arguments.createMap();
        map.putString("errorMsg", errorMsg);
        promise.reject(errorCode, errorMsg, map);
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

    public void selectOne(String type, String id, Promise promise)
    {
        VLog.i(TAG, "selectOne");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.selectOne(type, id);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("SELECTONEFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("SELECTONEFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("SELECTONE401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void select(String type, ReadableArray props, String where, String sort, double limit, Promise promise)
    {
        VLog.i(TAG, "select");

        if (!validate())
        {
            VLog.i(TAG, "Access Token INVALID");
        }
        else
        {
            VLog.i(TAG, "Access Token Valid");
        }

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        ArrayList propsArray = null;

        if (props != null)
        {
            propsArray = props.toArrayList();
        }

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject whereObject = null;

        if (where != null)
        {
            try
            {
                whereObject = new JsonParser().parse(where).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        //
        //  Turn the "sort" parameter into a SortSpec
        //
        SortSpec sortSpec = null;

        if (sort != null)
        {
            try
            {
                JsonObject sortJsonObject = new JsonParser().parse(sort).getAsJsonObject();

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
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.select(type, propsArray, whereObject, sortSpec, (long) limit);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("SELECTFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("SELECTFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            List<JsonObject> results = (List<JsonObject>) body;

            VLog.i(TAG, "Items returned: " + results.size());
            WritableArray wa = Arguments.createArray();

            for (int i = 0; i < results.size(); i++)
            {
                JsonObject jo = results.get(i);
                wa.pushMap(this.convertToWritableMap(jo));
            }
            promise.resolve(wa);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("SELECT401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }


    public void count(String type, String where, Promise promise)
    {
        VLog.i(TAG, "count");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject whereObject = null;

        if (where != null)
        {
            try
            {
                whereObject = new JsonParser().parse(where).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.count(type, whereObject);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("COUNTFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("COUNTFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            int count = ((Integer) body).intValue();
            VLog.i(TAG, "Items returned: " + count);
            promise.resolve(count);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("COUNT401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void insert(String type, String object, Promise promise)
    {
        VLog.i(TAG, "insert");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject objectToInsert = null;

        if (object != null)
        {
            try
            {
                objectToInsert = new JsonParser().parse(object).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.insert(type, objectToInsert);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("INSERTFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("INSERTFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(this.convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("INSERT401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void update(String type, String id, String object, Promise promise)
    {
        VLog.i(TAG, "update");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject objectToInsert = null;

        if (object != null)
        {
            try
            {
                objectToInsert = new JsonParser().parse(object).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.update(type, id, objectToInsert);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("UPDATEFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("UPDATEFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(this.convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("UPDATE401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void upsert(String type, String object, Promise promise)
    {
        VLog.i(TAG, "insert");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject objectToInsert = null;

        if (object != null)
        {
            try
            {
                objectToInsert = new JsonParser().parse(object).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.upsert(type, objectToInsert);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("UPSERTFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("UPSERTFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(this.convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("UPSERT401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void deleteOne(String type, String id, Promise promise)
    {
        VLog.i(TAG, "deleteOne");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.deleteOne(type, id);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("DELETEONEFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("DELETEONEFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("DELETEONE401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void delete(String type, String where, Promise promise)
    {
        VLog.i(TAG, "delete");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject whereObject = null;

        if (where != null)
        {
            try
            {
                whereObject = new JsonParser().parse(where).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.delete(type, whereObject);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("DELETEFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("DELETEFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("COUNT401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void execute(String procedureName, String params, Promise promise)
    {
        VLog.i(TAG, "execute");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject paramsAsObject = null;

        if (params != null)
        {
            try
            {
                paramsAsObject = new JsonParser().parse(params).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }
        else
        {
            paramsAsObject = new JsonObject();
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.execute(procedureName, paramsAsObject);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("EXECUTEFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("EXECUTEFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonElement je = (JsonElement) body;

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
                WritableMap wm = this.convertToWritableMap((JsonObject) je);
                promise.resolve(wm);
            }
            else if (je instanceof JsonArray)
            {
                WritableArray wa = this.convertToWritableArray((JsonArray) je);
                promise.resolve(wa);
            }
            else
            {
                promise.resolve(null);
            }
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("EXECUTE401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void publish(String topic, String message, Promise promise)
    {
        VLog.i(TAG, "publish");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject messageObject = null;

        if (message != null)
        {
            try
            {
                messageObject = new JsonParser().parse(message).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        try
        {
            vr = vantiqSDK.publish(Vantiq.SystemResources.TOPICS.value(), topic, messageObject);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("PUBLISHFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("PUBLISHFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("PUBLISH401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public void publishEvent(String resource, String event, String resourceId, String message, Promise promise)
    {
        VLog.i(TAG, "publishEvent");

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        //
        //  Turn the "where" parameter into a JsonObject
        //
        JsonObject messageObject = null;

        if (message != null)
        {
            try
            {
                messageObject = new JsonParser().parse(message).getAsJsonObject();
            }
            catch (Exception e)
            {
                WritableMap map = Arguments.createMap();
                String errorMessage = e.getLocalizedMessage();
                map.putString("errorMsg", errorMessage);
                promise.reject("PARSEFAILED", errorMessage, map);
                return;
            }
        }

        VantiqResponse vr = null;
        String exMessage = null;

        String id = event + "/" + resourceId;

        try
        {
            vr = vantiqSDK.publish(resource, id, messageObject);
        }
        catch (Exception ex)
        {
            exMessage = ex.getLocalizedMessage();
        }

        if (exMessage != null)
        {
            this.reject("PUBLISHFAILED",exMessage, promise);
        }
        else if (vr.hasException())
        {
            Throwable ex = vr.getException();
            this.reject("PUBLISHFAILED",ex.getLocalizedMessage(), promise);
        }
        //
        //  We don't really know or care why this request failed, just that it did. Keep trying with
        //  other users in the same namespace until we find one that works.
        //
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject("PUBLISH401","Access Token invalid", promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.reject(ve.getCode(),ve.getMessage(), promise);
                break;
            }
        }
    }

    public boolean validate()
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;
        String accessToken = a.getAccessToken();

        if (accessToken != null)
        {
            Vantiq vantiqSDK = new Vantiq(a.getServer());
            vantiqSDK.setAccessToken(accessToken);

            ArrayList<String> props = new ArrayList();
            props.add("username");

            try
            {
                VantiqResponse vr = vantiqSDK.select("system.users", props,null, null);

                if (vr.isSuccess())
                {
                    ArrayList ary = (ArrayList) vr.getBody();

                    if (ary.size() == 1)
                    {
                        JsonObject jo = (JsonObject) ary.get(0);
                        return (true);
                    }
                }
            }
            catch (Exception ex)
            {
            }
        }

        return false;
    }

}
