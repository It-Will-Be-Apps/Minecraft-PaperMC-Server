import { DescribeInstancesCommand, EC2Client, StartInstancesCommand, StopInstancesCommand } from '@aws-sdk/client-ec2';
import { GetCommandInvocationCommand, SendCommandCommand, SSMClient } from '@aws-sdk/client-ssm';

const EC2_INSTANCE_ID = process.env.EC2_INSTANCE_ID!;

const ec2Client = new EC2Client({});
const ssmClient = new SSMClient({});

export const handler = async (event: any) => {
  const interaction = event.interaction;

  if (!interaction) {
    throw new Error('Missing interaction');
  }

  try {
    const commandName = interaction.data?.name;
    console.log(`Processing command: ${commandName}`);

    switch (commandName) {
      case 'status':
        return await handleStatus(interaction);
      case 'start':
        return await handleStart(interaction);
      case 'stop':
        return await handleStop(interaction);
      case 'restart':
        return await handleRestart(interaction);
      case 'wipe':
        return await handleWipe(interaction);
      case 'restore':
        return await handleRestore(interaction);
      case 'run':
        return await handleRun(interaction);
      default:
        throw new Error(`Unknown command: ${commandName}`);
    }
  } catch (error: any) {
    console.error(`Failed to process interaction: ${error}`);
    await updateDiscordResponse(interaction, `❌ An error occurred: ${error.message}`);
  }
};

async function handleStatus(interaction: any) {
  const response = await ec2Client.send(new DescribeInstancesCommand({
    InstanceIds: [EC2_INSTANCE_ID]
  }));

  const instance = response.Reservations?.[0]?.Instances?.[0];
  if (!instance) {
    throw new Error('EC2 instance not found');
  }

  const instanceStatus = instance.State?.Name ?? 'unknown';
  console.log(`EC2 instance status: ${instanceStatus}`);

  if (instanceStatus !== 'running') {
    await updateDiscordResponse(interaction, formatServerStatus(instanceStatus, 'offline'));
    return;
  }

  const minecraftStatus = parseMinecraftStatus(await runServerScript('status.sh'));
  console.log(`Minecraft server status: ${minecraftStatus}`);

  if (!minecraftStatus.running) {
    await updateDiscordResponse(interaction, formatServerStatus(instanceStatus, 'offline'));
    return;
  }

  await updateDiscordResponse(interaction, formatServerStatus(instanceStatus, 'online', minecraftStatus.playersCount));
}

function formatServerStatus(ec2InstanceState: string, minecraftServerState: 'online' | 'offline', playersCount?: number): string {
  let serverStatus = `🖥️ **Instance:**: ${ec2InstanceState}` + `\n🎮 **Minecraft:** ${minecraftServerState}`;

  if (minecraftServerState === 'online') {
    serverStatus += `\n👥 **Players:** ${playersCount}`
  }

  return serverStatus;
}

function parseMinecraftStatus(minecraftStatus: string): { running: boolean; playersCount: number } {
  if (minecraftStatus.includes('OFFLINE')) {
    return { running: false, playersCount: 0 };
  }

  if (!minecraftStatus.includes('ONLINE')) {
    throw new Error(`Unexpected Minecraft status: ${minecraftStatus}`);
  }

  const match = minecraftStatus.match(/There are (\d+) of a max of \d+ players online/);

  if (!match) {
    throw new Error(`Could not parse player count from RCON output: ${minecraftStatus}`);
  }

  return { running: true, playersCount: Number(match[1]) };
}

async function runServerScript(scriptName: string): Promise<string> {
  const command = await ssmClient.send(new SendCommandCommand({
    InstanceIds: [EC2_INSTANCE_ID],
    DocumentName: 'AWS-RunShellScript',
    TimeoutSeconds: 30,
    Parameters: {
      commands: [`/opt/minecraft/scripts/${scriptName}`],
      executionTimeout: ['10']
    }
  }));

  const commandId = command.Command?.CommandId;

  if (!commandId) {
    throw new Error('SSM command did not return a command ID');
  }

  // Wait for the SSM command to finish
  for (let attempt = 0; attempt < 10; attempt++) {
    await sleep(1000);

    const result = await ssmClient.send(new GetCommandInvocationCommand({
      CommandId: commandId,
      InstanceId: EC2_INSTANCE_ID
    }));

    if (result.Status === 'Pending' || result.Status === 'InProgress') {
      continue;
    }

    if (result.Status !== 'Success') {
      throw new Error(`SSM command failed with status: ${result.Status}`);
    }

    const output = result.StandardOutputContent?.trim() ?? '';
    console.log(`SSM command output: ${output}`);

    return output;
  }

  throw new Error('Timed out waiting for SSM command');
}

async function handleStart(interaction: any) {
  // console.log(`EC2 instance state: ${state}`); TODO at least 1 log
  await ec2Client.send(new StartInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));
  await updateDiscordResponse(interaction, 'The /start command is not yet implemented');
}

async function handleStop(interaction: any) {
  // console.log(`EC2 instance state: ${state}`); TODO at least 1 log
  await ec2Client.send(new StopInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));
  await updateDiscordResponse(interaction, 'The /stop command is not yet implemented');
}

async function handleRestart(interaction: any) {
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
  // console.log(`EC2 instance state: ${state}`); TODO at least 1 log
  await updateDiscordResponse(interaction, 'The /run command is not yet implemented')
}

async function updateDiscordResponse(interaction: any, content: string) {
  const applicationId = interaction.application_id;
  const interactionToken = interaction.token;

  const url = `https://discord.com/api/v10/webhooks/${applicationId}/${interactionToken}/messages/@original`;

  const response = await fetch(url, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ content })
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Failed to update Discord response: ${response.status} ${body}`);
  }
}

function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
