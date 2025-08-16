package com.hackerton.hackerton2025.Security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class HmacSigner {
    private static final String ALG = "HmacSHA256";
    private final byte[] key;

    public HmacSigner(String secret) { this.key = secret.getBytes(); }

    public String sign(String value) {
        try {
            Mac mac = Mac.getInstance(ALG);
            mac.init(new SecretKeySpec(key, ALG));
            byte[] out = mac.doFinal(value.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public boolean verify(String value, String sig) {
        return sign(value).equals(sig);
    }
}