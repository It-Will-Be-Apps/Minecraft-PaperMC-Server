import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as lambda_nodejs from 'aws-cdk-lib/aws-lambda-nodejs';
import * as resourcegroups from 'aws-cdk-lib/aws-resourcegroups';
import * as cdk from 'aws-cdk-lib/core';
import { Construct } from 'constructs';
import * as fs from 'fs';
import * as path from 'path';

export interface MinecraftServerStackProps extends cdk.StackProps {

  // GitHub repository URL
  gitHubRepositoryUrl: string;

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

    const gitHubRepositoryUrl = props.gitHubRepositoryUrl;
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
      size: cdk.Size.gibibytes(dataVolumeSizeGiB),
      volumeType: ec2.EbsDeviceVolumeType.GP3,
      removalPolicy: cdk.RemovalPolicy.RETAIN
    });

    const worldDataVolumeId = worldDataVolume.volumeId;

    // ---------------------------------------------------------------------
    // EC2 instance to run the server
    // ---------------------------------------------------------------------
    const serverEc2InstanceSecurityGroup = new ec2.SecurityGroup(this, 'SecurityGroup', {
      vpc,
      description: 'Minecraft server access',
      allowAllOutbound: true,
    });

    serverEc2InstanceSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(25565), 'Minecraft Java Edition');
    // serverEc2InstanceSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH');

    const serverEc2InstanceRole = new iam.Role(this, 'ServerInstanceRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
    });

    serverEc2InstanceRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'));

    const userDataScript = fs.readFileSync(path.join(__dirname, '..', 'ec2', 'user-data.sh'), 'utf8')
        .replace('__WORLD_VOLUME_ID__', worldDataVolumeId)
        .replace('__GIT_HUB_REPOSITORY_URL__', gitHubRepositoryUrl);

    const userData = ec2.UserData.custom(userDataScript);

    const serverEc2InstanceProfile = new iam.InstanceProfile(this, 'ServerInstanceProfile', {
      role: serverEc2InstanceRole
    });

    const serverEc2Instance = new ec2.Instance(this, 'ServerInstance', {
      vpc,
      vpcSubnets: { subnets: [subnet] },
      instanceType: instanceType,
      machineImage: ec2.MachineImage.latestAmazonLinux2023({ cpuType: ec2.AmazonLinuxCpuType.ARM_64 }),
      securityGroup: serverEc2InstanceSecurityGroup,
      instanceProfile: serverEc2InstanceProfile,
      userData: userData
    });

    const serverEc2InstanceSSHPolicy = new iam.ManagedPolicy(this, APP_NAME + '-SSHPolicy', {
      managedPolicyName: APP_NAME + '-EC2AllowConsoleSSHOnly',
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'ssm:StartSession',
            'ssm:SendCommand'
          ],
          resources: [
            `arn:aws:ec2:${this.region}:${this.account}:instance/${serverEc2Instance.instanceId}`,
            'arn:aws:ssm:*:*:document/AWS-StartSSHSession'
          ]
        }),
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'ssm:TerminateSession',
            'ssm:ResumeSession'
          ],
          resources: ['arn:aws:ssm:*:*:session/\${aws:username}-*']
        })
      ]
    });

    new ec2.CfnVolumeAttachment(this, APP_NAME + '-DataVolumeAttachment', {
      instanceId: serverEc2Instance.instanceId,
      volumeId: worldDataVolumeId,
      device: '/dev/sdf'
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
      timeout: cdk.Duration.minutes(5)
    });

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:DescribeInstances'
      ],
      resources: ['*']
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:StartInstances',
        'ec2:StopInstances',
        'ec2:CreateSnapshot'
      ],
      resources: [
        this.formatArn({
          service: 'ec2',
          resource: 'instance',
          resourceName: serverEc2Instance.instanceId
        })
      ]
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ssm:SendCommand',
        'ssm:GetCommandInvocation',
      ],
        resources: ['*']
    }));


    // ---------------------------------------------------------------------
    // Lambda function to interface with Discord commands
    // ---------------------------------------------------------------------
    const discordInterfaceLambda = new lambda_nodejs.NodejsFunction(this, 'DiscordInterface', {
      runtime: lambda.Runtime.NODEJS_LATEST,
      handler: 'handler',
      entry: path.join(__dirname, '..', 'lambda', 'discord-interface', 'index.ts'),
      environment: {
        DISCORD_APP_PUBLIC_KEY: discordAppPublicKey,
        DISCORD_GUILD_ID: discordGuildId,
        DISCORD_PLAYER_ROLE_ID: discordPlayerRoleId,
        DISCORD_CONTROL_CHANNEL_ID: discordControlChannelId,
        DISCORD_OWNER_USER_ID: discordOwnerUserId,
        SERVER_MANAGEMENT_FUNCTION_NAME: serverManagementLambda.functionName
      },
      timeout: cdk.Duration.seconds(5)
    });

    const discordInterfaceLambdaUrl = discordInterfaceLambda.addFunctionUrl({
      authType: lambda.FunctionUrlAuthType.NONE
    });

    serverManagementLambda.grantInvoke(discordInterfaceLambda);

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
          ]
        }
      }
    });

    // ---------------------------------------------------------------------
    // Other configurations
    // ---------------------------------------------------------------------

    // Ensure all resources in the stack are tagged with the application name
    cdk.Tags.of(this).add('Application', APP_NAME);

    // Useful outputs from the generated resources
    new cdk.CfnOutput(this, 'DiscordFunctionUrl', { value: discordInterfaceLambdaUrl.url });
    new cdk.CfnOutput(this, 'EC2SSHPolicyArn', { value: serverEc2InstanceSSHPolicy.managedPolicyArn });
  }
}
