#!/bin/sh

set -eux

# create CA
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days 9999 -subj "/C=US/ST=CA/L=Palo Alto/O=Airlift/OU=RootCA" -key ca.key -out ca.crt

# create server key
openssl genrsa -out server.key 4096
openssl req -new -key server.key -subj "/C=US/ST=CA/L=Palo Alto/O=Airlift/OU=Server/CN=localhost" -out server.csr
openssl x509 -req -days 9999 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt

# create server keystore
openssl pkcs12 -name server -inkey server.key -in server.crt -export -passout pass:airlift -out server.keystore
keytool -import -noprompt -alias ca -file ca.crt -storetype pkcs12 -storepass airlift -keystore server.keystore

# create client key
openssl genrsa -out client.key 4096
openssl req -new -key client.key -subj "/C=US/ST=CA/L=Palo Alto/O=Airlift/OU=Client/CN=testing" -out client.csr
openssl x509 -req -days 9999 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 02 -out client.crt

# create client keystore
openssl pkcs12 -name client -inkey client.key -in client.crt -export -passout pass:airlift -out client.keystore

# create client truststore
keytool -import -noprompt -alias ca -file ca.crt -storetype pkcs12 -storepass airlift -keystore client.truststore


#A kesytore for testing the KeyStoreScanner's ability to transparently update the ssl config without needing a restart

# create replacement server cert
openssl req -new -key server.key -subj "/C=US/ST=CA/L=Palo Alto/O=Replacement/OU=Server/CN=localhost" -out replacementServer.csr
openssl x509 -req -days 9999 -in replacementServer.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out replacementServer.crt

# create replacement server keystore
openssl pkcs12 -name server -inkey server.key -in replacementServer.crt -export -passout pass:airlift -out replacementServer.keystore
keytool -import -noprompt -alias ca -file ca.crt -storetype pkcs12 -storepass airlift -keystore replacementServer.keystore

# create client cert
openssl req -new -key client.key -subj "/C=US/ST=CA/L=Palo Alto/O=Replacement/OU=Client/CN=testing" -out replacementClient.csr
openssl x509 -req -days 9999 -in replacementClient.csr -CA ca.crt -CAkey ca.key -set_serial 02 -out replacementClient.crt

# create replacement client keystore
openssl pkcs12 -name client -inkey client.key -in replacementClient.crt -export -passout pass:airlift -out replacementClient.keystore

# create replacement client truststore
keytool -import -noprompt -alias ca -file ca.crt -storetype pkcs12 -storepass airlift -keystore replacementClient.truststore