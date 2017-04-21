#!/bin/sh
docker run -ti -p 127.0.0.1:9042:9042 -p 127.0.0.1:7000:7000 --rm cassandra
