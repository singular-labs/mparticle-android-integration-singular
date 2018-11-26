package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.mparticle.AttributionResult;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.singular.sdk.DeferredDeepLinkHandler;
import com.singular.sdk.Singular;
import com.singular.sdk.SingularConfig;
import com.singular.sdk.SingularInstallReceiver;
import com.singular.sdk.internal.SingularLog;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class SingularKit extends KitIntegration implements
        KitIntegration.ActivityListener,
        KitIntegration.EventListener,
        KitIntegration.PushListener,
        KitIntegration.CommerceListener,
        KitIntegration.AttributeListener,
        DeferredDeepLinkHandler {

    //region Members

    // Config Consts
    private static final String API_KEY = "apiKey";
    private static final String API_SECRET = "secret";
    private static final String DDL_TIME_OUT = "ddlTimeout";
    private static final String KIT_NAME = "Singular";

    // User Attribute Consts
    private static final String USER_AGE_KEY = "age";
    private static final String USER_GENDER_KEY = "gender";


    private SingularLog logger = SingularLog.getLogger(Singular.class.getSimpleName());

    //endregion

    //region Kit Integration Implementation

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        // Returning the reporting message to state that the method was successful and
        // Preventing from the mParticle Kit to retry to activate to method.
        List<ReportingMessage> messages = new ArrayList<>();
        if (Singular.init(context, buildSingularConfig(settings))) {

            messages.add(new ReportingMessage(this,
                    ReportingMessage.MessageType.APP_STATE_TRANSITION,
                    System.currentTimeMillis(), null));
        }

        return messages;
    }

    public SingularConfig buildSingularConfig(Map<String, String> settings) {
        try {
            String singularKey = settings.get(API_KEY);
            String singularSecret = settings.get(API_SECRET);

            // Getting the DDL timeout from the settings. If does not exist, use 60(S) as default.
            String ddlTimeout = settings.get(DDL_TIME_OUT);
            long ddlHandlerTimeoutSec = 60L;
            if (!KitUtils.isEmpty(ddlTimeout)) {
                try {
                    ddlHandlerTimeoutSec = Long.parseLong(ddlTimeout);
                } catch (Exception unableToGetDDLTimeout) {
                }
            }

            SingularConfig config = new SingularConfig(singularKey, singularSecret);
            config.withDDLTimeoutInSec(ddlHandlerTimeoutSec);
            config.withDDLHandler(this);

            // TODO: Find out whats going on here. Speculation:
            // Checking if the app was opened by clicking on an URI. If so, adding it to the config.
            Uri openUri = MParticle.getInstance().getAppStateManager().getLaunchUri();
            if (null != openUri) {
                config.withOpenURI(openUri);
            }

            // If the environment is in development mode, enable logging.
            if (MParticle.getInstance().getEnvironment() == MParticle.Environment.Development) {
                config.withLoggingEnabled();
                config.withLogLevel(Log.DEBUG);
            }

            return config;

        } catch (Exception ex) {
            logger.error("Can't build Singular Config in the mParticle Kit", ex);
            return null;
        }
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean b) {
        return null;
    }

    @Override
    public String getName() {
        return KIT_NAME;
    }

    @Override
    public void setInstallReferrer(Intent intent) {
        new SingularInstallReceiver().onReceive(getContext(), intent);
    }

    //endregion

    //region Activity Listener Implementation

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
        Singular.onActivityResumed();
        // TODO: Check the meaning of returning null here
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
        Singular.onActivityPaused();
        // TODO: Check the meaning of returning null here
        return null;
    }

    //region Unimplemented (Empty Methods)

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle bundle) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
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

    //endregion

    //endregion

    //region Event Listener Implementation

    @Override
    public List<ReportingMessage> logEvent(MPEvent mpEvent) {
        List<ReportingMessage> messages = new ArrayList<>();

        if (mpEvent != null) {

            String eventName = mpEvent.getEventName();
            Map eventInfo = mpEvent.getInfo();

            // Logging the event with the Singular API
            boolean eventStatus;
            if (eventInfo != null && eventInfo.size() > 0) {
                eventStatus = Singular.eventJSON(eventName, new JSONObject(eventInfo));
            } else {
                eventStatus = Singular.event(eventName);
            }

            // If the Singular event logging was successful, return the message to the mParticle Kit
            // So it won't retry the event
            if (eventStatus) {
                messages.add(ReportingMessage.fromEvent(this, mpEvent));
            }
        }

        return messages;
    }

    //region Unimplemented (Empty Methods)

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
    public List<ReportingMessage> logScreen(String s, Map<String, String> map) {
        return null;
    }

    //endregion

    //endregion

    //region Push Listener Implementation

    @Override
    public boolean onPushRegistration(String deviceToken, String senderId) {
        // Saving the registration token to determine when the user uninstalls the app.
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

    //region Unimplemented (Empty Methods)

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        return false;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {
    }

    //endregion

    //endregion

    //region Commerce Listener Implementation

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        if (commerceEvent.getProductAction().equals(Product.PURCHASE)) {
            return handlePurchaseEvents(commerceEvent);
        } else {
            return handleNonPurchaseEvents(commerceEvent);
        }
    }

    private List<ReportingMessage> handlePurchaseEvents(CommerceEvent commerceEvent) {
        List<ReportingMessage> messages = new ArrayList<>();

        for (Product product : commerceEvent.getProducts()) {
            Singular.revenue(commerceEvent.getCurrency(),
                    product.getTotalAmount(),
                    product.getSku(),
                    product.getName(),
                    product.getCategory(),
                    (int) product.getQuantity(),
                    product.getUnitPrice());
        }

        messages.add(ReportingMessage.fromEvent(this, commerceEvent));

        return messages;
    }

    private List<ReportingMessage> handleNonPurchaseEvents(CommerceEvent commerceEvent) {
        List<ReportingMessage> messages = new ArrayList<>();

        // Getting the mParticle events from the commerce event
        List<MPEvent> eventList = CommerceEventUtils.expand(commerceEvent);
        if (eventList != null) {
            for (MPEvent event : eventList) {
                try {
                    for (ReportingMessage message: logEvent(event)){
                        messages.add(message);
                    }
                } catch (Exception e) {
                    Logger.warning("Failed to call logCustomEvent to Singular kit: " + e.toString());
                }
            }
        }

        messages.add(ReportingMessage.fromEvent(this, commerceEvent));

        return messages;
    }

    //region Unimplemented (Empty Methods)

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal,
                                                 BigDecimal bigDecimal1,
                                                 String s,
                                                 Map<String, String> map) {
        return null;
    }

    //endregion

    //endregion

    //region Deprecated Attribute Listener

    @Override
    public void setUserAttribute(String key, String value) {
        // TODO: Debug these lines to understand the code
        Map<String, String> map = new HashMap<>();
        if (MParticle.UserAttributes.AGE.equals(key)) {
            map.put(USER_AGE_KEY, value);
        } else if (MParticle.UserAttributes.GENDER.equals(key)) {
            if (value.contains("fe")) {
                map.put(USER_GENDER_KEY, "f");
            } else {
                map.put(USER_GENDER_KEY, "m");
            }
        }
        if (map != null && !map.isEmpty()) {
            Singular.eventJSON("UserAttribute", new JSONObject(map));
        }
    }

    @Override
    public void setUserAttributeList(String s, List<String> list) {

    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }

    @Override
    public void setAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1) {

    }

    @Override
    public void removeUserAttribute(String s) {

    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String s) {

    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {

    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    //endregion

    //region DDL Handler Implementation

    @Override
    public void handleLink(String link) {
        if (!KitUtils.isEmpty(link)) {
            AttributionResult attributionResult = new AttributionResult();
            attributionResult.setServiceProviderId(MParticle.ServiceProviders.SINGULAR);
            attributionResult.setLink(link);
            getKitManager().onResult(attributionResult);
        }
    }

    //endregion
}