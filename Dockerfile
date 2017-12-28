FROM openjdk:8-jre-alpine

ARG SEMUXVER="1.0.0-rc.5"

RUN echo $SEMUXVER

RUN apk --no-cache add curl
WORKDIR /
RUN TARBALL=semux-linux-${SEMUXVER}.tar.gz && \
    curl -LO https://github.com/semuxproject/semux/releases/download/v${SEMUXVER}/${TARBALL} && \
    mkdir -p /semux && \
    tar -xzf ${TARBALL} -C /semux --strip-components=1 && \
    rm /${TARBALL}

EXPOSE 5161

ENTRYPOINT ["/semux/semux-cli.sh"]
