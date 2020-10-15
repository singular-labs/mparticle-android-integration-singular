package com.singular.sdk;

import org.json.JSONObject;

public class Singular {
    private static boolean acceptEventJson;
    private static boolean acceptEvent;

    public static boolean eventJSON(String string, JSONObject json) {
        return acceptEventJson;
    }

    public static boolean event(String string) {
        return acceptEvent;
    }

    public static void setAcceptEventJson(boolean acceptEvent) {
        Singular.acceptEventJson = acceptEvent;
    }

    public static void setAcceptEvent(boolean acceptEvent) {
        Singular.acceptEvent = acceptEvent;
    }
}
