FROM alpine:3.6

ARG SEMUXVER="1.0.0-rc.3"

ENV JAVA_HOME=/usr/lib/jvm/default-jvm/jre

RUN echo $SEMUXVER

RUN apk --no-cache add openjdk8-jre && \
    apk --no-cache add curl

RUN curl -LO https://github.com/semuxproject/semux/releases/download/v${SEMUXVER}/semux-${SEMUXVER}.tar.gz
RUN mkdir -p /semux && tar -xzf semux-${SEMUXVER}.tar.gz -C /semux --strip-components=1 && rm /semux-${SEMUXVER}.tar.gz

EXPOSE 5161

CMD ["/semux/semux.sh", "--cli"]
