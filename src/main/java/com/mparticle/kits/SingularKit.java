package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mparticle.AttributionResult;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.consent.ConsentState;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.singular.sdk.DeferredDeepLinkHandler;
import com.singular.sdk.Singular;
import com.singular.sdk.SingularConfig;
import com.singular.sdk.SingularInstallReceiver;
import com.singular.sdk.SingularLinkHandler;
import com.singular.sdk.SingularLinkParams;
import com.singular.sdk.internal.SingularLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SingularKit extends KitIntegration implements
        KitIntegration.ActivityListener,
        KitIntegration.EventListener,
        KitIntegration.PushListener,
        KitIntegration.CommerceListener,
        KitIntegration.ApplicationStateListener,
        KitIntegration.UserAttributeListener,
        KitIntegration.AttributeListener {

    //region Members

    // Config Consts
    private static final String API_KEY = "apiKey";
    private static final String API_SECRET = "secret";
    private static final String DDL_TIMEOUT = "ddlTimeout";
    private static final String KIT_NAME = "Singular";

    // User Attribute Consts
    private static final String USER_AGE_KEY = "age";
    private static final String USER_GENDER_KEY = "gender";

    // Singular Link Consts
    private static final String PASSTHROUGH = "passthrough";
    private static final String IS_DEFERRED = "is_deferred";

    // Wrapper Consts
    private static final String MPARTICLE_WRAPPER_NAME = "mParticle";
    private static final String MPARTICLE_WRAPPER_VERSION = "1.0.1";

    private static Map<String, String> singularSettings;

    private SingularLog logger = SingularLog.getLogger(Singular.class.getSimpleName());

    //endregion

    //region Kit Integration Implementation

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        // Returning the reporting message to state that the method was successful and
        // Preventing from the mParticle Kit to retry to activate to method.
        List<ReportingMessage> messages = new ArrayList<>();
        if (Singular.init(context, buildSingularConfig(settings))) {
            singularSettings = settings;
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
            String ddlTimeout = settings.get(DDL_TIMEOUT);
            long ddlHandlerTimeoutSec = 60L;
            if (!KitUtils.isEmpty(ddlTimeout)) {
                try {
                    ddlHandlerTimeoutSec = Long.parseLong(ddlTimeout);
                } catch (Exception unableToGetDDLTimeout) {
                }
            }

            SingularConfig config = new SingularConfig(singularKey, singularSecret);
            config.withDDLTimeoutInSec(ddlHandlerTimeoutSec);

            Activity activity = getCurrentActivity().get();

            if (activity != null) {
                Intent intent = activity.getIntent();

                config.withSingularLink(intent, new SingularLinkHandler() {
                    @Override
                    public void onResolved(SingularLinkParams singularLinkParams) {
                        AttributionResult attributionResult = new AttributionResult();
                        attributionResult.setServiceProviderId(MParticle.ServiceProviders.SINGULAR);
                        attributionResult.setLink(singularLinkParams.getDeeplink());
                        try {
                            JSONObject linkParams = new JSONObject();
                            linkParams.put(PASSTHROUGH, singularLinkParams.getPassthrough());
                            linkParams.put(IS_DEFERRED, singularLinkParams.isDeferred());
                            attributionResult.setParameters(linkParams);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        getKitManager().onResult(attributionResult);
                    }
                });
            }

            // If the environment is in development mode, enable logging.
            if (MPUtility.isDevEnv()) {
                config.withLoggingEnabled();
                config.withLogLevel(Log.DEBUG);
            }

            Singular.setWrapperNameAndVersion(MPARTICLE_WRAPPER_NAME, MPARTICLE_WRAPPER_VERSION);

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
    }

    //endregion

    //region Activity Listener Implementation

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
            Map eventInfo = mpEvent.getCustomAttributes();

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
        if (MPUtility.isFirebaseAvailable()) {
            Singular.setFCMDeviceToken(deviceToken);
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
                    for (ReportingMessage message : logEvent(event)) {
                        messages.add(message);
                    }
                } catch (Exception e) {
                    Logger.warning("Failed to call logCustomEvent to Singular kit: " + e.toString());
                }
            }
        }

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

        if (!map.isEmpty()) {
            Singular.eventJSON("UserAttribute", new JSONObject(map));
        }
    }

    @Override
    public void setUserAttributeList(String s, List<String> list) {

    }

    @Override
    public void onIncrementUserAttribute(String s, Number i, String s1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onRemoveUserAttribute(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttribute(String s, Object o, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserTag(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttributeList(String s, List<String> list, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }

    @Override
    public void onConsentStateUpdated(ConsentState consentState, ConsentState consentState1, FilteredMParticleUser filteredMParticleUser) {
        if (consentState != null && consentState.getCCPAConsentState() != null) {
            Singular.limitDataSharing(consentState.getCCPAConsentState().isConsented());
        }
    }

    @Override
    public void setAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1) {

    }

    @Override
    public void removeUserAttribute(String s) {

    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String s) {
        if (identityType == MParticle.IdentityType.CustomerId) {
            Singular.setCustomUserId(s);
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (identityType == MParticle.IdentityType.CustomerId) {
            Singular.unsetCustomUserId();
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        Singular.unsetCustomUserId();
        List<ReportingMessage> messageList = new ArrayList<>();
        messageList.add(ReportingMessage.logoutMessage(this));
        return messageList;
    }

    @Override
    public void onApplicationForeground() {
        // Handling deeplinks when the application resumes from background
        Singular.init(getContext(), buildSingularConfig(singularSettings));
    }

    @Override
    public void onApplicationBackground() {
    }

    //endregion
}