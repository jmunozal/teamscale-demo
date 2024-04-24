package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class JavaTest {

    @Test
    public void testStack() throws IOException {
        App app = new App();
        TodoApiStack stack = new TodoApiStack(app, "test");

        Template template = Template.fromStack(stack);

        template.resourceCountIs("AWS::Lambda::Function", 6);

        template.hasResource("AWS::DynamoDB::Table", 1);

        template.hasResource("AWS::ApiGateway::RestApi", 1);

    }
}
