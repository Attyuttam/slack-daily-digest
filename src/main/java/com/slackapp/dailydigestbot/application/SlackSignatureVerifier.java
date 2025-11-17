package com.slackapp.dailydigestbot.application;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

@Component
public class SlackSignatureVerifier {
    private static final String VERSION = "v0";

    public boolean verify(String signingSecret, String timestamp, String body, String slackSig) {
        try {
            String basestring = VERSION + ":" + timestamp + ":" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(), "HmacSHA256"));
            String mySig = VERSION + "=" + Hex.encodeHexString(mac.doFinal(basestring.getBytes()));
            return mySig.equals(slackSig);
        } catch (Exception e) {
            return false;
        }
    }
}
