# Pull base image
FROM sevi/sevi-base-java:latest

# Deploy project
ADD sevi-ontology-service.jar ${deploy_dir}/sevi-ontology-service.jar

# If you want to serve static content (e.g. /sevi-example-java-service/swagger/index.html)
# from spring boot application that is running inside docker container you shall run into
# e.g. following problem:
#
# WARN  o.e.jetty.servlet.ServletHandler - /sevi-example-java-service/swagger/index.html
# java.io.FileNotFoundException: class path resource [static/swagger/index.html] cannot be resolved in the file system for resolving its last-modified timestamp
#
# Reason for this behaviour is explained here: https://jira.spring.io/browse/SPR-12862
# Workaround is described here: https://spring.io/guides/gs/spring-boot-docker/
#
# Enable following command to solve problem:
RUN sh -c 'touch ${deploy_dir}/sevi-ontology-service.jar'

# Expose port
EXPOSE 9089
EXPOSE 19089

# Set default command on run
ENTRYPOINT ["/bootstrap.sh", "sevi-ontology-service.jar"]
