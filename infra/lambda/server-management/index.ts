import {
  AttachVolumeCommand,
  CreateSnapshotCommand,
  CreateVolumeCommand,
  DeleteSnapshotCommand,
  DeleteVolumeCommand,
  DescribeInstancesCommand,
  DescribeSnapshotsCommand,
  DescribeVolumesCommand,
  DetachVolumeCommand,
  EC2Client,
  Snapshot,
  StartInstancesCommand,
  StopInstancesCommand
} from '@aws-sdk/client-ec2';
import { ChangeResourceRecordSetsCommand, Route53Client } from '@aws-sdk/client-route-53';
import { GetCommandInvocationCommand, SendCommandCommand, SSMClient } from '@aws-sdk/client-ssm';

const APP_NAME = process.env.APP_NAME!;
const EC2_INSTANCE_ID = process.env.EC2_INSTANCE_ID!;
const MAX_IDLE_DURATION_IN_MINUTES = Number(process.env.MAX_IDLE_DURATION_IN_MINUTES);
const WORLD_VOLUME_ID = process.env.WORLD_VOLUME_ID!;
const SNAPSHOTS_TO_KEEP = Number(process.env.SNAPSHOTS_TO_KEEP);
const HOSTED_ZONE_ID = process.env.HOSTED_ZONE_ID;
const DOMAIN_NAME = process.env.DOMAIN_NAME;

const INTERACTION_TYPE_APPLICATION_COMMAND = 2;
const INTERACTION_TYPE_MESSAGE_COMPONENT = 3;
const INTERACTION_TYPE_AUTOCOMPLETE = 4;
const COMPONENT_TYPE_ACTION_ROW = 1;
const COMPONENT_TYPE_BUTTON = 2;
const BUTTON_STYLE_SECONDARY = 2;
const BUTTON_STYLE_DANGER = 4;

const MINECRAFT_IDLE_CHECK = 'minecraft-idle-check'
const MINECRAFT_STOP_CONFIRM = 'minecraft_stop_confirm';
const MINECRAFT_STOP_CANCEL = 'minecraft_stop_cancel';
const MINECRAFT_RESTART_CONFIRM = 'minecraft_restart_confirm';
const MINECRAFT_RESTART_CANCEL = 'minecraft_restart_cancel';
const MINECRAFT_WIPE_CONFIRM = 'minecraft_wipe_confirm';
const MINECRAFT_WIPE_CANCEL = 'minecraft_wipe_cancel';
const MINECRAFT_RESTORE_CONFIRM = 'minecraft_restore_confirm';
const MINECRAFT_RESTORE_CANCEL = 'minecraft_restore_cancel';

const ec2Client = new EC2Client({});
const ssmClient = new SSMClient({});
const route53Client = new Route53Client({});

export const handler = async (event: any) => {
  if (event.type === MINECRAFT_IDLE_CHECK) {
    return await handleMinecraftIdleCheck();
  }

  const interaction = event.interaction;

  if (!interaction) {
    throw new Error('Missing interaction');
  }
  
  try {
    const interactionType = interaction.type;
    const isAdmin = event.isAdmin;

    // Application command
    if (interactionType === INTERACTION_TYPE_APPLICATION_COMMAND) {
      const commandName = interaction.data?.name;
      console.log(`Processing command: ${commandName}`);

      switch (commandName) {
        case 'status':
          return await handleStatus(interaction);
        case 'start':
          return await handleStart(interaction);
        case 'stop':
          return await handleStop(interaction, isAdmin, false);
        case 'restart':
          return await handleRestart(interaction, isAdmin, false);
        case 'wipe':
          return await handleWipe(interaction, false);
        case 'restore':
          return await handleRestore(interaction);
        case 'run':
          return await handleRun(interaction);
        default:
          throw new Error(`Unknown command: ${commandName}`);
      }
    }

    // Message component
    if (interactionType === INTERACTION_TYPE_MESSAGE_COMPONENT) {
      const customId = interaction.data?.custom_id;

      if (customId === MINECRAFT_STOP_CONFIRM) {
        return await handleStop(interaction, isAdmin, true);
      }

      if (customId === MINECRAFT_RESTART_CONFIRM) {
          return await handleRestart(interaction, isAdmin, true);
      }

      if (customId === MINECRAFT_WIPE_CONFIRM) {
          return await handleWipe(interaction, true);
      }

      if (customId.startsWith(MINECRAFT_RESTORE_CONFIRM)) {
        const snapshotId = customId.split(":")[1];
        return await handleRestore(interaction, snapshotId);
      }

      throw new Error(`Unknown component: ${interaction.data?.custom_id}`);
    }

    // Autocomplete request
    if (interaction.type === INTERACTION_TYPE_AUTOCOMPLETE) {
      const commandName = interaction.data?.name;
      console.log(`Processing autocomplete for command: ${commandName}`);

      switch (commandName) {
        case 'restore':
          return await handleRestoreAutocomplete(interaction);
        default:
          throw new Error(`Unknown command: ${commandName}`);
      }
    }

    throw new Error(`Unknown interaction type: ${interactionType}`);
  } catch (error: any) {
    console.error('Failed to process interaction: ', error);
    await updateDiscordResponse(interaction, `❌ An error occurred: ${error.message}`);
  }
};

async function handleMinecraftIdleCheck() {
  const instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'running') {
    return;
  }

  const minecraftStatus = await getMinecraftStatus();
  console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

  if (!minecraftStatus.idle || !minecraftStatus.idleSince) {
    return;
  }

  const idleSince = new Date(minecraftStatus.idleSince);

  if (Number.isNaN(idleSince.getTime())) {
    console.error(`Invalid idleSince: ${minecraftStatus.idleSince}`);
    return;
  }

  const idleMinutes = (Date.now() - idleSince.getTime()) / 1000 / 60;
  console.log(`Minecraft has been idle for ${idleMinutes.toFixed(1)} minutes (timeout: ${MAX_IDLE_DURATION_IN_MINUTES} minutes)`);

  if (idleMinutes < MAX_IDLE_DURATION_IN_MINUTES) {
    return;
  }

  console.log('Minecraft idle timeout reached, stopping server...');

  if (minecraftStatus.running) {
    await runServerScript('stop.sh', 60);
  }

  await stopEc2();

  try {
    await createWorldSnapshot();
  } catch (error: any) {
    console.error('Failed to create world snapshot: ', error);
  }

  return;
}

async function handleStatus(interaction: any) {
  const instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'running') {
    await updateDiscordResponse(interaction, formatServerStatus(instanceStatus.state, 'offline'));
    return;
  }

  const minecraftStatus = await getMinecraftStatus();
  console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

  if (!minecraftStatus.running) {
    await updateDiscordResponse(interaction, formatServerStatus(instanceStatus.state, 'offline'));
    return;
  }

  await updateDiscordResponse(interaction, formatServerStatus(instanceStatus.state, 'online', minecraftStatus.players.join(', ') || 'N/A'));
}

async function getEc2Status(): Promise<{ state: string, ipAddress: string, availabilityZone?: string }> {
  const response = await ec2Client.send(new DescribeInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));

  const instance = response.Reservations?.[0]?.Instances?.[0];

  if (!instance) {
    throw new Error('EC2 instance not found');
  }

  return {
    state: instance.State?.Name ?? 'unknown',
    ipAddress: instance.PublicIpAddress || 'unknown',
    availabilityZone: instance.Placement?.AvailabilityZone
  }
}

function formatServerStatus(ec2InstanceState: string, minecraftServerState: 'online' | 'offline', players?: string): string {
  let serverStatus = `🖥️ **Server:** ${ec2InstanceState}` + `\n🎮 **Minecraft:** ${minecraftServerState}`;

  if (minecraftServerState === 'online') {
    serverStatus += `\n👥 **Players:** ${players}`
  }

  return serverStatus;
}

async function getMinecraftStatus(): Promise<{
  running: boolean;
  players: string[];
  idle: boolean;
  idleSince: string | null
}> {
  const minecraftStatus = await runServerScript('status.sh', 10);

  try {
    return JSON.parse(minecraftStatus);
  } catch (error: any) {
    console.error('Invalid Minecraft status JSON: ', error);
    throw new Error(`Invalid Minecraft status JSON: ${JSON.stringify(minecraftStatus)}`);
  }
}

async function runServerScript(scriptName: string, executionTimeoutInSeconds: number, ...args: string[]): Promise<string> {
  const command = [
    `/opt/minecraft/server/scripts/${scriptName}`,
    ...args.map(arg => `'${arg.replace(/'/g, "'\\''")}'`)
  ].join(' ');

  let response;
  for (let attempt = 0; attempt < 30; attempt++) {
    try {
      response = await ssmClient.send(new SendCommandCommand({
        InstanceIds: [EC2_INSTANCE_ID],
        DocumentName: 'AWS-RunShellScript',
        TimeoutSeconds: 30,
        Parameters: {
          commands: [command],
          executionTimeout: [executionTimeoutInSeconds.toString()]
        }
      }));

      break;
    } catch (error: any) {
      console.log(`SSM is not ready yet, waiting... attempt ${attempt + 1}/30`);
      await sleep(2000);
    }
  }

  const commandId = response?.Command?.CommandId;

  if (!commandId) {
    throw new Error('Timed out waiting for SSM agent to become ready');
  }

  // Wait for the SSM command to finish
  const maxAttempts = executionTimeoutInSeconds + 10
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    console.log(`Waiting for SSM command... attempt ${attempt + 1}/${maxAttempts}}`);
    await sleep(1000);

    const result = await ssmClient.send(new GetCommandInvocationCommand({ CommandId: commandId, InstanceId: EC2_INSTANCE_ID }));

    if (result.Status === 'Pending' || result.Status === 'InProgress') {
      continue;
    }

    if (result.Status !== 'Success') {
      console.error('SSM command failed', {
        status: result.Status,
        stdout: result.StandardOutputContent,
        stderr: result.StandardErrorContent
      });

      throw new Error(`SSM command failed with status: ${result.Status}`);
    }

    const output = result.StandardOutputContent?.trim() ?? '';
    console.log(`SSM command output: ${output}`);

    return output;
  }

  throw new Error('Timed out waiting for SSM command');
}

async function handleStart(interaction: any) {
  let instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'stopped' && instanceStatus.state !== 'running') {
    await updateDiscordResponse(interaction, 'The Minecraft server cannot be started right now, please wait a moment and try again');
    return;
  }

  if (instanceStatus.state === 'stopped') {
    await ec2Client.send(new StartInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));

    instanceStatus = await waitForEc2();
    await updateMinecraftDns(instanceStatus.ipAddress);

    await waitForMinecraft();

    await updateDiscordResponse(interaction, 'The Minecraft server is started and ready to use');
    return;
  }

  const minecraftStatus = await getMinecraftStatus();
  console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

  if (minecraftStatus.running) {
    await updateDiscordResponse(interaction, 'The Minecraft server is already started');
    return;
  }

  await runServerScript('deploy.sh', 60);
  await waitForMinecraft();

  await updateDiscordResponse(interaction, 'The Minecraft server is started and ready to use');
  return;
}

async function waitForEc2(): Promise<{ state: string, ipAddress: string, availabilityZone?: string }> {
  let instanceStatus;

  for (let attempt = 0; attempt < 30; attempt++) {
    console.log(`Waiting for EC2... attempt ${attempt + 1}/30`);
    
    instanceStatus = await getEc2Status();

    if (instanceStatus.state === 'running') {
      console.log('EC2 is running');
      return instanceStatus;
    }

    await sleep(4000);
  }

  throw new Error('Timed out waiting for EC2 to start');
}

async function waitForMinecraft(): Promise<void> {
  for (let attempt = 0; attempt < 30; attempt++) {
    console.log(`Waiting for Minecraft... attempt ${attempt + 1}/30`);

    try {
      const minecraftStatus = await getMinecraftStatus();
      if (minecraftStatus.running) {
        console.log('Minecraft is running');
        return;
      }
    } catch (error: any) {
      console.log('Minecraft status check unavailable: ', error);
    }

    await sleep(5000);
  }

  throw new Error('Timed out waiting for Minecraft to start');
}

async function updateMinecraftDns(publicIp: string) {
  console.log(`Updating minecraft.${DOMAIN_NAME} to ${publicIp}`);

  await route53Client.send(new ChangeResourceRecordSetsCommand({
    HostedZoneId: HOSTED_ZONE_ID,
    ChangeBatch: {
      Changes: [
        {
          Action: 'UPSERT',
          ResourceRecordSet: {
            Name: `minecraft.${DOMAIN_NAME}`,
            Type: 'A',
            TTL: 30,
            ResourceRecords: [
              {
                Value: publicIp
              }
            ]
          }
        }
      ]
    }
  }));

  console.log(`Updated minecraft.${DOMAIN_NAME} to ${publicIp}`);
}

async function handleStop(interaction: any, isAdmin: boolean, confirmed: boolean) {
  const instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state === 'stopped') {
    await updateDiscordResponse(interaction, 'The Minecraft server is already stopped');
    return;
  }

  if (instanceStatus.state === 'running') {
    const minecraftStatus = await getMinecraftStatus();
    console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

    if (minecraftStatus.running) {
      if (minecraftStatus.players.length > 0) {
        if (!isAdmin) {
          await updateDiscordResponse(interaction, 'You cannot stop the Minecraft server while players are online');
          return;
        }

        if (!confirmed) {
          await updateDiscordResponse(interaction, 'There are still players in the game, are you sure you want to stop the server?', MINECRAFT_STOP_CONFIRM, MINECRAFT_STOP_CANCEL);
          return;
        }
      }

      await runServerScript('stop.sh', 60);
    }

    await stopEc2();

    let snapshotCreated = false;
    try {
      await createWorldSnapshot();
      snapshotCreated = true;
    } catch (error: any) {
      console.error('Failed to create world snapshot:', error);
    }

    if (snapshotCreated) {
      await updateDiscordResponse(interaction, 'The Minecraft server was successfully stopped and a backup was created');
    } else {
      await updateDiscordResponse(interaction, 'The Minecraft server was successfully stopped but could not be backed up');
    }

    return;
  }

  await updateDiscordResponse(interaction, 'The Minecraft server cannot be stopped right now, please wait a moment and try again');
}

async function stopEc2() {
  await ec2Client.send(new StopInstancesCommand({
    InstanceIds: [EC2_INSTANCE_ID]
  }));

  for (let attempt = 0; attempt < 60; attempt++) {
    console.log(`Waiting for EC2 to stop... attempt ${attempt + 1}/60`);
    await sleep(2000);

    const instanceStatus = await getEc2Status();

    if (instanceStatus.state === 'stopped') {
      return;
    }

    if (instanceStatus.state !== 'stopping') {
      throw new Error(`EC2 entered unexpected state while stopping: ${instanceStatus.state}`);
    }
  }

  throw new Error('Timed out waiting for EC2 to stop');
}

async function createWorldSnapshot() {
  const response = await ec2Client.send(new CreateSnapshotCommand({
    VolumeId: WORLD_VOLUME_ID,
    Description: 'Minecraft world backup',
    TagSpecifications: [
      {
        ResourceType: 'snapshot',
        Tags: [
          {
            Key: 'Application',
            Value: APP_NAME
          }
        ]
      }
    ]
  }));

  const snapshotId = response.SnapshotId;

  if (!snapshotId) {
    throw new Error('Snapshot creation did not return a snapshot ID');
  }

  console.log(`Created snapshot: ${snapshotId}`);

  for (let attempt = 0; attempt < 30; attempt++) {
    console.log(`Waiting for snapshot... attempt ${attempt + 1}/30`);
    await sleep(4000);

    const result = await ec2Client.send(new DescribeSnapshotsCommand({
      SnapshotIds: [snapshotId]
    }));

    const snapshot = result.Snapshots?.[0];
    if (!snapshot) {
      throw new Error(`Snapshot ${snapshotId} could not be found`);
    }

    console.log(`Snapshot ${snapshotId} state: ${snapshot.State}`);

    if (snapshot.State === 'completed') {
      try {
        rotateWorldSnapshots()
      } catch (error: any) {
        console.error('Failed to rotate world snapshots: ', error);
      }
      return;
    }

    if (snapshot.State === 'error') {
      throw new Error(`Snapshot ${snapshotId} entered error state`);
    }
  }

  throw new Error(`Timed out waiting for snapshot ${snapshotId}`);
}

async function rotateWorldSnapshots() {
  const snapshots = await getWorldSnapshots();
  snapshots.sort((first, second) => (first.StartTime?.getTime() ?? 0) - (second.StartTime?.getTime() ?? 0));

  console.log(`Found ${snapshots.length} Minecraft world snapshots`);

  const snapshotsToDelete = snapshots.slice(0, Math.max(0, snapshots.length - SNAPSHOTS_TO_KEEP));

  for (const snapshot of snapshotsToDelete) {
    if (!snapshot.SnapshotId) {
      console.log("continuing");
      continue;
    }

    console.log(`Deleting old snapshot ${snapshot.SnapshotId}`);

    await ec2Client.send(new DeleteSnapshotCommand({
      SnapshotId: snapshot.SnapshotId
    }));

    console.log(`Deleted snapshot ${snapshot.SnapshotId}`);
  }
}

async function getWorldSnapshots(): Promise<Snapshot[]> {
  const response = await ec2Client.send(new DescribeSnapshotsCommand({
    Filters: [
      {
        Name: 'volume-id',
        Values: [WORLD_VOLUME_ID]
      },
      {
        Name: 'tag:Application',
        Values: [APP_NAME]
      },
      {
        Name: 'status',
        Values: ['completed']
      }
    ]
  }));

  return response.Snapshots ?? [];
}

async function handleRestart(interaction: any, isAdmin: boolean, confirmed: boolean) {
  let instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'stopped' && instanceStatus.state !== 'running') {
    await updateDiscordResponse(interaction, 'The Minecraft server cannot be restarted right now, please wait a moment and try again');
    return;
  }

  if (instanceStatus.state === 'stopped') {
    await ec2Client.send(new StartInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));

    instanceStatus = await waitForEc2();
    await updateMinecraftDns(instanceStatus.ipAddress);
  }

  const minecraftStatus = await getMinecraftStatus();
  console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

  if (minecraftStatus.running) {
    if (minecraftStatus.players.length > 0) {
      if (!isAdmin) {
        await updateDiscordResponse(interaction, 'You cannot restart the Minecraft server while players are online');
        return;
      }

      if (!confirmed) {
        await updateDiscordResponse(interaction, 'There are still players in the game, are you sure you want to restart the server?', MINECRAFT_RESTART_CONFIRM, MINECRAFT_RESTART_CANCEL);
        return;
      }

      await runServerScript('stop.sh', 60);
    }
  }

  await runServerScript('deploy.sh', 60);
  await waitForMinecraft();

  await updateDiscordResponse(interaction, 'The Minecraft server was successfully restarted');
}

async function handleWipe(interaction: any, confirmed: boolean) {
  const instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'running') {
    await updateDiscordResponse(interaction, 'The server is not running, use /start first');
    return;
  }

  if (!confirmed) {
    await updateDiscordResponse(interaction, 'Are you sure you want to wipe the Minecraft world?', MINECRAFT_WIPE_CONFIRM, MINECRAFT_WIPE_CANCEL);
    return;
  }

  await runServerScript('stop.sh', 60);
  await runServerScript('wipe.sh', 60);

  await updateDiscordResponse(interaction, 'The Minecraft world was successfully wiped');
}

async function handleRestoreAutocomplete(interaction: any) {
  const query = interaction.data?.options?.find((option: any) => option.name === 'backup')?.value ?? '';

  const snapshots = (await getWorldSnapshots())
    .filter(snapshot => snapshot.SnapshotId && snapshot.StartTime)
    .sort((first, second) => (second.StartTime?.getTime() ?? 0) - (first.StartTime?.getTime() ?? 0))
    .map(snapshot => ({ name: formatSnapshotDate(snapshot.StartTime!), value: snapshot.SnapshotId }))
    .filter(snapshot => snapshot.name.includes(query))

  console.log(`Found ${snapshots.length} Minecraft world snapshots`);

  return snapshots;
}

function formatSnapshotDate(date: Date): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Toronto',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: false,
    timeZoneName: 'short'
  }).format(date);
}

async function handleRestore(interaction: any, confirmedSnapshotId?: string) {
  let instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'running') {
    await updateDiscordResponse(interaction, 'The server is not started, use /start first');
    return;
  }

  const snapshotId = confirmedSnapshotId ? confirmedSnapshotId : interaction.data?.options?.find((option: any) => option.name === 'backup')?.value;

  if (!snapshotId) {
    await updateDiscordResponse(interaction, 'No backup was specified');
    return;
  }

  const snapshotToRestore = (await getWorldSnapshots()).find(snapshot => snapshot.SnapshotId === snapshotId);

  if (!snapshotToRestore) {
    await updateDiscordResponse(interaction, 'The selected backup could not be found');
    return;
  }

  if (!snapshotToRestore.StartTime) {
    await updateDiscordResponse(interaction, 'The selected backup could not be restored, please try another backup');
    return;
  }

  if (!confirmedSnapshotId) {
    const snapshotDate = formatSnapshotDate(snapshotToRestore.StartTime);
    await updateDiscordResponse(interaction, `Restore Minecraft world with backup **${snapshotDate}**?`, `${MINECRAFT_RESTORE_CONFIRM}:${snapshotId}`, MINECRAFT_RESTORE_CANCEL);
    return;
  }

  console.log(`Restoring world snapshot with ID: ${snapshotToRestore?.SnapshotId}`);

  const minecraftStatus = await getMinecraftStatus();
  console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

  if (minecraftStatus.running) {
    await runServerScript('stop.sh', 60);
  }

  let temporaryVolumeId: string | undefined;
  let temporaryVolumeAttached = false;

  try {
    temporaryVolumeId = await createRestoreVolume(snapshotId, instanceStatus.availabilityZone!);
    console.log(`Created temporary restore volume ${temporaryVolumeId}`);

    await attachRestoreVolume(temporaryVolumeId, EC2_INSTANCE_ID);
    temporaryVolumeAttached = true;
    console.log(`Attached temporary restore volume ${temporaryVolumeId}`);

    await runServerScript('restore-world.sh', 300, temporaryVolumeId);

    await detachRestoreVolume(temporaryVolumeId);
    temporaryVolumeAttached = false;

    await deleteRestoreVolume(temporaryVolumeId);
    temporaryVolumeId = undefined;

    await updateDiscordResponse(interaction, 'The Minecraft world was successfully restored');
  } catch (error: any) {
    console.error('Minecraft world restore failed: ', error);

    // Try to detach the temporary volume if necessary.
    try {
      if (temporaryVolumeId && temporaryVolumeAttached) {
        await detachRestoreVolume(temporaryVolumeId);
      }

      if (temporaryVolumeId) {
        await deleteRestoreVolume(temporaryVolumeId);
      }

      await updateDiscordResponse(interaction, `❌ An error occurred: ${error.message}`);
    } catch (cleanupError) {
      console.error('Failed to clean up the temporary restore volume: ', cleanupError);
      throw new Error('Failed to restore the backup, and temporary volume cleanup failed, manual intervention may be required');
    }
  }
}

async function createRestoreVolume(snapshotId: string, availabilityZone: string): Promise<string> {
  console.log(`Creating temporary restore volume from snapshot ${snapshotId}...`);

  const response = await ec2Client.send(new CreateVolumeCommand({
    SnapshotId: snapshotId,
    AvailabilityZone: availabilityZone,
    VolumeType: 'gp3',
    TagSpecifications: [
      {
        ResourceType: 'volume',
        Tags: [
          { Key: 'Application', Value: APP_NAME }
        ]
      }
    ]
  }));

  const volumeId = response.VolumeId;

  if (!volumeId) {
    throw new Error('Creating the temporary restore volume did not return a volume ID');
  }

  for (let attempt = 0; attempt < 60; attempt++) {
    console.log(`Waiting for temporary restore volume to become available... attempt ${attempt + 1}/60`);
    await sleep(2000);

    const result = await ec2Client.send(new DescribeVolumesCommand({
      VolumeIds: [volumeId]
    }));

    const volume = result.Volumes?.[0];

    if (!volume) {
      throw new Error(`Temporary volume ${volumeId} could not be found`);
    }

    console.log(`Temporary volume ${volumeId} state: ${volume.State}`);

    if (volume.State === 'available') {
      return volumeId;
    }

    if (volume.State === 'error') {
      throw new Error(`Temporary volume ${volumeId} entered an error state`);
    }
  }

  throw new Error(`Timed out waiting for temporary volume ${volumeId} to become available`);
}

async function attachRestoreVolume(volumeId: string, instanceId: string) {
  const device = '/dev/sdg';
  console.log(`Attaching temporary restore volume ${volumeId} to ${instanceId} as ${device}...`);

  await ec2Client.send(new AttachVolumeCommand({
    VolumeId: volumeId,
    InstanceId: instanceId,
    Device: device
  }));

  for (let attempt = 0; attempt < 60; attempt++) {
    console.log(`Waiting for EBS volume to attach... attempt ${attempt + 1}/60`);
    await sleep(2000);

    const result = await ec2Client.send(new DescribeVolumesCommand({
      VolumeIds: [volumeId]
    }));

    const volume = result.Volumes?.[0];

    if (!volume) {
      throw new Error(`Temporary volume ${volumeId} could not be found`);
    }

    const attachment = volume.Attachments?.find(attachment => attachment.InstanceId === instanceId);
    console.log(`Temporary volume ${volumeId} attachment state: ${attachment?.State}`);

    if (attachment?.State === 'attached') {
      return;
    }

    if (attachment?.State === 'detached') {
      throw new Error(`Temporary volume ${volumeId} unexpectedly became detached`);
    }
  }

  throw new Error(`Timed out waiting for temporary volume ${volumeId} to attach`);
}

async function detachRestoreVolume(volumeId: string) {
  console.log(`Detaching temporary restore volume ${volumeId}...`);

  await ec2Client.send(new DetachVolumeCommand({
    VolumeId: volumeId
  }));

  for (let attempt = 0; attempt < 60; attempt++) {
    console.log(`Waiting for EBS volume to detach... attempt ${attempt + 1}/60`);
    await sleep(2000);

    const result = await ec2Client.send(new DescribeVolumesCommand({
      VolumeIds: [volumeId]
    }));

    const volume = result.Volumes?.[0];

    if (!volume) {
      throw new Error(`Temporary volume ${volumeId} could not be found`);
    }

    console.log(`Temporary volume ${volumeId} state: ${volume.State}`);

    if (volume.State === 'available') {
      return;
    }

    if (volume.State === 'error') {
      throw new Error(`Temporary volume ${volumeId} entered an error state while detaching`);
    }
  }

  throw new Error(`Timed out waiting for temporary volume ${volumeId} to detach`);
}

async function deleteRestoreVolume(volumeId: string) {
 console.log(`Deleting temporary restore volume ${volumeId}...`);

  await ec2Client.send(new DeleteVolumeCommand({
      VolumeId: volumeId
  }));

  console.log(`Deleted temporary restore volume ${volumeId}`);
}

async function handleRun(interaction: any) {
  const instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state !== 'running') {
    await updateDiscordResponse(interaction, 'The Minecraft server is not running');
    return;
  }

  const minecraftStatus = await getMinecraftStatus();
  console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

  if (!minecraftStatus.running) {
    await updateDiscordResponse(interaction, 'The Minecraft server is not running');
    return;
  }

  const command = interaction.data?.options?.find((option: any) => option.name === 'command')?.value as string;

  if (!command) {
    await updateDiscordResponse(interaction, 'No Minecraft command was provided');
    return;
  }

  const output = await runServerScript('run-command.sh', 60, command);
  await updateDiscordResponse(interaction, output ? `\`\`\`\n${output}\n\`\`\`` : 'Command executed successfully');
}

async function updateDiscordResponse(interaction: any, content: string, confirmId?: string, cancelId?: string) {
  const applicationId = interaction.application_id;
  const interactionToken = interaction.token;

  const url = `https://discord.com/api/v10/webhooks/${applicationId}/${interactionToken}/messages/@original`;
  const body: any = { content };

  if (confirmId && cancelId) {
    const components = [
      {
        type: COMPONENT_TYPE_ACTION_ROW,
        components: [
          {
            type: COMPONENT_TYPE_BUTTON,
            style: BUTTON_STYLE_DANGER,
            label: `Confirm ${interaction.data?.name}`,
            custom_id: confirmId
          },
          {
            type: COMPONENT_TYPE_BUTTON,
            style: BUTTON_STYLE_SECONDARY,
            label: 'Cancel',
            custom_id: cancelId
          }
        ]
      }
    ]

    body.components = components;
  }

  const response = await fetch(url, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Failed to update Discord response: ${response.status} ${body}`);
  }
}

function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
