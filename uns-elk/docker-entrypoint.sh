#!/bin/sh
set -e

CA_SRC="/usr/share/webapp/certs/ca/ca.crt"
CA_DST="/usr/local/share/ca-certificates/elk-ca.crt"
CACERTS_PATH="/opt/java/openjdk/lib/security/cacerts"
CACERTS_ALIAS="elastic-ca"
CACERTS_PASS="changeit"

# Add ELK CA to system trust if present
if [ -f "$CA_SRC" ]; then
  echo "Copying ELK CA to system trust store..."
  cp "$CA_SRC" "$CA_DST"
  update-ca-certificates
fi

# Import ELK CA into JVM trust store if not already present
if [ -f "$CA_SRC" ]; then
  echo "Checking if ELK CA is already imported into JVM trust store..."
  if ! keytool -list -keystore "$CACERTS_PATH" -storepass "$CACERTS_PASS" -alias "$CACERTS_ALIAS" >/dev/null 2>&1; then
    echo "Importing ELK CA into JVM trust store..."
    keytool -importcert -trustcacerts -noprompt \
      -storepass "$CACERTS_PASS" \
      -alias "$CACERTS_ALIAS" \
      -file "$CA_SRC" \
      -keystore "$CACERTS_PATH"
  else
    echo "ELK CA already present in JVM trust store."
  fi
fi

exec java $JAVA_OPTS -jar /app/app.jar