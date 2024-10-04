package com.vantiqinterfacelibrary;

import android.os.Bundle;
import android.os.Parcelable;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
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

            try
            {
                Bundle bundle = this.convertToBundle(jo);
                WritableMap map = Arguments.fromBundle(bundle);
                promise.resolve(map);
            }
            catch (JsonParseException ex)
            {
                WritableMap map = Arguments.createMap();
                map.putString("errorMsg", ex.getLocalizedMessage());
                promise.reject("SELECTFAILED", ex.getLocalizedMessage(), map);
            }
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

        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

/*
    VLog.i(TAG, "type=" + type);
    VLog.i(TAG, "props=" + (props != null ? props.toString() : "EMPTY"));
    VLog.i(TAG, "where=" + where);
    VLog.i(TAG, "sort=" + sort);
    VLog.i(TAG, "limit=" + limit);
*/

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
            WritableArray ary = Arguments.createArray();

            for (int i = 0; i < results.size(); i++)
            {
                JsonObject jo = results.get(i);

                try
                {
                    Bundle bundle = this.convertToBundle(jo);
                    WritableMap map = Arguments.fromBundle(bundle);
                    ary.pushMap(map);

                }
                catch (JsonParseException ex)
                {
                    this.reject("SELECTFAILED",ex.getLocalizedMessage(), promise);
                }
            }
            promise.resolve(ary);
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

    private void setPrimitive(Bundle bundle, String key, JsonElement je)
    {
        if (je instanceof JsonArray)
        {
            JsonArray jsonArray = (JsonArray) (Object) je;

            // Empty list, can't even figure out the type, assume an ArrayList<String>
            if (jsonArray.size() == 0)
            {
                ArrayList<String> stringArrayList = new ArrayList<String>();
                bundle.putStringArrayList(key, stringArrayList);
            }
            else
            {
                JsonElement firstJE = jsonArray.get(0);

                if (firstJE.isJsonObject())
                {
                    ArrayList<Parcelable> objectArrayList = new ArrayList<Parcelable>();
                    bundle.putParcelableArrayList(key, objectArrayList);

                    for (int i = 0; i < jsonArray.size(); i++)
                    {
                        JsonObject current = (JsonObject) jsonArray.get(i);

                        Bundle b = convertToBundle(current);
                        objectArrayList.add(b);
                    }
                }
                else if (firstJE.isJsonPrimitive())
                {
                    JsonPrimitive firstJP = (JsonPrimitive) firstJE;

                    if (firstJP.isBoolean())
                    {
                        boolean[] booleanArray = new boolean[jsonArray.size()];
                        bundle.putBooleanArray(key, booleanArray);

                        for (int i = 0; i < jsonArray.size(); i++)
                        {
                            JsonElement current = jsonArray.get(i);
                            booleanArray[i] = current.getAsBoolean();
                        }

                        bundle.putBooleanArray(key, booleanArray);
                    }
                    else if (firstJP.isNumber())
                    {
                        double[] doubleArray = new double[jsonArray.size()];
                        bundle.putDoubleArray(key, doubleArray);

                        for (int i = 0; i < jsonArray.size(); i++)
                        {
                            JsonElement current = jsonArray.get(i);
                            doubleArray[i] = current.getAsDouble();
                        }

                        bundle.putDoubleArray(key, doubleArray);
                    }
                    else if (firstJP.isString())
                    {
                        ArrayList<String> stringArrayList = new ArrayList<String>();
                        bundle.putStringArrayList(key, stringArrayList);

                        for (int i = 0; i < jsonArray.size(); i++)
                        {
                            JsonPrimitive current = (JsonPrimitive) jsonArray.get(i);
                            if (current.isString())
                            {
                                stringArrayList.add((current.getAsString()));
                            }
                            else
                            {
                                throw new IllegalArgumentException("Unexpected type in an array: " + current.getClass());
                            }
                        }

                        bundle.putStringArrayList(key, stringArrayList);
                    }
                    else
                    {
                        ArrayList<String> stringArrayList = new ArrayList<String>();
                        bundle.putStringArrayList(key, stringArrayList);
                    }
                }
                else
                {
                    ArrayList<String> stringArrayList = new ArrayList<String>();
                    bundle.putStringArrayList(key, stringArrayList);
                }
            }
        }
        else if (je instanceof JsonPrimitive)
        {
            JsonPrimitive jp = (JsonPrimitive) je;
            if (jp.isBoolean())
            {
                bundle.putBoolean(key, jp.getAsBoolean());
            }
            else if (jp.isNumber())
            {
                bundle.putDouble(key, jp.getAsDouble());
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

    private Bundle convertToBundle(JsonObject jsonObject) throws JsonParseException
    {
        Bundle bundle = new Bundle();

        @SuppressWarnings("unchecked")
        Iterator<String> jsonIterator = jsonObject.keySet().iterator();
        while (jsonIterator.hasNext())
        {
            String key = jsonIterator.next();
            Object value = jsonObject.get(key);

            if (value == null)
            {
                // Null is not supported.
                continue;
            }

            // Special case JsonObject as it's one way, on the return it would be Bundle.
            if (value instanceof JsonObject)
            {
                bundle.putBundle(key, convertToBundle((JsonObject) value));
                continue;
            }
            else if (value instanceof JsonArray)
            {
                this.setPrimitive(bundle, key, (JsonArray) (Object) value);
                continue;
            }
            else if (value instanceof JsonPrimitive)
            {
                JsonPrimitive jp = (JsonPrimitive) value;

                this.setPrimitive(bundle, key, jp);
            }
        }

        return bundle;
    }
}
