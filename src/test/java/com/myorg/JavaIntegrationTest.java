package com.myorg;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;

import org.junit.jupiter.api.Test;

public class JavaIntegrationTest {

    @Test
    public void whenSendingGet_thenMessageIsReturned() throws IOException {
        // get url from aws cloudformation stack output
        CloudFormationClient cloudFormationClient = CloudFormationClient.create();
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder()
                .stackName("TsAppBackendStack-yqwau")
                .build();
        DescribeStacksResponse describeStacksResult = cloudFormationClient.describeStacks(describeStacksRequest);
        // Get ApiEndpoint from stack output called "ApiEndpoint"
        String url = describeStacksResult.stacks().get(0).outputs().stream()
                .filter(output -> output.outputKey().equals("ApiEndpoint"))
                .findFirst()
                .get()
                .outputValue();

        URLConnection connection = new URL(url + "/api/todos").openConnection();
        try (InputStream response = connection.getInputStream();
                Scanner scanner = new Scanner(response)) {
            String responseBody = scanner.nextLine();
            assertNotNull(responseBody);
        }
    }
}
