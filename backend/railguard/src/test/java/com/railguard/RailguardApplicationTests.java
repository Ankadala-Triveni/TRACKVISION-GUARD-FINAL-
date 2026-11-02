package com.railguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "JWT_SECRET=dGhpcy1pcy1hLXNlY3VyZS1hbmQtc3VmZmljaWVudGx5LWxvbmctc2VjcmV0LWtleS1mb3ItZGV2LTIwMjYtcmFpbGd1YXJk"
})
class RailguardApplicationTests {

	@Test
	void contextLoads() {
	}

}
