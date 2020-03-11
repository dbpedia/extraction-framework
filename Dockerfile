FROM maven:3-jdk-8

WORKDIR /dbpedia

RUN bash -c 'mkdir -p /dbpedia/extraction-framework-files/{basedir,logdir}'

ADD . /dbpedia/extraction-framework

VOLUME /dbpedia/extraction-framework-files

RUN grep -rl "/home/marvin/workspace/active/basedir" ./* | xargs sed -i 's\/home/marvin/workspace/active/\/dbpedia/extraction-framework-files/\g'

RUN cd extraction-framework/ && mvn clean install
