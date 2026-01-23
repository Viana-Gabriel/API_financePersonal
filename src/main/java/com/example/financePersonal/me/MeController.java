package com.example.financePersonal.me;

import com.example.financePersonal.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class MeController {

    @GetMapping("/me")
    public Object me(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();

        var body = new HashMap<String, Object>();
        body.put("userId", principal.userId().toString());
        body.put("email", principal.email());
        return body;
    }
}
