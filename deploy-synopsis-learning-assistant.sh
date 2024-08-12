#!/bin/zsh

gcloud run deploy synopsis-ai-assistant-bot --env-vars-file=config/synopsis-ai-assistant-bot.yaml \
   --image=europe-west4-docker.pkg.dev/peak-empire-400413/ai-telegram-assistants/ai-telegram-assistants:latest \
   --region=europe-west1 --min-instances=0