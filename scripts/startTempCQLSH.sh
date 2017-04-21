#!/bin/bash
docker run -ti --rm --net=host cassandra sh -c 'exec cqlsh 127.0.0.1'
