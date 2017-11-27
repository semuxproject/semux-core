#!/usr/bin/env bash

HOST=127.0.0.1
PORT=5161

wait_for()
{
    echo "waiting for $HOST:$PORT"
    start_ts=$(date +%s)
    while :
    do
        nc -v -z $HOST $PORT
        result=$?
        if [[ $result -eq 0 ]]; then
            end_ts=$(date +%s)
            echo "$HOST:$PORT is available after $((end_ts - start_ts)) seconds"
            break
        fi
        sleep 1
    done
    return $result
}

wait_for
RESULT=$?
exit $RESULT