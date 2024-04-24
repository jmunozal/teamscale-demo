# This blueprint
This blueprint creates a REST API project. The project uses AWS Lambda and Amazon API Gateway with a To Do service reference and deploys it into a chosen AWS account. 

# Architecture overview

The project deploys a RESTful API application that uses the following AWS Serverless technologies:

* AWS API Gateway (https://aws.amazon.com/api-gateway) to provide the REST interface to the user.
* Amazon DynamoDB (https://aws.amazon.com/dynamodb) as a data store
* AWS Lambda (https://aws.amazon.com/lambda) process the API gateway requests and read data from or write data to a DynamoDB table. 

Both the AWS Cloud Development Kit (CDK) application and AWS Lambda code are written in three languages. You can choose from the following programming languages:

* Python 3.8
* Java 11
* Node.js 16 (Typescript)

The build pipeline runs unit and integration tests on the application and produces testing reports. Failed tests will stop the artifacts from publishing.

![Architecture Diagram](https://deyn4asqcu6xj.cloudfront.net/serverless-todo-backend-arch.png) 

## Connections and permissions

The `"To Do" service` supports the Amazon CodeCatalyst Development Role, which can be created from the [AWS management console Codecatalyst application](https://us-west-2.console.aws.amazon.com/codecatalyst/home?region=us-west-2#/). When clicking “add IAM role”, the first option is to create a CodeCatalyst development role. After clicking create, the role will be automatically added to the Amazon CodeCatalyst space. 

The other option is creating a application specific IAM role, which can be added to the Amazon CodeCatalyst space by selecting "Add an existing IAM role" from the add IAM role options. The IAM role needs to contain the CodeCatalyst trust policy, as well as the following permissions:

## IAM Role

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cloudformation:*",
                "ssm:*",
                "s3:*",
                "iam:PassRole",
                "iam:GetRole",
                "iam:CreateRole",
                "iam:AttachRolePolicy",
                "iam:PutRolePolicy"
            ],
            "Resource": "*"
        }
    ]
}
```

The IAM roles also require the following CodeCatalyst service principals:
*  codecatalyst.amazonaws.com
*  codecatalyst-runner.amazonaws.com

## Trust policy:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "",
            "Effect": "Allow",
            "Principal": {
                "Service": [
                    "codecatalyst.amazonaws.com",
                    "codecatalyst-runner.amazonaws.com"
                ]
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
```

# Project resources

This blueprint creates the following Amazon CodeCatalyst resources:

* Source repository named todo-app
* A workflow defined in .codecatalyst/workflows/main_fullstack_workflow.yaml
* Initial deployment of the architecture stacks to the linked AWS account.

After being created successfully, this project deploys the following AWS resource: 

* Amazon DynamoDB table based on input name
* Amazon Lambda functions to handle back end transactions
* Amazon API Gateway REST API with chosen name

View the deployment status in the project's workflow.

## Installation 

The `cdk.json` file tells the CDK Toolkit how to execute your app.

### Prerequisite

Install tools:
  - Maven with [official doc](https://maven.apache.org/install.html)
  - AWS CDK with `npm install -g aws-cdk`
  - Docker with [official doc](https://docs.docker.com/engine/install/)

Install dependencies
```
mvn install
```

Bootstrap AWS account
```
cdk bootstrap
```

### Develop locally

Run unit test (optional)
```
mvn test
```

### Deploy to the cloud

Deploy
```
cdk deploy
```

Run e2e tests
```
mvn test -Pinteg
```

## Additional resources

See the Amazon CodeCatalyst user guide for additional information on using the features and resources of Amazon CodeCatalyst