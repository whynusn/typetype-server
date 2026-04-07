#! /usr/bin/fish
set -x JWT_SECRET_KEY $(openssl rand -base64 32); echo $(date) $JWT_SECRET_KEY >> env.log; mvn spring-boot:run;
