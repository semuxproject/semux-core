FROM openjdk:8-jre-alpine

WORKDIR /

RUN apk --no-cache add curl

RUN LINK=`curl -s https://api.github.com/repos/semuxproject/semux/releases | grep -o "https://.*semux-linux.*.tar.gz"` && \
    TARBALL=`echo ${LINK} | grep -o "semux-linux.*.tar.gz"` && \
    curl -LO ${LINK} && \
    mkdir -p /semux && \
    tar -xzf ${TARBALL} -C /semux --strip-components=1 && \
    rm ${TARBALL}

EXPOSE 5161

ENTRYPOINT ["/semux/semux-cli.sh"]
