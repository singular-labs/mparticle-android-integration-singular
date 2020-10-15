package com.mparticle.kits;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

public class MockSingularKit extends SingularKit {

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        if (commerceEvent != null) {
            return Collections.singletonList(new ReportingMessage(this, commerceEvent.getEventName(), System.currentTimeMillis(), new HashMap()));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ReportingMessage> logEvent(final MPEvent mpEvent) {
        if (mpEvent != null) {
            return Collections.singletonList(new ReportingMessage(this, mpEvent.getEventType().toString(), System.currentTimeMillis(), new HashMap()));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public KitConfiguration getConfiguration() {
        try {
            return KitConfiguration.createKitConfiguration(new JSONObject().put("id", MParticle.ServiceProviders.SINGULAR));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
