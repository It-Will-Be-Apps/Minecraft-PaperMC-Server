import { CreateSnapshotCommand, DescribeInstancesCommand, DescribeSnapshotsCommand, EC2Client, StartInstancesCommand, StopInstancesCommand } from '@aws-sdk/client-ec2';
import { GetCommandInvocationCommand, SendCommandCommand, SSMClient } from '@aws-sdk/client-ssm';

const APP_NAME = process.env.APP_NAME!;
const EC2_INSTANCE_ID = process.env.EC2_INSTANCE_ID!;
const MAX_IDLE_DURATION_IN_MINUTES = Number(process.env.MAX_IDLE_DURATION_IN_MINUTES);
const WORLD_VOLUME_ID = process.env.WORLD_VOLUME_ID;

const INTERACTION_TYPE_APPLICATION_COMMAND = 2;
const INTERACTION_TYPE_MESSAGE_COMPONENT = 3;
const COMPONENT_TYPE_ACTION_ROW = 1;
const COMPONENT_TYPE_BUTTON = 2;
const BUTTON_STYLE_SECONDARY = 2;
const BUTTON_STYLE_DANGER = 4;

const MINECRAFT_IDLE_CHECK = 'minecraft-idle-check'
const MINECRAFT_STOP_CONFIRM = 'minecraft_stop_confirm';
const MINECRAFT_STOP_CANCEL = 'minecraft_stop_cancel';

const ec2Client = new EC2Client({});
const ssmClient = new SSMClient({});

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
          return await handleRestart(interaction, isAdmin);
        case 'wipe':
          return await handleWipe(interaction);
        case 'restore':
          return await handleRestore(interaction);
        case 'run':
          return await handleRun(interaction);
        default:
          throw new Error(`Unknown command: ${commandName}`);
      }
    }

    if (interactionType === INTERACTION_TYPE_MESSAGE_COMPONENT) {
      switch (interaction.data?.custom_id) {
        case MINECRAFT_STOP_CONFIRM:
          return await handleStop(interaction, isAdmin, true);
        default:
          throw new Error(`Unknown component: ${interaction.data?.custom_id}`);
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

async function getEc2Status(): Promise<{ state: string, ipAddress: string }> { // TODO 
  const response = await ec2Client.send(new DescribeInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));

  const instance = response.Reservations?.[0]?.Instances?.[0];
  if (!instance) {
    throw new Error('EC2 instance not found');
  }

  return {
    state: instance.State?.Name ?? 'unknown',
    ipAddress: instance.PublicIpAddress || 'unknown'
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

  const response = await ssmClient.send(new SendCommandCommand({
    InstanceIds: [EC2_INSTANCE_ID],
    DocumentName: 'AWS-RunShellScript',
    TimeoutSeconds: 30,
    Parameters: {
      commands: [command],
      executionTimeout: [executionTimeoutInSeconds.toString()]
    }
  }));

  const commandId = response.Command?.CommandId;

  if (!commandId) {
    throw new Error('SSM command did not return a command ID');
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
  const instanceStatus = await getEc2Status();
  console.log(`EC2 instance status: ${instanceStatus.state}`);

  if (instanceStatus.state === 'running') {
    const minecraftStatus = await getMinecraftStatus();
    console.log(`Minecraft server status: ${JSON.stringify(minecraftStatus)}`);

    if (minecraftStatus.running) {
      await updateDiscordResponse(interaction, 'The Minecraft server is already started');
      return;
    }

    await runServerScript('deploy.sh', 30);
    await waitForMinecraft();

    await updateDiscordResponse(interaction, 'The Minecraft server is started and ready to use');
    return;
  }

  if (instanceStatus.state === 'stopped') {
    await ec2Client.send(new StartInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));

    await waitForMinecraft();
    const postStartStatus = await getEc2Status(); // TODO remove when no longer need IP

    await updateDiscordResponse(interaction, `The Minecraft server is started and ready to use, with IP address: ${postStartStatus.ipAddress}`);
    return;
  }

  await updateDiscordResponse(interaction, 'The Minecraft server cannot be started right now, please wait a moment and try again');
}

async function waitForMinecraft(): Promise<void> {
  for (let attempt = 0; attempt < 30; attempt++) {
    console.log(`Waiting for Minecraft... attempt ${attempt + 1}/30`);

    const instanceStatus = await getEc2Status();

    if (instanceStatus.state !== 'running') {
      await sleep(4000);
      continue;
    }

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
          await updateDiscordResponse(interaction, 'You cannot stop the Minecraft server while players are online'); // TODO
          return;
        }

        if (!confirmed) {
          await updateDiscordResponse(interaction, 'There are still players in the game, are you sure you want to stop the server?', MINECRAFT_STOP_CONFIRM, MINECRAFT_STOP_CANCEL); // TODO
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
      await updateDiscordResponse(interaction, 'The Minecraft server is stopped and a backup was created');
    } else {
      await updateDiscordResponse(interaction, 'The Minecraft server is stopped but could not be backed up');
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
      throw new Error(`EC2 entered unexpected state while stopping: ${instanceStatus}`);
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
      return;
    }

    if (snapshot.State === 'error') {
      throw new Error(`Snapshot ${snapshotId} entered error state`);
    }
  }

  throw new Error(`Timed out waiting for snapshot ${snapshotId}`);
}

async function handleRestart(interaction: any, isAdmin: boolean) {
  // console.log(`EC2 instance state: ${state}`); TODO at least 1 log
  await updateDiscordResponse(interaction, 'The /restart command is not yet implemented');
}

async function handleWipe(interaction: any) {
  // console.log(`EC2 instance state: ${state}`); TODO at least 1 log
  await updateDiscordResponse(interaction, 'The /wipe command is not yet implemented');
}

async function handleRestore(interaction: any) {
  // console.log(`EC2 instance state: ${state}`); TODO at least 1 log
  await updateDiscordResponse(interaction, 'The /restore command is not yet implemented')
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
