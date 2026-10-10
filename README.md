# Minecraft PaperMC-Server

A simple Minecraft server based on PaperMC, hosted on AWS to play with a few friends.

It also comes with a pugin called Cathy's Quest, that brings Everest and Obélix, two Great Pyrenees dogs into the game, as well as Grogu from the Mandalorian, into a little quest for the players.

## Running locally

This repo contains a dev container based in Itzg's Minecraft server image, a feature rich image to run any kind of Minecraft server. To run it locally, simply run the /start command in a terminal inside the dev container. You can then connect from your Minecraft client at `localhost:25565`. To configure the server, update the .env file or .env.local for sensitive values that should not be committed to source control. You have to rebuild the container for the changes to take effect. The scripts folder includes several utility scripts to interact with the local server.

### Plugins (local)

To test a plugin locally, build it with gradle, then run the utility deploy.sh scripts (from the scripts folder at the root of the repository). This will place the plugin jar file in the proper location for the Minecraft server to pick it up on next startup.

## Deploying the server

This repo includes a complete AWS CDK stack to deploy and manage the server on AWS infrastructure. Refer to the README.md file under the infra directory for more details. The `.env` file to configure the server can be found in the server directory.

### Managing the server

The CDK stack contains a Lambda function that can receive commands to interact with the server. It is configured to receive these commands from a Discord bot. Instructions can be found online on how to install your own Discord bot and register commands with it. The following commands are implemented in the Lambda

- /status (displays the current state of the server)
- /start (starts EC2 and Minecraft)
- /stop (stops Minecraft and EC2)
- /restart (restarts the Minecraft server while keeping EC2 running)
- /wipe (wipes the world data)
- /run command (submits the given command to Minecraft to execute)
- /restore backup (restores the given world data backup)

### Plugins (server)

The repository comes with a CI/CD setup using GitHub Actions to run a workflow any time changes are committed under the plugins directory on the main branch. This workflow builds all plugins, collects the jar files and pushes them to an S3 bucket (defined in the CDK stack). The CDK stack also configures the EC2 instance to retrieve the latest jar files from S3 any time Minecraft is started.

## Upgrade paths

This section suggests upgrade paths for various components

### New Minecraft version

When a new Minecraft / PaperMC version is released and you want to upgrade, all you have to do is update the `VERSION` variable in `server/.env` to the desired version. On next start of the Minecraft server, the game will update to the next version. This project comes with the Chunky plugin installed, which can be used to pre-generate the world data for improved playing performance. This can lead to a situation where a wide radius around spawn is pre-generated, so new biomes or features introduced in the latest version cannot be found anywhere near spawn. To remediate this, Chunky has a feature to trim the world data, keeping a determined radius around spawn intact. If you have not built anything past a certain radius, you can shrink the world down and re-generate it with Chunky to get the new biomes and structures from the latest version of Minecraft.

### New Itzg/minecraft image version

The docker compose file that runs on the server is configured to pull the latest image once a day, so it should naturally keep up to date. This behavior can be disabled in the `server/compose.yaml` file.
