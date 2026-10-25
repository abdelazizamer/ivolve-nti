FROM node:20-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY db.js server.js ./
COPY frontend ./frontend

EXPOSE 3000

CMD ["npm", "start"]
