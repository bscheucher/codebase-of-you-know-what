package com.ibosng.validationservice;

import com.ibosng.validationservice.config.DataSourceConfigTest;
import com.ibosng.validationservice.services.impl.ValidatorServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {DataSourceConfigTest.class})
class ValidationServiceApplicationTests {


    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private ValidatorServiceImpl validatorServiceImpl;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

}
