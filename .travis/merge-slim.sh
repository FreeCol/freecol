#!/bin/bash
set -xe

# Create a deploy key from the encrypted file
openssl aes-256-cbc -K $encrypted_57654742c400_key -iv $encrypted_57654742c400_iv \
  -in ./.travis/freecol-deploy-key.enc -out freecol-deploy-key -d
chmod 600 freecol-deploy-key
eval $(ssh-agent -s)
ssh-add freecol-deploy-key

git remote add github git@github.com:FreeCol/freecol.git
git remote update
git merge origin/master --no-edit
