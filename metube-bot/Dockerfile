FROM node:18-alpine

WORKDIR /app

# Install dependencies first for caching
COPY package*.json ./
RUN npm install --production

# Copy source code
COPY . .

# Environment variables (defaults)
ENV NODE_ENV=production
ENV METUBE_URL=http://metube:8081/

# Start the bot
CMD ["npm", "start"]
