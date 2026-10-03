import { InvokeCommand, LambdaClient } from '@aws-sdk/client-lambda';

import { verifyKey } from 'discord-interactions';

const DISCORD_APP_PUBLIC_KEY = process.env.DISCORD_APP_PUBLIC_KEY;
const DISCORD_CONTROL_CHANNEL_ID = process.env.DISCORD_CONTROL_CHANNEL_ID;
const DISCORD_GUILD_ID = process.env.DISCORD_GUILD_ID;
const DISCORD_OWNER_USER_ID = process.env.DISCORD_OWNER_USER_ID;
const DISCORD_PLAYER_ROLE_ID = process.env.DISCORD_PLAYER_ROLE_ID;

const INTERACTION_TYPE_PING = 1;
const INTERACTION_TYPE_APPLICATION_COMMAND = 2;
const INTERACTION_TYPE_MESSAGE_COMPONENT = 3;

const RESPONSE_TYPE_PONG = 1;
const RESPONSE_TYPE_CHANNEL_MESSAGE = 4;
const RESPONSE_TYPE_DEFERRED_CHANNEL_MESSAGE = 5;
const RESPONSE_TYPE_UPDATE_MESSAGE = 7;

const MINECRAFT_STOP_CONFIRM = 'minecraft_stop_confirm';
const MINECRAFT_STOP_CANCEL = 'minecraft_stop_cancel';
const MINECRAFT_RESTART_CONFIRM = 'minecraft_restart_confirm';
const MINECRAFT_RESTART_CANCEL = 'minecraft_restart_cancel';
const MINECRAFT_WIPE_CONFIRM = 'minecraft_wipe_confirm';
const MINECRAFT_WIPE_CANCEL = 'minecraft_wipe_cancel';
const MINECRAFT_RESTORE_CONFIRM = 'minecraft_restore_confirm';
const MINECRAFT_RESTORE_CANCEL = 'minecraft_restore_cancel';

const SERVER_MANAGEMENT_FUNCTION_NAME = process.env.SERVER_MANAGEMENT_FUNCTION_NAME!;

const USER_COMMANDS = ['status', 'start', 'stop', 'restart'];
const ADMIN_COMMANDS = ['run', 'restore', 'wipe'];

const lambdaClient = new LambdaClient({});

export const handler = async (event: any) => {
  const signature = event.headers['x-signature-ed25519'];
  const timestamp = event.headers['x-signature-timestamp'];

  const rawBody = event.isBase64Encoded
    ? Buffer.from(event.body ?? '', 'base64').toString('utf-8')
    : event.body ?? '';

  const isValid = signature && timestamp && (await verifyKey(rawBody, signature, timestamp, DISCORD_APP_PUBLIC_KEY));

  if (!isValid) {
    return unauthorized('Bad request signature');
  }

  const interaction = JSON.parse(rawBody);

  // Discord PING request
  if (interaction.type === INTERACTION_TYPE_PING) {
    return success({ type: RESPONSE_TYPE_PONG });
  }

  // Application command
  if (interaction.type === INTERACTION_TYPE_APPLICATION_COMMAND) {
    return await handleApplicationCommand(interaction);
  }

  // Message component
  if (interaction.type === INTERACTION_TYPE_MESSAGE_COMPONENT) {
    return await handleMessageComponent(interaction);
  }

  return badRequest('Unknown interaction type');
}

async function handleApplicationCommand(interaction: any) {
  const command = interaction.data?.name;

  if (!commandIsValid(command)) {
    return badRequest('Unknown command');
  }

  const isAdmin = interaction.member?.user?.id === DISCORD_OWNER_USER_ID

  if (!interactionIsValid(interaction, command, isAdmin)) {
    return success({
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' }
    });
  }

  await invokeWorker(interaction, isAdmin);

  return success({ type: RESPONSE_TYPE_DEFERRED_CHANNEL_MESSAGE });
}

function commandIsValid(command: string) {
  return USER_COMMANDS.includes(command) || ADMIN_COMMANDS.includes(command);
}

function interactionIsValid(interaction: any, command: string, isAdmin: boolean): boolean {
  const adminOnly = ADMIN_COMMANDS.includes(command);

  return interaction.guild_id === DISCORD_GUILD_ID
    && interaction.channel_id === DISCORD_CONTROL_CHANNEL_ID
    && interaction.member?.roles?.includes(DISCORD_PLAYER_ROLE_ID)
    && (!adminOnly || isAdmin);
}

async function invokeWorker(interaction: any, isAdmin: boolean) {
  await lambdaClient.send(new InvokeCommand({
    FunctionName: SERVER_MANAGEMENT_FUNCTION_NAME,
    InvocationType: 'Event',
    Payload: Buffer.from(JSON.stringify({ interaction, isAdmin }))
  }));
}

async function handleMessageComponent(interaction: any) {
  const customId = interaction.data?.custom_id;
  const isAdmin = interaction.member?.user?.id === DISCORD_OWNER_USER_ID

  if (interaction.guild_id !== DISCORD_GUILD_ID
      || interaction.channel_id !== DISCORD_CONTROL_CHANNEL_ID
      || !interaction.member?.roles?.includes(DISCORD_PLAYER_ROLE_ID)
      || !isAdmin) {
      
    return success({
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' }
    });
  }

  if (customId === MINECRAFT_STOP_CONFIRM) {
    await invokeWorker(interaction, isAdmin);

    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Stopping the Minecraft server...',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_STOP_CANCEL) {
    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Stop command cancelled',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_RESTART_CONFIRM) {
    await invokeWorker(interaction, isAdmin);

    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Restarting the Minecraft server...',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_RESTART_CANCEL) {
    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Restart command cancelled',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_WIPE_CONFIRM) {
    await invokeWorker(interaction, isAdmin);

    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Wiping the Minecraft world...',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_WIPE_CANCEL) {
    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Wipe command cancelled',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_RESTORE_CONFIRM) {
    await invokeWorker(interaction, isAdmin);

    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Restoring the Minecraft world...',
        components: []
      }
    });
  }

  if (customId === MINECRAFT_RESTORE_CANCEL) {
    return success({
      type: RESPONSE_TYPE_UPDATE_MESSAGE,
      data: {
        content: 'Restore command cancelled',
        components: []
      }
    });
  }

  return badRequest('Unknown component');
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

const unauthorized = (errorMessage: string) => ({
  statusCode: 401,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: errorMessage })
});
