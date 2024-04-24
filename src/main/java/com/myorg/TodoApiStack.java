package com.myorg;

import software.amazon.awscdk.services.apigateway.*;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;

import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class TodoApiStack extends Stack {
    public TodoApiStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public TodoApiStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final Table ddb = Table.Builder.create(this, "TodosDB")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .build();

        List<String> packagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "timeout -s SIGKILL 5m mvn clean install && cp target/java-app-1.0.jar /asset-output");

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(packagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again
                        // inside the
                        // container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/asset-input/?/.m2/")
                                .build()))
                .outputType(ARCHIVED);

        Code javaBundle = Code.fromAsset("./lambda/", AssetOptions.builder()
                .bundling(builderOptions
                        .command(packagingInstructions)
                        .build())
                .build());

        Object origins = this.getNode().tryGetContext("origins");
        final String allowedOrigins = (origins == null ? "*" : origins.toString());
        Map<String, String> commonEnvVariables = new java.util.HashMap<String, String>() {
            {
                put("TABLE_NAME", ddb.getTableName());
                put("POWERTOOLS_LOG_LEVEL", "INFO");
                put("JAVA_TOOL_OPTIONS", "-XX:+TieredCompilation -XX:TieredStopAtLevel=1");
                put("ALLOWED_ORIGINS", allowedOrigins);
            }
        };

        FunctionProps.Builder functionBuilder = FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(javaBundle)
                .memorySize(1024)
                .environment(commonEnvVariables)
                .timeout(Duration.seconds(29))
                .logRetention(RetentionDays.ONE_WEEK)
                .tracing(Tracing.ACTIVE);

        Function getTodos = new Function(this, "getTodos", functionBuilder
                .handler("com.myorg.todo.ListTodosFunction::handleRequest")
                .build());
        getTodos.addEnvironment("POWERTOOLS_SERVICE_NAME", "list_todo");
        ddb.grantReadData(getTodos);

        Function getTodo = new Function(this, "getTodo", functionBuilder
                .handler("com.myorg.todo.ReadTodoFunction::handleRequest")
                .build());
        getTodo.addEnvironment("POWERTOOLS_SERVICE_NAME", "get_todo");
        ddb.grantReadData(getTodo);

        Function addTodo = new Function(this, "addTodo", functionBuilder
                .handler("com.myorg.todo.CreateTodoFunction::handleRequest")
                .build());
        addTodo.addEnvironment("POWERTOOLS_SERVICE_NAME", "add_todo");
        ddb.grantWriteData(addTodo);

        Function deleteTodo = new Function(this, "deleteTodo", functionBuilder
                .handler("com.myorg.todo.DeleteTodoFunction::handleRequest")
                .build());
        deleteTodo.addEnvironment("POWERTOOLS_SERVICE_NAME", "delete_todo");
        ddb.grantWriteData(deleteTodo);

        Function updateTodo = new Function(this, "updateTodo", functionBuilder
                .handler("com.myorg.todo.UpdateTodoFunction::handleRequest")
                .build());
        updateTodo.addEnvironment("POWERTOOLS_SERVICE_NAME", "update_todo");
        ddb.grantReadWriteData(updateTodo);

        RestApi apiGateway = new RestApi(this, "ApiGateway",
                RestApiProps.builder()
                        .defaultCorsPreflightOptions(CorsOptions.builder()
                                .allowCredentials(true)
                                .allowMethods(Arrays.asList("GET", "PUT", "POST", "DELETE", "OPTIONS"))
                                .allowHeaders(Arrays.asList("Content-Type", "Authorization", "Content-Length",
                                        "X-Requested-With"))
                                .allowOrigins(Arrays.asList(allowedOrigins.split(",")))
                                .build())
                        .build());

        Resource api = apiGateway.getRoot().addResource("api");
        Resource todos = api.addResource("todos");
        todos.addMethod("GET", new LambdaIntegration(getTodos));
        todos.addMethod("POST", new LambdaIntegration(addTodo));

        Resource todoId = todos.addResource("{id}");
        todoId.addMethod("PUT", new LambdaIntegration(updateTodo));
        todoId.addMethod("GET", new LambdaIntegration(getTodo));
        todoId.addMethod("DELETE", new LambdaIntegration(deleteTodo));

        new CfnOutput(this, "ApiEndpoint", CfnOutputProps.builder()
                .value(apiGateway.getUrl())
                .build());

        new CfnOutput(this, "ApiDomain", CfnOutputProps.builder()
                .value(apiGateway.getUrl().split("/")[2])
                .build());
        new CfnOutput(this, "ApiStage", CfnOutputProps.builder()
                .value(apiGateway.getDeploymentStage().getStageName())
                .build());
    }
}
