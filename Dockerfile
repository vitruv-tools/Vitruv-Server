FROM ubuntu:latest
LABEL authors="Lorenz"

ENTRYPOINT ["top", "-b"]