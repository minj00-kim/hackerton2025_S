package com.hackerton.hackerton2025.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GuestCookieFilter extends OncePerRequestFilter {

    public static final String ATTR = "ANON_USER_ID";
    private final HmacSigner signer;

    public GuestCookieFilter(@Value("${app.guest.secret}") String secret) {
        this.signer = new HmacSigner(secret);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String idStr = null, sig = null;

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            idStr = Arrays.stream(cookies)
                    .filter(c -> "anon_id".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst().orElse(null);

            sig = Arrays.stream(cookies)
                    .filter(c -> "anon_sig".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst().orElse(null);
        }

        boolean valid = idStr != null && sig != null && signer.verify(idStr, sig);

        if (!valid) {
            long newId = ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
            idStr = Long.toString(newId);
            sig = signer.sign(idStr);

            boolean secure = req.isSecure(); // HTTPS면 true
            ResponseCookie idC = ResponseCookie.from("anon_id", idStr)
                    .httpOnly(true).secure(secure).sameSite("Lax")
                    .path("/").maxAge(60L * 60 * 24 * 180).build();
            ResponseCookie sigC = ResponseCookie.from("anon_sig", sig)
                    .httpOnly(true).secure(secure).sameSite("Lax")
                    .path("/").maxAge(60L * 60 * 24 * 180).build();

            res.addHeader("Set-Cookie", idC.toString());
            res.addHeader("Set-Cookie", sigC.toString());
        }

        // 컨트롤러에서 Long으로 바로 쓰게 attribute로 꽂아줌
        try {
            req.setAttribute(ATTR, Long.parseLong(idStr));
        } catch (NumberFormatException e) {
            // 혹시 모를 파싱 이슈 대비: 새로 발급
            long newId = ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE);
            req.setAttribute(ATTR, newId);
            boolean secure = req.isSecure();
            ResponseCookie idC = ResponseCookie.from("anon_id", Long.toString(newId))
                    .httpOnly(true).secure(secure).sameSite("Lax")
                    .path("/").maxAge(60L * 60 * 24 * 180).build();
            ResponseCookie sigC = ResponseCookie.from("anon_sig", signer.sign(Long.toString(newId)))
                    .httpOnly(true).secure(secure).sameSite("Lax")
                    .path("/").maxAge(60L * 60 * 24 * 180).build();
            res.addHeader("Set-Cookie", idC.toString());
            res.addHeader("Set-Cookie", sigC.toString());
        }

        chain.doFilter(req, res);
    }
}
