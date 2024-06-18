#!/bin/zsh

gcloud run deploy ai-telegram-assistants --env-vars-file=config/dutch-learner.yaml \
   --image=europe-west4-docker.pkg.dev/peak-empire-400413/ai-telegram-assistants/ai-telegram-assistants:latest \
   --region=europe-west1 --min-instances=0