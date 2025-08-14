FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy application
COPY target/uns-elk-0.1.0.jar app.jar

ENV JAVA_OPTS="-javaagent:/app/elastic-apm-agent.jar -Delastic.apm.server_urls=http://fleet-server:8200 -Delastic.apm.service_name=uns-elk -Delastic.apm.secret_token=supersecrettoken -Delastic.apm.environment=development"

# Download APM agent
ADD https://search.maven.org/remotecontent?filepath=co/elastic/apm/elastic-apm-agent/1.48.0/elastic-apm-agent-1.48.0.jar /app/elastic-apm-agent.jar

# Install CA certificates utilities
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Add entrypoint that updates trust store with ELK CA before running app
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8000

ENTRYPOINT ["/docker-entrypoint.sh"]