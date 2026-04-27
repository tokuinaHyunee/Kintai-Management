package com.example.kintai;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {

    @Test
    void printHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = "1234!@";
        String hash = encoder.encode(raw);
        System.out.println("NEW_HASH=" + hash);
    }
}
