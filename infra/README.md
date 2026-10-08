# CDK Typescript Stack

This is the AWS stack definition for the Minecraft server, in Typescript.

## Prerequisites

- Add GitHub as an identity provider on AWS

## Resources

The stack defines the following AWS resources:

### 1. S3 Bucket

A S3 bucket to hold the custom plugins

### 2. VPC

A VPC to host the Minecraft server

### 3. EBS Storage

An EBS storage to hold the world data

### 4. EC2 Instance

An EC2 instance to run the Minecraft server

### 5. Lambda Function

A Lambda function to interact with the Minecraft server

### 6. Lambda Function

A Lambda function to interface with Discord commands

### 7. Resource Group

A resource group to be able to easily browse all resources related to the project

## Bootstraping the project

To bootstrap the project (one time per AWS account/region), run:

```bash
cdk bootstrap
```

## Updating the Infrastructure

To update the project infrastructure, update the `lib/infra-stack.ts` and/or the `bin/infra.ts` files as needed.

## Deploying Changes

After updating the infrastructure files, commit them to the repository and run:

```bash
cd infra
cdk deploy \
  -c apiSecret=$(openssl rand -hex 24) \
  -c githubRepos='["yourname/minecraft-server","yourname/simple-mod"]'
```
