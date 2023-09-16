package com.github;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = IAssignApplication.class)
public class SpringApplicationTests {
}
