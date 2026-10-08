import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as lambda_nodejs from 'aws-cdk-lib/aws-lambda-nodejs';
import * as resourcegroups from 'aws-cdk-lib/aws-resourcegroups';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as s3 from 'aws-cdk-lib/aws-s3';
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
  maxIdleDurationInMinutes?: number;

  // Idle check period in minutes
  idleCheckPeriodInMinutes?: number;

  // Number of world backups to retain
  snapshotsToKeep?: number;

  // Domain name to reach the server
  domainName: string;
}

export class InfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: MinecraftServerStackProps) {
    super(scope, id, props);

    const APP_NAME = 'MinecraftPaperMCServer';

    const gitHubRepositoryUrl = props.gitHubRepositoryUrl;
    const gitHubRepository = gitHubRepositoryUrl.replace(/^https:\/\/github\.com\//, '').replace(/\.git$/, '').replace(/\/$/, '');
    const instanceType = props.instanceType ?? ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.LARGE);
    const dataVolumeSizeGiB = props.dataVolumeSizeGiB ?? 8;
    const maxIdleDurationInMinutes = props.maxIdleDurationInMinutes ?? 10;
    const idleCheckPeriodInMinutes = props.idleCheckPeriodInMinutes ?? 5;
    const snapshotsToKeep = props.snapshotsToKeep ?? 10;
    const domainName = props.domainName;

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
    // IAM role to allow GitHub to update the plugins bucket
    // ---------------------------------------------------------------------
    const githubOidcProvider = iam.OpenIdConnectProvider.fromOpenIdConnectProviderArn(
      this,
      'GitHubOidcProvider',
      `arn:aws:iam::${this.account}:oidc-provider/token.actions.githubusercontent.com`
    );

    const githubDeploymentRole = new iam.Role(this, 'GitHubDeploymentRole', {
      roleName: `${APP_NAME}-GitHubDeploymentRole`,
      assumedBy: new iam.WebIdentityPrincipal(
        githubOidcProvider.openIdConnectProviderArn,
        {
          StringEquals: {
            'token.actions.githubusercontent.com:aud': 'sts.amazonaws.com'
          },
          StringLike: {
            'token.actions.githubusercontent.com:sub':
              `repo:${gitHubRepository}:ref:refs/heads/main`
          }
        }
      )
    });

    // ---------------------------------------------------------------------
    // S3 Bucket to hold the custom plugins
    // ---------------------------------------------------------------------
    const pluginsBucket = new s3.Bucket(this, APP_NAME + '-PluginsBucket', {
      bucketName: 'minecraft-papermc-server-plugins-bucket',
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true
    });

    pluginsBucket.grantRead(serverEc2InstanceRole, '*');
    pluginsBucket.grantPut(githubDeploymentRole, '*');

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

    const serverEc2InstanceRole = new iam.Role(this, 'ServerInstanceRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
    });

    serverEc2InstanceRole.addManagedPolicy(
      iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore')
    );

    const userDataScript = fs.readFileSync(path.join(__dirname, '..', 'ec2', 'user-data.sh'), 'utf8')
        .replace('__WORLD_VOLUME_ID__', worldDataVolumeId)
        .replace('__GIT_HUB_REPOSITORY_URL__', gitHubRepositoryUrl)
        .replace('__PLUGINS_BUCKET_NAME__', pluginsBucket.bucketName);

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
    // Hosted zone to hold the domain name
    // ---------------------------------------------------------------------
    const hostedZone = new route53.PublicHostedZone(this, APP_NAME + '-HostedZone', {
      zoneName: domainName
    });

    new route53.ARecord(this, APP_NAME + '-MinecraftDnsRecord', {
      zone: hostedZone,
      recordName: 'minecraft',
      target: route53.RecordTarget.fromIpAddresses('0.0.0.0'),
      ttl: cdk.Duration.seconds(30)
    });

    // ---------------------------------------------------------------------
    // Lambda function to interact with the server
    // ---------------------------------------------------------------------
    const serverManagementLambda = new lambda_nodejs.NodejsFunction(this, 'ServerManagement', {
      runtime: lambda.Runtime.NODEJS_LATEST,
      handler: 'handler',
      entry: path.join(__dirname, '..', 'lambda', 'server-management', 'index.ts'),
      environment: {
        APP_NAME: APP_NAME,
        EC2_INSTANCE_ID: serverEc2Instance.instanceId,
        MAX_IDLE_DURATION_IN_MINUTES: String(maxIdleDurationInMinutes),
        WORLD_VOLUME_ID: worldDataVolume.volumeId,
        SNAPSHOTS_TO_KEEP: String(snapshotsToKeep),
        HOSTED_ZONE_ID: hostedZone.hostedZoneId,
        DOMAIN_NAME: domainName
      },
      timeout: cdk.Duration.minutes(10)
    });

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:DescribeInstances',
        'ec2:DescribeSnapshots',
        'ec2:DescribeVolumes',
        'ec2:CreateVolume',
        'ssm:GetCommandInvocation'
      ],
      resources: ['*']
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:StartInstances',
        'ec2:StopInstances',
        'ec2:CreateSnapshot',
        'ssm:SendCommand'
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
        'ssm:SendCommand'
      ],
      resources: [
        this.formatArn({
          service: 'ssm',
          resource: 'document',
          resourceName: 'AWS-RunShellScript',
          account: ''
        })
      ]
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:CreateSnapshot',
        'ec2:DeleteSnapshot'
      ],
      resources: [
        this.formatArn({
          service: 'ec2',
          resource: 'volume',
          resourceName: worldDataVolume.volumeId
        }),
        this.formatArn({
          service: 'ec2',
          resource: 'snapshot',
          resourceName: '*',
          account: ''
        })
      ]
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:CreateTags'
      ],
      resources: [
        this.formatArn({
          service: 'ec2',
          resource: 'snapshot',
          resourceName: '*',
          account: ''
        })
      ],
      conditions: {
        StringEquals: {
          'ec2:CreateAction': 'CreateSnapshot'
        }
      }
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:AttachVolume',
        'ec2:DetachVolume',
        'ec2:DeleteVolume'
      ],
      resources: [
        this.formatArn({
          service: 'ec2',
          resource: 'volume',
          resourceName: '*'
        }),
        this.formatArn({
          service: 'ec2',
          resource: 'instance',
          resourceName: serverEc2Instance.instanceId
        })
      ]
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ec2:CreateTags'
      ],
      resources: [
        this.formatArn({
          service: 'ec2',
          resource: 'volume',
          resourceName: '*'
        })
      ],
      conditions: {
        StringEquals: {
          'ec2:CreateAction': 'CreateVolume'
        }
      }
    }));

    serverManagementLambda.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'route53:ChangeResourceRecordSets'
      ],
      resources: [
        hostedZone.hostedZoneArn
      ],
      conditions: {
        'ForAllValues:StringEquals': {
          'route53:ChangeResourceRecordSetsNormalizedRecordNames': [
            `minecraft.${domainName}`
          ],
          'route53:ChangeResourceRecordSetsRecordTypes': [
            'A'
          ],
          'route53:ChangeResourceRecordSetsActions': [
            'UPSERT'
          ]
        }
      }
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
    // Event Bridge rule to shutdown the server when idle for too long
    // ---------------------------------------------------------------------
    const minecraftIdleCheckRule = new events.Rule(this, 'MinecraftIdleCheckRule', {
      schedule: events.Schedule.rate(cdk.Duration.minutes(idleCheckPeriodInMinutes))
    });

    minecraftIdleCheckRule.addTarget(new targets.LambdaFunction(serverManagementLambda, {
      event: events.RuleTargetInput.fromObject({
        type: 'minecraft-idle-check'
      })
    }));

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
