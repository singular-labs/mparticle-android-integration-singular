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

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@PrepareForTest({
        Singular.class,
        MParticle.class,
        MPUtility.class,
        ReportingMessage.class,
        CommerceEvent.class,
        CommerceEventUtils.class,
        Product.class})
public class KitTests {

    //region Tests Setup

    private SingularKit kit;
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
        MockitoAnnotations.initMocks(this);

        kit = new SingularKit();
        settings = new HashMap<>();
        settings.put(API_KEY, "Test");
        settings.put(API_SECRET, "Test");

        PowerMockito.mockStatic(Singular.class);
        PowerMockito.mockStatic(MParticle.class);
        PowerMockito.mockStatic(ReportingMessage.class);
        PowerMockito.mockStatic(CommerceEventUtils.class);
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

    @Test
    public void buildConfigWithoutDDL() {
        MParticle mParticle = Mockito.mock(MParticle.class);

        Mockito.when(MParticle.getInstance()).thenReturn(mParticle);

        Mockito.when(MParticle.getInstance().getEnvironment()).thenReturn(MParticle.Environment.Production);

        SingularConfig testConfig = kit.buildSingularConfig(settings);

        Assert.assertTrue(testConfig.apiKey.equals("Test"));
        Assert.assertTrue(testConfig.secret.equals("Test"));
        Assert.assertTrue(testConfig.ddlHandler.timeoutInSec == 60L);
    }

    @Test
    public void buildConfigInDevelopmentMode() {
        PowerMockito.mockStatic(MPUtility.class);

        Mockito.when(MPUtility.isDevEnv()).
                thenReturn(true);

        SingularConfig testConfig = kit.buildSingularConfig(settings);

        Assert.assertTrue(testConfig.apiKey.equals("Test"));
        Assert.assertTrue(testConfig.secret.equals("Test"));
        Assert.assertTrue(testConfig.logLevel == Log.DEBUG);
        Assert.assertTrue(testConfig.enableLogging == true);
        Assert.assertTrue(testConfig.ddlHandler.timeoutInSec == 60L);
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
            Mockito.when(Singular.eventJSON(any(String.class), any(JSONObject.class))).
                    thenReturn(true);

            Mockito.when(ReportingMessage.fromEvent(any(KitIntegration.class),
                    any(MPEvent.class))).
                    thenReturn(reportingMessage);

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
            event.getInfo().clear();

            // Mocking the kit Methods
            Mockito.when(Singular.event(any(String.class))).
                    thenReturn(true);

            Mockito.when(ReportingMessage.fromEvent(any(KitIntegration.class),
                    any(MPEvent.class))).
                    thenReturn(reportingMessage);

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
            CommerceEvent commerceEvent = PowerMockito.mock(CommerceEvent.class);

            Mockito.when(commerceEvent.getProductAction()).thenReturn(Product.PURCHASE);

            // Creating the products list to return
            List<Product> products = new ArrayList<>();
            Product first = Mockito.mock(Product.class);
            products.add(first);
            Product second = Mockito.mock(Product.class);
            products.add(second);

            // Mocking the values of the products
            Mockito.when(commerceEvent.getProducts()).thenReturn(products);
            Mockito.when(commerceEvent.getCurrency()).thenReturn("USD");

            Mockito.when(first.getSku()).thenReturn("Unknown");
            Mockito.when(first.getTotalAmount()).thenReturn(2.0);
            Mockito.when(first.getName()).thenReturn("Testing");
            Mockito.when(first.getCategory()).thenReturn("Category");
            Mockito.when(first.getQuantity()).thenReturn(1.0);
            Mockito.when(first.getUnitPrice()).thenReturn(2.0);

            Mockito.when(ReportingMessage.fromEvent(any(KitIntegration.class),
                    any(CommerceEvent.class))).
                    thenReturn(reportingMessage);

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
            CommerceEvent commerceEvent = PowerMockito.mock(CommerceEvent.class);

            // Mocking any action that is not purchase. Detail was selected randomly
            Mockito.when(commerceEvent.getProductAction()).thenReturn(Product.DETAIL);

            List<MPEvent> events = new ArrayList<>();

            // Creating the event
            JSONObject eventJson = new JSONObject();
            eventJson.put("eventName", "Testing");
            eventJson.put("eventType", "Unknown");
            events.add(MPEvent.Builder.parseString(eventJson.toString()).build());
            events.add(MPEvent.Builder.parseString(eventJson.toString()).build());

            // Mocking the kit Methods
            Mockito.when(CommerceEventUtils.expand(commerceEvent)).thenReturn(events);

            Mockito.when(Singular.event(any(String.class))).
                    thenReturn(true);

            Mockito.when(ReportingMessage.fromEvent(any(KitIntegration.class),
                    any(MPEvent.class))).
                    thenReturn(reportingMessage);

            Mockito.when(ReportingMessage.fromEvent(any(KitIntegration.class),
                    any(CommerceEvent.class))).
                    thenReturn(reportingMessage);

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
        String className = kit.getClass().getName();
        for (Map.Entry<Integer, String> entry : integrations.entrySet()) {
            if (entry.getValue().equals(className)) {
                return;
            }
        }
        fail(className + " not found as a known integration.");
    }

    //endregion
}