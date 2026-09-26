import { EC2Client, StartInstancesCommand, StopInstancesCommand } from '@aws-sdk/client-ec2';

import { verifyKey } from 'discord-interactions';

const ec2Client = new EC2Client({});
const EC2_INSTANCE_ID = process.env.EC2_INSTANCE_ID!;

const DISCORD_APP_PUBLIC_KEY = process.env.DISCORD_APP_PUBLIC_KEY!;
const DISCORD_CONTROL_CHANNEL_ID = process.env.DISCORD_CONTROL_CHANNEL_ID;
const DISCORD_GUILD_ID = process.env.DISCORD_GUILD_ID;
const DISCORD_OWNER_USER_ID = process.env.DISCORD_OWNER_USER_ID;
const DISCORD_PLAYER_ROLE_ID = process.env.DISCORD_PLAYER_ROLE_ID;

const INTERACTION_TYPE_PING = 1;
const INTERACTION_TYPE_APPLICATION_COMMAND = 2;

const RESPONSE_TYPE_PONG = 1;
const RESPONSE_TYPE_CHANNEL_MESSAGE = 4;

export const handler = async (event: any) => {
  const signature = event.headers['x-signature-ed25519'];
  const timestamp = event.headers['x-signature-timestamp'];

  // Handle optional base64 encoding of the body
  const rawBody = event.isBase64Encoded
    ? Buffer.from(event.body ?? '', 'base64').toString('utf-8')
    : event.body ?? '';

  const isValid = signature && timestamp && (await verifyKey(rawBody, signature, timestamp, DISCORD_APP_PUBLIC_KEY));

  if (!isValid) {
    return { statusCode: 401, body: 'Bad request signature' };
  }

  const interaction = JSON.parse(rawBody);
  console.log("interaction: ", interaction);

  // Discord PING request
  if (interaction.type === INTERACTION_TYPE_PING) {
    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: RESPONSE_TYPE_PONG }),
    };
  }

  // Status command
  else if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND && interaction.data?.name === 'status') {
    try {
      return success(await getServerStatus(interaction));
    } catch (err: any) {
      return error("Unable to get server status: " + err.message);
    }
  }

  // Start command
  else if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND && interaction.data?.name === 'start') {
    try {
      return success(await startServer(interaction));
    } catch (err: any) {
      return error("Unable to start server: " + err.message);
    }
  }

  // Stop command
  else if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND && interaction.data?.name === 'stop') {
    try {
      return success(await stopServer(interaction));
    } catch (err: any) {
      return error("Unable to stop server: " + err.message);
    }
  }

  // Restart command
  else if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND && interaction.data?.name === 'restart') {
    try {
      return success(await restartServer(interaction));
    } catch (err: any) {
      return error("Unable to restart server: " + err.message);
    }
  }

  // Wipe command
  else if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND && interaction.data?.name === 'wipe') {
    try {
      return success(await wipeWorldData(interaction));
    } catch (err: any) {
      return error("Unable to wipe world data: " + err.message);
    }
  }

  // Run command
  else if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND && interaction.data?.name === 'run') {
    try {
      return success(await runServerCommand(interaction));
    } catch (err: any) {
      return error("Unable to run server command: " + err.message);
    }
  }

  return badRequest('Unknown command');
};

async function getServerStatus(interaction: any) {
  if (interactionIsValid(interaction, false)) {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You have invoked the /status command but it is not yet implemented' },
    };
  } else {
      return {
        type: RESPONSE_TYPE_CHANNEL_MESSAGE,
        data: { content: 'You are not allowed to invoke this command' },
      };
    }
}

async function startServer(interaction: any) {
  if (interactionIsValid(interaction, false)) {
    await ec2Client.send(new StartInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'The server is starting, you can use /status to check if it is ready' },
    };
  } else {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' },
    };
  }
}

async function stopServer(interaction: any) {
  if (interactionIsValid(interaction, false)) {
    await ec2Client.send(new StopInstancesCommand({ InstanceIds: [EC2_INSTANCE_ID] }));
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'The server is stopping, you can use /status to check if it is stopped' },
    };
  } else {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' },
    };
  }
}

async function restartServer(interaction: any) {
  if (interactionIsValid(interaction, false)) {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You have invoked the /restart command but it is not yet implemented' },
    };
  } else {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' },
    };
  }
}

async function wipeWorldData(interaction: any) {
  if (interactionIsValid(interaction, true)) {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You have invoked the /wipe command but it is not yet implemented' },
    };
  } else {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' },
    };
  }
}

async function runServerCommand(interaction: any) {
  if (interactionIsValid(interaction, true)) {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You have invoked the /run command but it is not yet implemented' },
    };
  } else {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' },
    };
  }
}

function interactionIsValid(interaction: any, adminOnly: boolean): boolean {
  return interaction.guild_id === DISCORD_GUILD_ID
    && interaction.channel_id === DISCORD_CONTROL_CHANNEL_ID
    && interaction.member?.roles?.includes(DISCORD_PLAYER_ROLE_ID)
    && (!adminOnly || interaction.member?.user?.id === DISCORD_OWNER_USER_ID);
}

const success = (body: any) => ({
  statusCode: 200,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body)
});

const badRequest = (errorMessage: string) => ({
  statusCode: 400,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: errorMessage })
});

const error = (errorMessage: string) => ({
  statusCode: 500,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: errorMessage })
});
