#!/bin/bash

# Script to keep the tools required for the repo up to date
echo 'Updating tools required for the repository...'

# Upgrade the Ubuntu managed packages
apt-get update
apt-get -y upgrade

# Update cURL
echo 'Updating cURL...'
apt-get -y install curl
echo 'cURL updated!'

# Update Git
echo 'Updating Git...'
apt-get -y install git
echo 'Git updated!'

# Update Libatomic
echo 'Updating Libatomic...'
apt-get -y install libatomic1
echo 'Libatomic updated!'

# Update NVM
echo 'Updating NVM...'
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/master/install.sh | bash
source $NVM_DIR/nvm.sh
echo 'NVM updated!'

# Update Node.js
echo 'Updating Node.js...'
nvm install node
nvm list | grep -v "$(nvm current)" | grep -vE '\->' | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | while read version; do nvm uninstall "$version"; done
echo 'Node.js updated!'

# Update NPM
echo 'Updating NPM...'
nvm install-latest-npm
echo 'NPM updated!'

# Update AWS CLI
echo 'Updating AWS CLI...'
aws update
echo 'AWS CLI updated!'

# Update AWS CDK
echo 'Updating AWS CDK...'
npm install -g aws-cdk
echo 'AWS CDK updated!'
