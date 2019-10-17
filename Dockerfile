FROM ubuntu:14.04 AS build

RUN apt-get update -yq && \
    apt-get install -yq build-essential \
                        gcc \
                        g++ \
                        libevent-dev \
                        libmysqlclient-dev \
                        libsqlite3-dev \
                        libpq-dev \
                        libz-dev \
                        libssl-dev \
                        libpcre3-dev \
                        libgtest-dev \
                        bison \ 
                        autoconf \
                        automake \
                        clang \
                        expect \
                        libtool \
                        telnet \
                        curl

RUN ln -s /usr/bin/make /usr/bin/gmake

ARG FLUFFOS_ARCHIVE=https://github.com/fluffos/fluffos/archive/v2017.2018123101.tar.gz

RUN mkdir /fluffos && \
    echo ${FLUFFOS_ARCHIVE} && \
    curl -kL "$FLUFFOS_ARCHIVE" -o /fluffos/src.tgz && \
    tar -xzf /fluffos/src.tgz --strip 1 -C /fluffos

WORKDIR /fluffos/src

COPY local_options /fluffos/src

RUN ./build.FluffOS && \
    make && \
    make install && echo 0

FROM ubuntu:14.04

RUN apt-get update -yq && \
    apt-get install -yq --no-install-recommends libevent-dev \
                                                libmysqlclient-dev \
                                                libsqlite3-dev \
                                                libpq-dev \
                                                libz-dev \
                                                libssl-dev \
                                                libpcre3-dev \
                                                telnet && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /fluffos/bin/driver /usr/local/bin/
COPY --from=build /fluffos/bin/portbind /usr/local/bin/
COPY entrypoint.sh /usr/local/bin/entrypoint.sh

ARG USER_ID=501
ARG USER_NAME=mud

RUN addgroup --gid $USER_ID $USER_NAME \
    && useradd --shell /bin/bash -u $USER_ID -g $USER_NAME -o -c "" -m $USER_NAME

EXPOSE 5000

USER $USER_NAME

VOLUME [ "/mud/library", "/mud/config" ]
ENTRYPOINT [ "/usr/local/bin/entrypoint.sh" ]
