FROM openjdk:8-jre

WORKDIR /

RUN apt-get update && apt-get install --yes curl jq

RUN LATEST=`curl -s https://api.github.com/repos/semuxproject/semux/releases/latest | jq '.assets[]  | select(.name | contains("linux"))'` && \
    LINK=`echo ${LATEST} | jq -r '.browser_download_url'` && \
    TARBALL=`echo ${LATEST} | jq -r '.name'` && \
    curl -Lo ${TARBALL} ${LINK} && \
    mkdir -p /semux && \
    tar -xzf ${TARBALL} -C /semux --strip-components=1 && \
    rm ${TARBALL}

RUN apt-get remove --yes curl jq

EXPOSE 5161

ENTRYPOINT ["/semux/semux-cli.sh"]
