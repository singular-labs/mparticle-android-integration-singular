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

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class SingularKit extends KitIntegration implements KitIntegration.ActivityListener, KitIntegration.EventListener, KitIntegration.PushListener, KitIntegration.CommerceListener, DeferredDeepLinkHandler, KitIntegration.AttributeListener {

    private static final String API_KEY = "apiKey";
    private static final String API_SECRET = "secret";
    private static final String DDL_TIME_OUT = "ddlTimeout";
    String singularKey;
    String singularSecret;
    SingularConfig config;
    long DDL_HANDLER_TIMEOUT_SEC = 60L;
    String currency;
    double amount;
    String productSKU;
    String productName;
    String productCategory;
    double productQuantity;
    double productPrice;

    boolean eventStatus;
    private String mLink;

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        singularKey = settings.get(API_KEY);
        singularSecret = settings.get(API_SECRET);
        String ddlTimeout = settings.get(DDL_TIME_OUT);
        if (!KitUtils.isEmpty(ddlTimeout)) {
            try {
                DDL_HANDLER_TIMEOUT_SEC = Long.parseLong(ddlTimeout);
            } catch (Exception unableToGetDDLTimeout) {
            }
        }
        config = new SingularConfig(singularKey, singularSecret);
        config.withDDLTimeoutInSec(DDL_HANDLER_TIMEOUT_SEC);
        config.withDDLHandler(this);
        Uri openUri = getKitManager().getLaunchUri();
        if (null != openUri) {
            config.withOpenURI(openUri);
        }
        if (MParticle.getInstance().getEnvironment() == MParticle.Environment.Development) {
            config.withLoggingEnabled();
            config.withLogLevel(Log.DEBUG);
        }
        Singular.init(context, config);
        List<ReportingMessage> messages = new ArrayList<ReportingMessage>();
        messages.add(new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null));
        return messages;
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
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        String eventName = mpEvent.getEventName();
        Map eventInfo = mpEvent.getInfo();
        if (eventInfo != null) {
            JSONObject params = new JSONObject(eventInfo);
            eventStatus = Singular.event(eventName, params.toString());
        } else {
            eventStatus = Singular.event(eventName);
        }
        if (eventStatus) {
            messages.add(ReportingMessage.fromEvent(this, mpEvent));
        }
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String s, Map<String, String> map) {
        return null;
    }

    @Override
    public void handleLink(String link) {
        mLink = link;
        if (!KitUtils.isEmpty(link)) {
            AttributionResult attributionResult = new AttributionResult();
            attributionResult.setServiceProviderId(MParticle.ServiceProviders.SINGULAR);
            attributionResult.setLink(link);
            getKitManager().onResult(attributionResult);
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

    @Override
    public void setInstallReferrer(Intent intent) {
        new SingularInstallReceiver().onReceive(getContext(), intent);
    }


    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal, BigDecimal bigDecimal1, String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        if (!commerceEvent.getProductAction().equals(Product.PURCHASE)) {
            List<MPEvent> eventList = CommerceEventUtils.expand(commerceEvent);
            if (eventList != null) {
                for (int i = 0; i < eventList.size(); i++) {
                    try {
                        logEvent(eventList.get(i));
                        messages.add(ReportingMessage.fromEvent(this, commerceEvent));
                    } catch (Exception e) {
                        Logger.warning("Failed to call logCustomEvent to Singular kit: " + e.toString());
                    }
                }
            }
        } else {
            if (!KitUtils.isEmpty(commerceEvent.getCurrency())) {
                currency = commerceEvent.getCurrency();
            }
            if (commerceEvent.getProducts().size() > 0) {
                List<Product> productList = commerceEvent.getProducts();
                for (Product product : productList) {
                    productPrice = product.getUnitPrice();
                    productQuantity = product.getQuantity();
                    if (!KitUtils.isEmpty(product.getSku())) {
                        productSKU = product.getSku();
                    }
                    if (!KitUtils.isEmpty(product.getCategory())) {
                        productCategory = product.getCategory();
                    }
                    productName = product.getName();
                    amount = product.getTotalAmount();
                    Singular.revenue(currency, amount, productSKU, productName, productCategory, (int) productQuantity, productPrice);
                }
                messages.add(ReportingMessage.fromEvent(this, commerceEvent));
            }
        }
        return messages;
    }

    @Override
    public void setUserAttribute(String key, String value) {
        Map<String, String> map = new HashMap<String, String>();
        if (MParticle.UserAttributes.AGE.equals(key)) {
            map.put("age", value);
        }  else if (MParticle.UserAttributes.GENDER.equals(key)) {
            if (value.contains("fe")) {
                map.put("gender", "f");
            } else {
                map.put("gender", "m");
            }
        }
        if (map != null && map.size() > 0) {
            JSONObject params = new JSONObject(map);
            Singular.event("UserAttribute", params.toString());
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
}