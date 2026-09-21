FROM itzg/minecraft-server:latest

# Update and upgrade packages
RUN apt-get update
RUN apt-get -y upgrade

# Install cURL
RUN apt-get -y install curl

# Install Git
RUN apt-get -y install git

# Install Git auto-completion
RUN curl https://raw.githubusercontent.com/git/git/master/contrib/completion/git-completion.bash -o ~/.git-completion.bash
RUN echo "source ~/.git-completion.bash" >> ~/.bashrc

# Update bash prompt with colors and Git branch
RUN curl https://raw.githubusercontent.com/git/git/master/contrib/completion/git-prompt.sh -o ~/.git-prompt.sh
RUN echo "source ~/.git-prompt.sh" >> ~/.bashrc
RUN echo "PS1='\[\\\033[01;32m\]\u \[\\\033[35m\]\s \[\\\033[01;33m\]\w \[\\\033[01;36m\]\$(__git_ps1 \"(%s)\") \[\\\033[00m\]\$ '" >> ~/.bashrc

# Install Libatomic1, required for recent Node versions
RUN apt-get -y install libatomic1

# Install NVM
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/master/install.sh | bash

# Install Node.js
RUN . ~/.bashrc && nvm install node

# Install AWS CLI
RUN curl -fsSL https://awscli.amazonaws.com/v2/install.sh | bash -s -- --system

# Install AWS CDK
RUN . ~/.bashrc && npm install -g aws-cdk
