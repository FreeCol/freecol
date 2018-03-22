#!/bin/bash
set -e

# Create a deploy key from the encrypted file
openssl aes-256-cbc -K $encrypted_57654742c400_key -iv $encrypted_57654742c400_iv \
    -in ./.travis/freecol-deploy-key.enc -out ./.travis/freecol-deploy-key -d
chmod 600 ./.travis/github_deploy_key
eval $(ssh-agent -s)
ssh-add ./.travis/github_deploy_key


git remote add upstream git://git.code.sf.net/p/freecol/git

# git merge upstream/master --no-edit
# git push origin master
