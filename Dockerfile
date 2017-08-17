# # Pull base image, this has to be 3.2.5
FROM maven:3.2.5-jdk-8

## clone repositories
RUN git clone https://github.com/dbpedia/extraction-framework ###

## change to repo dir
WORKDIR /extraction-framework

## switch to rml branch
RUN git checkout rml

## update submodules
RUN git submodule update --init --recursive

## install RML-Mapper
RUN mkdir -p /root/.m2/repository/be/ugent/mmlab/rml/RML-Mapper/1.1/
RUN cp dependencies/RML-Mapper.jar /root/.m2/repository/be/ugent/mmlab/rml/RML-Mapper/1.1/RML-Mapper-1.1.jar ####

## build the server
RUN mvn clean install -fn ##

#?
RUN chmod -R 700 ~

# Run the extraction!
WORKDIR ./server
CMD ../run server