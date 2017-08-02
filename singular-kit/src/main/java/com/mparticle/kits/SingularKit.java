package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.mparticle.DeepLinkListener;
import com.mparticle.DeepLinkResult;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.internal.MPUtility;
import com.singular.sdk.DeferredDeepLinkHandler;
import com.singular.sdk.Singular;
import com.singular.sdk.SingularConfig;

import java.util.List;
import java.util.Map;


public class SingularKit extends KitIntegration implements KitIntegration.ActivityListener, KitIntegration.EventListener, KitIntegration.PushListener, DeferredDeepLinkHandler {

    private static final String API_KEY = "apiKey";
    private static final String API_SECRET = "secret";
    private static final String DDL_TIME_OUT = "ddlTimeout";
    String apsalarKey;
    String apsalarSecret;
    SingularConfig config;
    long DDL_HANDLER_TIMEOUT_SEC = 60L;

    Context context;

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        apsalarKey = settings.get(API_KEY);
        apsalarSecret = settings.get(API_SECRET);
        String ddlTimeout = settings.get(DDL_TIME_OUT);
        if (!TextUtils.isEmpty(ddlTimeout)) {
            try {
                DDL_HANDLER_TIMEOUT_SEC = Long.parseLong(ddlTimeout);
            } catch (Exception unableToGetDDLTimeout) {
            }
        }
        this.context = context;
        config = new SingularConfig(apsalarKey, apsalarSecret);
        config.withDDLTimeoutInSec(DDL_HANDLER_TIMEOUT_SEC);
        config.withDDLHandler(this);
        Singular.init(context, config);
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean b) {
        return null;
    }

    @Override
    public String getName() {
        return "Singular";
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle bundle) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
        Singular.onActivityResumed();
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
        Singular.onActivityPaused();
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityDestroyed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception e, Map<String, String> map, String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent mpEvent) {
        String eventName = mpEvent.getEventName();
        Map eventInfo = mpEvent.getInfo();
        Singular.event(eventName, eventInfo);
        return null;
    }

    @Override
    public List<ReportingMessage> logScreen(String s, Map<String, String> map) {
        return null;
    }

    @Override
    public void handleLink(String link) {
        DeepLinkListener deepLinkListener = MParticle.getInstance().getDeepLinkListener();
        if (deepLinkListener != null) {
            DeepLinkResult deepLinkResult = new DeepLinkResult();
            deepLinkResult.setServiceProviderId(MParticle.ServiceProviders.SINGULAR);
            deepLinkResult.setLink(link);
            deepLinkListener.onResult(deepLinkResult);
        }
    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        return false;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {

    }

    @Override
    public boolean onPushRegistration(String deviceToken, String senderId) {
        try {
            switch (MPUtility.getAvailableInstanceId()) {
                case GCM:
                    Singular.setGCMDeviceToken(deviceToken);
                    break;
                case FCM:
                    Singular.setFCMDeviceToken(deviceToken);
                    break;
            }
        } catch (Exception unableToSetDeviceToken) {
            return false;
        }
        return true;
    }
}