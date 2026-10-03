#!/usr/bin/env node
import { App } from 'aws-cdk-lib';
import { InfraStack } from '../lib/infra-stack';

const app = new App();

new InfraStack(app, 'MinecraftPaperMCServer', {
  env: { account: '815354249022', region: 'us-east-1' },

  gitHubRepositoryUrl: "https://github.com/It-Will-Be-Apps/Minecraft-PaperMC-Server.git",
  instanceType: app.node.tryGetContext('instanceType'),
  dataVolumeSizeGiB: app.node.tryGetContext('dataVolumeSizeGiB'),
  maxIdleDurationInMinutes: app.node.tryGetContext('maxIdleDurationInMinutes'),
  idleCheckPeriodInMinutes: app.node.tryGetContext('idleCheckPeriodInMinutes'),
  snapshotsToKeep: app.node.tryGetContext('snapshotsToKeep'),
  domainName: "itwillbeapps.ca"
});
