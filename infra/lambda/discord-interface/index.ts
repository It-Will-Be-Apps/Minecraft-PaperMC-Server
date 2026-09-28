import { InvokeCommand, LambdaClient } from '@aws-sdk/client-lambda';

import { verifyKey } from 'discord-interactions';

const DISCORD_APP_PUBLIC_KEY = process.env.DISCORD_APP_PUBLIC_KEY;
const DISCORD_CONTROL_CHANNEL_ID = process.env.DISCORD_CONTROL_CHANNEL_ID;
const DISCORD_GUILD_ID = process.env.DISCORD_GUILD_ID;
const DISCORD_OWNER_USER_ID = process.env.DISCORD_OWNER_USER_ID;
const DISCORD_PLAYER_ROLE_ID = process.env.DISCORD_PLAYER_ROLE_ID;

const INTERACTION_TYPE_PING = 1;
const INTERACTION_TYPE_APPLICATION_COMMAND = 2;

const RESPONSE_TYPE_PONG = 1;
const RESPONSE_TYPE_CHANNEL_MESSAGE = 4;
const RESPONSE_TYPE_DEFERRED_CHANNEL_MESSAGE = 5;

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

  // Unknown interaction type
  if (interaction.type !== INTERACTION_TYPE_APPLICATION_COMMAND) {
    return badRequest('Unknown interaction type');
  }

  const command = interaction.data?.name;

  if (!commandIsValid(command)) {
    return badRequest('Unknown command');
  }

  if (!interactionIsValid(interaction, command)) {
    return {
      type: RESPONSE_TYPE_CHANNEL_MESSAGE,
      data: { content: 'You are not allowed to invoke this command' },
    };
  }

  await invokeWorker(interaction);

  return success({ type: RESPONSE_TYPE_DEFERRED_CHANNEL_MESSAGE });
}

function commandIsValid(command: string) {
  return USER_COMMANDS.includes(command) || ADMIN_COMMANDS.includes(command);
}

function interactionIsValid(interaction: any, command: string): boolean {
  const adminOnly = ADMIN_COMMANDS.includes(command);

  return interaction.guild_id === DISCORD_GUILD_ID
    && interaction.channel_id === DISCORD_CONTROL_CHANNEL_ID
    && interaction.member?.roles?.includes(DISCORD_PLAYER_ROLE_ID)
    && (!adminOnly || interaction.member?.user?.id === DISCORD_OWNER_USER_ID);
}

async function invokeWorker(interaction: any) {
  await lambdaClient.send(new InvokeCommand({
    FunctionName: SERVER_MANAGEMENT_FUNCTION_NAME,
    InvocationType: 'Event',
    Payload: Buffer.from(JSON.stringify({ interaction }))
  }));
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
