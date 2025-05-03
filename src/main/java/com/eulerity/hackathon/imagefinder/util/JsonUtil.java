package com.eulerity.hackathon.imagefinder.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility class for converting Java objects to their JSON string representation.
 */
public class JsonUtil {

    private static final Gson gson = new GsonBuilder().create();

    /**
     * Serializes the given object into a JSON string.
     *
     * @param obj the object to serialize
     * @return the JSON string representation of the object
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
