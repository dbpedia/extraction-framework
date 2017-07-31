#!/bin/bash
# This script installs the RML-Mapper in this folder into the local mvn repos

cp dependencies/RML-Mapper.jar /home/{username}/.m2/repository/be/ugent/mmlab/rml/RML-Mapper/1.1/RML-Mapper-1.1.jar
echo "RML Mapper installed in local Maven repository."
