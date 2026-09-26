import { CfnOutput, Duration, RemovalPolicy, Size, StackProps } from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as lambda_nodejs from 'aws-cdk-lib/aws-lambda-nodejs';
import * as resourcegroups from 'aws-cdk-lib/aws-resourcegroups';
import * as cdk from 'aws-cdk-lib/core';
import { Construct } from 'constructs';
import * as fs from 'fs';
import * as path from 'path';

export interface MinecraftServerStackProps extends StackProps {
  // EC2 instance type
  instanceType?: ec2.InstanceType;

  // Size of the persistent world-data EBS volume, in GiB
  dataVolumeSizeGiB?: number;

  // Minutes with 0 players online before the server auto-stops
  idleMinutesBeforeStop?: number;

  // Number of world backups to retain
  snapshotsToKeep?: number;
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: MinecraftServerStackProps) {
    super(scope, id, props);

    const APP_NAME = 'MinecraftPaperMCServer';

    const instanceType = props.instanceType ?? ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MEDIUM);
    const dataVolumeSizeGiB = props.dataVolumeSizeGiB ?? 8;
    const idleMinutesBeforeStop = props.idleMinutesBeforeStop ?? 10;
    const snapshotsToKeep = props.snapshotsToKeep ?? 3;

    // ---------------------------------------------------------------------
    // Parameters initialization
    // ---------------------------------------------------------------------
    const discordAppPublicKey = this.node.tryGetContext('discordAppPublicKey');
    if (!discordAppPublicKey) {
      throw new Error('Missing required parameter discordAppPublicKey');
    }

    const discordGuildId = this.node.tryGetContext('discordGuildId');
    if (!discordGuildId) {
      throw new Error('Missing required parameter discordGuildId');
    }

    const discordPlayerRoleId = this.node.tryGetContext('discordPlayerRoleId');
    if (!discordPlayerRoleId) {
      throw new Error('Missing required parameter discordPlayerRoleId');
    }

    const discordControlChannelId = this.node.tryGetContext('discordControlChannelId');
    if (!discordControlChannelId) {
      throw new Error('Missing required parameter discordControlChannelId');
    }

    const discordOwnerUserId = this.node.tryGetContext('discordOwnerUserId');
    if (!discordOwnerUserId) {
      throw new Error('Missing required parameter discordOwnerUserId');
    }

    // ---------------------------------------------------------------------
    // VPC to host the server
    // ---------------------------------------------------------------------
    const vpc = new ec2.Vpc(this, APP_NAME + '-Vpc', {
      maxAzs: 1,
      natGateways: 0,
      subnetConfiguration: [{ name: 'public', subnetType: ec2.SubnetType.PUBLIC, cidrMask: 24 }]
    });

    const subnet = vpc.publicSubnets[0];

    // ---------------------------------------------------------------------
    // EBS storage to hold the world data
    // ---------------------------------------------------------------------
    const worldDataVolume = new ec2.Volume(this, APP_NAME + '-DataVolume', {
      availabilityZone: subnet.availabilityZone,
      size: Size.gibibytes(dataVolumeSizeGiB),
      volumeType: ec2.EbsDeviceVolumeType.GP3,
      removalPolicy: RemovalPolicy.RETAIN
    });

    // ---------------------------------------------------------------------
    // EC2 instance to run the server
    // ---------------------------------------------------------------------
    const serverEc2InstanceSecurityGroup = new ec2.SecurityGroup(this, 'SecurityGroup', {
      vpc,
      description: 'Minecraft server access',
      allowAllOutbound: true,
    });

    serverEc2InstanceSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(25565), 'Minecraft Java Edition');
    serverEc2InstanceSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH');

    const userDataScript = fs.readFileSync(path.join(__dirname, '..', 'ec2', 'user-data.sh'), 'utf8');
    const userData = ec2.UserData.custom(userDataScript);

    const serverEc2Instance = new ec2.Instance(this, 'ServerInstance', {
      vpc,
      instanceType: instanceType,
      machineImage: ec2.MachineImage.latestAmazonLinux2023({ cpuType: ec2.AmazonLinuxCpuType.ARM_64 }),
      securityGroup: serverEc2InstanceSecurityGroup,
      userData: userData
    });

    new ec2.CfnVolumeAttachment(this, APP_NAME + '-DataVolumeAttachment', {
      instanceId: serverEc2Instance.instanceId,
      volumeId: worldDataVolume.volumeId,
      device: '/dev/sdf',
    });

    // ---------------------------------------------------------------------
    // Lambda function to interact with the server
    // ---------------------------------------------------------------------
    const serverManagementLambda = new lambda_nodejs.NodejsFunction(this, 'ServerManagement', {
      runtime: lambda.Runtime.NODEJS_LATEST,
      handler: 'handler',
      entry: path.join(__dirname, '..', 'lambda', 'server-management', 'index.ts'),
      environment: {
        DISCORD_APP_PUBLIC_KEY: discordAppPublicKey,
        DISCORD_GUILD_ID: discordGuildId,
        DISCORD_PLAYER_ROLE_ID: discordPlayerRoleId,
        DISCORD_CONTROL_CHANNEL_ID: discordControlChannelId,
        DISCORD_OWNER_USER_ID: discordOwnerUserId,
        EC2_INSTANCE_ID: serverEc2Instance.instanceId
      },
      timeout: Duration.minutes(1)
    });

    const serverManagementLambdaUrl = serverManagementLambda.addFunctionUrl({
      authType: lambda.FunctionUrlAuthType.NONE,
    });

    serverManagementLambda.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['ec2:StartInstances', 'ec2:StopInstances'],
        resources: [
          this.formatArn({
            service: 'ec2',
            resource: 'instance',
            resourceName: serverEc2Instance.instanceId
          }),
        ],
      })
    );

    // Ensure all resources in the stack are tagged with the application name
    cdk.Tags.of(this).add('Application', APP_NAME);

    // ---------------------------------------------------------------------
    // Resource group to group all resources in the stack together
    // ---------------------------------------------------------------------
    new resourcegroups.CfnGroup(this, 'ApplicationResourceGroup', {
      name: `${APP_NAME}-Group`,
      description: `Tracks all resources for the ${APP_NAME} app`,
      resourceQuery: {
        type: 'TAG_FILTERS_1_0',
        query: {
          resourceTypeFilters: ['AWS::AllSupported'],
          tagFilters: [
            {
              key: 'Application',
              values: [APP_NAME],
            }
          ],
        },
      },
    });

    // ---------------------------------------------------------------------
    // Useful outputs from the generated resources
    // ---------------------------------------------------------------------
    new CfnOutput(this, 'DiscordFunctionUrl', { value: serverManagementLambdaUrl.url });
  }
}
