package com.vantiqreact;

import com.facebook.react.bridge.*;
import com.google.gson.*;
import com.vantiqreact.misc.VLog;
import io.vantiq.androidlib.VantiqAndroidLibrary;
import io.vantiq.androidlib.misc.Account;
import io.vantiq.client.SortSpec;
import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

        WritableMap map = Arguments.createMap();
        map.putString("errorMsg", errorMsg);

        promise.reject(errorCode, errorMsg, map);
    }

    public void rejectException(String op, Throwable ex, Promise promise)
    {
        String errorMessage = ex.getLocalizedMessage();

        VLog.e(TAG, "REJECTEXCEPTION: " + op + "  msg=" + errorMessage);

        WritableMap map = Arguments.createMap();
        map.putString("errorMsg", errorMessage);

        promise.reject(Error.veOSError, errorMessage, map);
    }

    public void rejectParse(String op, String propertyName, Exception e, Promise promise)
    {
        String errorMessage = e.getLocalizedMessage();
        errorMessage = "Invalid JSON in '" + propertyName + "': " + errorMessage;

        VLog.e(TAG, "REJECTPARSE: op=" + op + " msg=" + errorMessage);

        WritableMap map = Arguments.createMap();
        map.putString("errorMsg", errorMessage);

        promise.reject(Error.veJsonParseError, errorMessage, map);
    }

    public void rejectVantiqError(String op, VantiqError ve, Promise promise)
    {
        String errorCode = ve.getCode();
        String errorMessage = ve.getMessage();

        VLog.e(TAG, "REJECTVANTIQERROR: op=" + op + " code=" + errorCode + " msg=" + errorMessage);

        WritableMap map = Arguments.createMap();
        map.putString("errorMsg", errorMessage);

        promise.reject(Error.veRESTError, errorMessage, map);
    }

    public void reject401(String op, Promise promise)
    {
        VLog.e(TAG, "REJECT401: op=" + op);

        WritableMap map = Arguments.createMap();
        String errorMessage = "Access Token Invalid";
        map.putString("errorMsg", errorMessage);
        map.putInt("httpStatus", 401);

        promise.reject(Error.veNotAuthorized, errorMessage, map);
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
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.selectOne(type, id);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("selectOne",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("selectOne",vr.getException(), promise);
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
            this.reject401("selectOne",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("selectOne",ve,promise);
                break;
            }
        }
    }



    public void select(String type, ReadableArray props, ReadableMap where, ReadableMap sort, double limit, Promise promise)
    {
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

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.select(type, propsArray, whereObject, sortSpec, (long) limit);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("select",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("select",vr.getException(), promise);
        }
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
            this.reject401("select",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("select",ve,promise);
                break;
            }
        }
    }


    public void count(String type, ReadableMap where, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

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

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.count(type, whereObject);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("count",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("count",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            int count = ((Integer) body).intValue();
            VLog.i(TAG, "Items returned: " + count);
            promise.resolve(count);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("count",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("count",ve,promise);
                break;
            }
        }
    }

    public void insert(String type, ReadableMap object, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

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

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.insert(type, objectToInsert);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("insert",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("insert",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(this.convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("insert",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("insert",ve,promise);
                break;
            }
        }
    }

    public void update(String type, String id, ReadableMap object, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

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

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.update(type, id, objectToUpdate);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("update",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("update",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(this.convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("update",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("update",ve,promise);
                break;
            }
        }
    }

    public void upsert(String type, ReadableMap object, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        JsonObject objectToUpsert = null;

        //
        //  Turn the "object" parameter into a JsonObject
        //
        if (object != null)
        {
            objectToUpsert = this.convertToObject("upsert","object",object,promise);

            if (objectToUpsert == null)
            {
                return;
            }
        }

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.upsert(type, objectToUpsert);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("upsert",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("upsert",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            Object body = vr.getBody();
            JsonObject jo = (JsonObject) body;
            promise.resolve(this.convertToWritableMap(jo));
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("upsert",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("upsert",ve,promise);
                break;
            }
        }
    }

    public void deleteOne(String type, String id, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.deleteOne(type, id);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("deleteOne",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("deleteOne",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("deleteOne",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("deleteOne",ve,promise);
                break;
            }
        }
    }

    public void delete(String type, ReadableMap where, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        JsonObject whereObject = null;

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

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.delete(type, whereObject);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("delete",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("delete",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("delete",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("delete",ve,promise);
                break;
            }
        }
    }

    public void executeByPosition(String procedureName, ReadableArray paramsArray, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonArray
        //
        JsonArray paramsAsArray =  this.convertToArray("executeByPosition", "params", paramsArray, promise);

        this.execute(procedureName, paramsAsArray, promise);
    }

    public void executeByName(String procedureName, ReadableMap paramsMap, Promise promise)
    {
        //
        //  Turn the "params" parameter into a JsonObject
        //;
        JsonObject paramsAsObject = this.convertToObject("executeByName","params",paramsMap,promise);

        this.execute(procedureName, paramsAsObject, promise);
    }

    private void execute(String procedureName, Object paramsAsObject, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.execute(procedureName, paramsAsObject);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("execute",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("execute",vr.getException(), promise);
        }
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
            this.reject401("execute",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("execute",ve,promise);
                break;
            }
        }
    }

    public void publish(String topic, ReadableMap message, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        JsonObject messageObject = null;

        //
        //  Turn the "message" parameter into a JsonObject
        //
        if (message != null)
        {
            messageObject = this.convertToObject("publish","message",message,promise);

            if (messageObject == null)
            {
                return;
            }
        }

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.publish(Vantiq.SystemResources.TOPICS.value(), topic, messageObject);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("publish",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("publish",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("publish",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("publish",ve,promise);
                break;
            }
        }
    }

    public void publishEvent(String resource, String resourceId, ReadableMap message, Promise promise)
    {
        VantiqAndroidLibrary val = VantiqAndroidLibrary.INSTANCE;
        Account a = val.account;

        Vantiq vantiqSDK = new Vantiq(a.getServer());
        vantiqSDK.setAccessToken(a.getAccessToken());
        vantiqSDK.setUsername(a.getUsername());

        JsonObject messageObject = null;

        //
        //  Turn the "message" parameter into a JsonObject
        //
        if (message != null)
        {
            messageObject = this.convertToObject("publishEvent","message",message,promise);

            if (messageObject == null)
            {
                return;
            }
        }

        VantiqResponse vr = null;
        Exception theException = null;

        try
        {
            vr = vantiqSDK.publish(resource, resourceId, messageObject);
        }
        catch (Exception ex)
        {
            theException = ex;
        }

        if (theException != null)
        {
            this.rejectException("publishEvent",theException, promise);
        }
        else if (vr.hasException())
        {
            this.rejectException("publishEvent",vr.getException(), promise);
        }
        else if (vr.isSuccess())
        {
            promise.resolve(true);
        }
        else if (vr.getStatusCode() == 401)
        {
            this.reject401("publishEvent",promise);
        }
        else
        {
            List<VantiqError> ves = vr.getErrors();
            for (int k = 0; k < ves.size(); k++)
            {
                VantiqError ve = ves.get(k);
                this.rejectVantiqError("publishEvent",ve,promise);
                break;
            }
        }
    }


}
