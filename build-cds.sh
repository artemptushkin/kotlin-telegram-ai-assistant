#!/bin/sh

IMAGE_TAG=$(date +%s)
IMAGE_TARGET_TAG=latest

echo "Building the prebuild image, the tag is: $IMAGE_TAG"
./gradlew jibDockerBuild -Djib.to.image=ai-telegram-assistants-prebuild -Djib.to.tags=$IMAGE_TAG

mkdir -p ${PWD}/bot/build/libs/cds

echo "Running the prebuild image to prepare the CDS archive"
docker run -w /app -ti --entrypoint=/opt/java/openjdk/bin/java \
  -v ${PWD}/bot/build/libs/cds:/cds ai-telegram-assistants-prebuild:$IMAGE_TAG -XX:ArchiveClassesAtExit=/cds/application.jsa \
  -Dspring.context.exit=onRefresh \
  -cp "@jib-classpath-file" io.github.artemptushkin.ai.assistants.AiTelegramAssistantsApplicationKt || true

echo "Building the final image, the tag is: $IMAGE_TAG"
./gradlew jib \
  -Djib.to.image=europe-west4-docker.pkg.dev/peak-empire-400413/ai-telegram-assistants/ai-telegram-assistants \
  -Djib.container.jvmFlags="-Dspring.aot.enabled=true,-Xshare:on,-XX:SharedArchiveFile=/cds/application.jsa" \
  -Djib.to.tags=$IMAGE_TAG,$IMAGE_TARGET_TAG

echo "Image has been built, the tag is: $IMAGE_TAG"
