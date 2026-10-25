FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install --omit=dev

COPY app.js .

RUN mkdir -p /app/logs

EXPOSE 3000

CMD ["npm", "start"]
