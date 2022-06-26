#!/bin/bash

dos2unix broker-core/*

# GENERIC IMAGES

mvn -f ../ clean package
rm broker-core/broker-core-*.jar
cp ../broker-core/target/broker-core-*.jar broker-core/
docker build broker-core/ -t registry.gitlab.cc-asp.fraunhofer.de/metadata-registry/core:1.0.2

#cleanup
rm -rf ../index-common/target
rm -rf ../broker-common/target


# fuseki
#docker build fuseki/ -t registry.gitlab.cc-asp.fraunhofer.de/eis-ids/broker-open/fuseki

# reverseproxy
#docker build reverseproxy/ -t registry.gitlab.cc-asp.fraunhofer.de/eis-ids/broker-open/reverseproxy
