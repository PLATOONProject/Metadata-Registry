
# Metadata Registry

This is an implementation of an International Data Spaces (IDS) Metadata Broker extended to handle metadata of a Data App, which is a registry for IDS Connector self-description documents and Metadata of the Data App. It is currently under development and intends to act as a central metadata registry for members of Platoon project. 

The Open-source Data Broker has been funded by the PLATOON H2020 project funded by the EU commission..

## 1. Purpose

The goal of this implementation is to show how the concepts introduced in the [Handshake Document](https://industrialdataspace.jiveon.com/docs/DOC-1817#jive_content_id_Standard_Protocols_HTTPS_MQTT__TLS) (currently restricted to IDSA members) can be turned into an actual application. It, therefore, demonstrates the usage of the [IDS Information Model]() for core communication tasks. More specifically, it shows:

* Implementation of the messaging interfaces for IDS infrastructure-level communication,
* Information flow of typical interactions with the Metadata Registry.

Security is currently supported in terms of TLS via a reverse proxy.

## 2. Repository Structure

[broker-core](./broker-core): The main Maven Artifact to start with.

[open-broker-common](./open-broker-common): Shared code which [broker-core](./broker-core) requires.

[open-index-common](./open-index-common): Shared functionalities not only for [open-broker-common](./open-broker-common) but also for further IDS index services (for instance ParIS).

[docker](./docker): Docker ([installation guide](https://docs.docker.com/engine/install/)) and DockerCompose ([installation guide](https://docs.docker.com/compose/install/)) files to deploy the IDS Metadata Broker.

## 3. Prerequisites

In this section, we will provide some guidance as to recommendations for the number of resources that should be available to smoothly operate the IDS Metadata Broker. The actual number of resources required heavily depends on the load. In case of very little traffic, fewer resources than listed below might be required.


### 3.1 Hardware

2GB of disk space is required for operating the IDS Metadata Broker, though we recommend providing at least 20GB of free disk storage to avoid running out of disk space with increasing number of registered items. To provide enough processing power for all Docker containers, we recommend using a 64bit quad core processor or better.

### 3.2 Software

- **OS**: We recommend using a Linux based operating system. However, any operating system with a Docker installation can be used (tested on Ubuntu 20.04 and Windows 10). More strict hardware requirements than listed above might apply if a non-Linux operating system is used.
- **Docker**: version 20.10.7 or later
- **Docker Compose**: version 1.29.1 or later
- **OpenSSL**: Version 1.1.1k or later. A valid X.509 certificate, signed by a trusted certification authority, is strongly recommended to avoid warnings about insecure HTTPS connections. Docker must be installed on the target machine.
- **Java**: Java 11 or later should be installed in your local environment to build the docker image.
- **Maven**: Maven 3.6.3 or later should be installed in your local environment to build the docker image (execute `mvn -version` to check the successful installation).

## 4  Installation Guide
This part aims to aid IT administrators or developers in the installation of the IDS Metadata Broker. Metadata Broker is still actively maintained by Fraunhofer IAIS. If any problem arises while following the installation guide, please get in touch with the email provided at the end of this file.

### 4.1 Prepare The SSL Certificate
For the SSL certificate, you need to have these two files:
-  **server.crt:** an x509 certificate, either self-signed or from an official CA
- **server.key:** the private key for the certificate.

The certificate needs to be of *.crt* format and must have the name *server.crt* and the file for private key should have the name *server.key*. In case your certificate is of *.pem* format, it can be converted with the following commands, which require OpenSSL to be installed:

			openssl x509 -in mycert.pem -out server.crt
			openssl rsa -in mykey.pem -out server.key
			mkdir cert
			mv server.crt cert/
			mv server.key cert/

### 4.2 Running The Broker
To run the broker you can either make use of docker images provided by us as shown in **Section 4.2.1** 

#### 4.2.1 Running The Broker With Provided Image
If you want to run the broker with the provided image please follow the following steps:

**Step 1: Clone the repository**

	git clone https://github.com/PLATOONProject/Metadata-Registry.git

**Step 2: Configure the docker-compose file**

Once the repository is cloned, the docker-compose file will be found in this path:

	`./docker/composefiles/broker-localhost/docker-compose.yml`




The most crucial part of adapting the configuration is to provide the correct location of the X.509 certificate created above in the broker-reverseproxy service.

**For Linux users:**  if the location of the certificate is *“/home/ids/cert”*, the corresponding configuration in the yml file is:

	services: broker-reverseproxy:
		image: registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/broker/reverseproxy
		volumes:
		- /home/ids/cert:/etc/cert/
		[…]



**For Windows users:**  if the location of the certificate is *“c:/etc/ids/cert”*, the corresponding configuration in the yml file is:  


	services: broker-reverseproxy:
		image: registry.gitlab.cc-asp.fraunhofer.de:4567/eis-ids/broker/reverseproxy
		volumes:
		- c:/etc/ids/cert:/etc/cert/
		[…]

**Step 3: Download the docker images**

All the IDS Metadata Broker Docker images are hosted at the GitLab of Fraunhofer IAIS. No credentials needed to download the images. The following command is for pulling all docker images:

		docker-compose pull

Note that this command should be executed in the same path of docker-compose.yml file.


**Step 4: Start up the IDS Metadata Broker**

To start up the IDS Metadata Broker, run the following command inside the directory of the docker-compose.yml file:

		docker-compose up –d



This process can take several minutes to complete. You can test whether the IDS Metadata Broker has successfully started by opening [https://localhost](https://localhost/). The result should be a JSON document, providing some general metadata about the IDS Metadata Broker.

Furthermore, the docker-compose logs command can be used to access the logs for a docker-compose.yml file, see [here](https://docs.docker.com/compose/reference/logs/).


**Step 5: Stop the IDS Metadata Broker**

To stop the Broker, run the following in the terminal in the same path as the docker-compose.yml file:

		docker-compose down

**Step 6: Update the IDS Metadata Broker**

To update an existing installation of the IDS Metadata Broker, first repeat the steps explained in **Step 3**. Containers can be either hot updated or restarted to apply the changes. To hot update a container, run the following command:

		docker-compose up -d --no-deps --build <container name>

Alternatively, one can restart the entire service by running:

		docker-compose down
		docker-compose up –d

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management
* [Spring Boot](https://projects.spring.io/spring-boot/) - Application Framework
* [Apache Jena](https://jena.apache.org/documentation/) - Parsing and serializing RDF and Fuseki as triple store for meta data

## Contact

*  Fraunhofer IAIS: [Najmehsadat Mousavinezhad](mailto:najmehsadat.mousavinezhad@iais.fraunhofer.de), [Tasneem Tazeen Rashid](mailto:tasneem.tazeen.rashid@iais.fraunhofer.de), [Tejas Morbagal Harish](mailto:tejas.morbagal.harish@iais.fraunhofer.de)
* or create an issue
