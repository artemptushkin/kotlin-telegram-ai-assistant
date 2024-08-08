#!/bin/zsh

gcloud run deploy dutch-ai-assistant-bot --env-vars-file=config/dutch-ai-assistant-bot.yaml \
   --image=europe-west4-docker.pkg.dev/peak-empire-400413/ai-telegram-assistants/ai-telegram-assistants:latest \
   --region=europe-west1 --min-instances=0