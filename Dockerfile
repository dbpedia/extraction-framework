FROM maven:3-jdk-8

WORKDIR /dbpedia

RUN git clone https://github.com/dbpedia/extraction-framework.git

RUN mkdir -p /dbpedia/extraction-framework-files/basedir && mkdir /dbpedia/extraction-framework-files/logdir 

VOLUME /dbpedia/extraction-framework-files

RUN grep -rl "/home/marvin/workspace/active/basedir" ./* | xargs sed -i 's\/home/marvin/workspace/active/\/dbpedia/extraction-framework-files/\g'

RUN cd extraction-framework/ && mvn clean install
