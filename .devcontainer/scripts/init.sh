#!/bin/bash

# One time script to initialize the tools required for the repo
echo 'Initializing tools required for the repository...'

# Configure Git
echo 'Configuring Git...'
git config --global core.editor "code --wait"
git config --global user.name $GIT_USER_NAME
git config --global user.email $GIT_USER_EMAIL
git config --global user.signingkey $HOME/.ssh/id_ed25519.pub
git config --global gpg.format ssh
git config --global commit.gpgsign true
echo 'Git configured!'
