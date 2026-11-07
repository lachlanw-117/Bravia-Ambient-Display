FROM node:22-alpine
WORKDIR /app
COPY server.mjs /app/server.mjs
ENV DATA_DIR=/data/cache PHOTO_DIR=/share/bravia-ambient BIND_HOST=0.0.0.0 PORT=8099
USER root
CMD ["node", "/app/server.mjs"]
