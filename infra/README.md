# CDK Typescript Stack

This is the AWS stack definition for the Minecraft server, in Typescript.

## Resources

The stack defines the following AWS resources:

### 1. VPC

A VPC to host the Minecraft server

### 2. EBS Storage

An EBS storage to hold the world data

### 3. EC2 Instance

An EC2 instance to run the Minecraft server

### 4. Lambda Function

A Lambda function to interact with the Minecraft server

### 5. Resource Group

A resource group to be able to easily browse all resources related to the project

## Updating the Infrastructure

To update the project infrastructure, update the lib/infra-stack.ts and/or the bin/infra.ts files as needed.

## Deploying Changes

After updating the infrastructure files, commit it them to the repository and run:

```bash
cd infra
cdk deploy \
  -c apiSecret=$(openssl rand -hex 24) \
  -c githubRepos='["yourname/minecraft-server","yourname/simple-mod"]'
```
