package com.myorg;

import org.junit.jupiter.api.Timeout;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class TodoApiStackTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public void testStack() {
        App app = new App();
        TodoApiStack stack = new TodoApiStack(app, "test");

        Template template = Template.fromStack(stack);

        template.resourceCountIs("AWS::Lambda::Function", 6);

        template.hasResource("AWS::DynamoDB::Table", 1);

        template.hasResource("AWS::ApiGateway::RestApi", 1);

    }
}
