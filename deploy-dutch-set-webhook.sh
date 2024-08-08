#!/bin/zsh

cd webhook-provisioning

gcloud functions deploy setwebhook-dutch-ai-assistant-bot --entry-point io.github.artemptushkin.ai.assistants.webhook.SetWebhookPubsubFunction \
  --runtime java17 --trigger-topic webhook-update-events --allow-unauthenticated --memory 256MB \
  --env-vars-file=../config/dutch-ai-assistant-bot.yaml