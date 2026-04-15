package com.qiankubx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "SPRING_INTEGRATION_TEST", matches = "true")
class QianKuApplicationTests {

    @Test
    void contextLoads() {
    }
}
