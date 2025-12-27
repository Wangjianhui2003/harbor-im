# syntax=docker/dockerfile:1
FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY ./dist /harbor-frontend

# run
EXPOSE 80 443