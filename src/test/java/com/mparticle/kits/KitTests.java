package com.mparticle.kits;

import android.util.Log;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.internal.MPUtility;
import com.singular.sdk.Singular;
import com.singular.sdk.SingularConfig;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
public class KitTests {

    //region Tests Setup

    private MockSingularKit kit;
    private Map<String, String> settings;
    private SingularConfig config;

    private static final String API_KEY = "apiKey";
    private static final String API_SECRET = "secret";
    private static final String DDL_TIME_OUT = "ddlTimeout";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private ReportingMessage reportingMessage;

    @Before
    public void setUp() {
        //MockitoAnnotations.initMocks(this);

        kit = new MockSingularKit();
        settings = new HashMap<>();
        settings.put(API_KEY, "Test");
        settings.put(API_SECRET, "Test");
    }

    //endregion

    //region Config Tests

    @Test
    public void buildConfigWithoutSettings() {
        SingularConfig testConfig = kit.buildSingularConfig(null);

        Assert.assertNull(testConfig);
    }

    @Test
    public void buildConfigWithEmptySettings() {
        SingularConfig testConfig =
                kit.buildSingularConfig(new HashMap<String, String>());

        Assert.assertNull(testConfig);
    }

    //endregion

    //region Log MPEvent Tests

    @Test
    public void logEventWithInfo() {
        List<ReportingMessage> result = null;
        try {
            // Creating the event
            JSONObject eventJson = new JSONObject();
            eventJson.put("eventName", "Testing");
            eventJson.put("eventType", "Unknown");
            MPEvent event = MPEvent.Builder.parseString(eventJson.toString()).build();

            // Mocking the Kit Methods
            Singular.setAcceptEvent(true);

            result = kit.logEvent(event);
        } catch (Exception e) {
            e.printStackTrace();

            Assert.fail(String.
                    format("logEventWithInfo failed with exception message:%s", e.getMessage()));
        } finally {
            Assert.assertNotNull(result);
            Assert.assertTrue(!result.isEmpty());
        }
    }

    @Test
    public void logEventWithoutInfo() {
        List<ReportingMessage> result = null;
        try {
            // Creating the event
            JSONObject eventJson = new JSONObject();
            eventJson.put("eventName", "Testing");
            eventJson.put("eventType", "Unknown");
            MPEvent event = MPEvent.Builder.parseString(eventJson.toString()).build();
            event.getCustomAttributes().clear();

            // Mocking the kit Methods
            Singular.setAcceptEvent(true);

            result = kit.logEvent(event);
        } catch (Exception e) {
            e.printStackTrace();

            Assert.fail(String.
                    format("logEventWithInfo failed with exception message:%s", e.getMessage()));
        } finally {
            Assert.assertNotNull(result);
            Assert.assertTrue(!result.isEmpty());
        }
    }

    @Test
    public void logEventWithInvalidData() {
        MPEvent event = null;
        List<ReportingMessage> result = kit.logEvent(event);

        Assert.assertTrue(result.isEmpty());
    }

    //endregion

    //region Log CommerceEvent Tests

    @Test
    public void logCommercePurchaseEvents() {
        List<ReportingMessage> result = null;

        try {
            CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.PURCHASE, new Product.Builder("Testing", "Unknown", 2.0)
                    .quantity(1.0)
                    .category("Category")
                    .build())
                    .addProduct(new Product.Builder("Unknown", "b", 1.0).build())
                    .build();

            result = kit.logEvent(commerceEvent);

        } catch (Exception e) {
            e.printStackTrace();

            Assert.fail(String.
                    format("logCommercePurchaseEvents failed with exception message:%s", e.getMessage()));
        } finally {
            Assert.assertNotNull(result);
            Assert.assertTrue(!result.isEmpty());
        }
    }

    @Test
    public void logCommerceNonPurchaseEvents() {
        List<ReportingMessage> result = null;

        try {
            CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.DETAIL, new Product.Builder("Testing", "Unknown", 2.0)
                    .quantity(1.0)
                    .category("Category")
                    .build())
                    .addProduct(new Product.Builder("Unknown", "b", 1.0).build())
                    .build();


            result = kit.logEvent(commerceEvent);

        } catch (Exception e) {
            e.printStackTrace();

            Assert.fail(String.
                    format("logCommerceNonPurchaseEvents failed with exception message:%s", e.getMessage()));
        } finally {
            Assert.assertNotNull(result);
            Assert.assertTrue(!result.isEmpty());
        }
    }

    //endregion

    //region MParticle Kit Factory Tests

    @Test
    public void isSingularIntegrationInFactory() throws Exception {
        KitIntegrationFactory factory = new KitIntegrationFactory();
        Map<Integer, String> integrations = factory.getKnownIntegrations();
        String className = new SingularKit().getClass().getName();
        for (Map.Entry<Integer, String> entry : integrations.entrySet()) {
            if (entry.getValue().equals(className)) {
                return;
            }
        }
        fail(className + " not found as a known integration.");
    }

    //endregion
}