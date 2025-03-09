# Vitruv Security Server
Vitruv Security Server is a Java-based server application that handles HTTPS requests from clients 
(members of the DFN-AAI federation), validates them, and forwards them to an internal Vitruv server. 
The implementation avoids frameworks like Spring, opting for a lightweight and direct architecture instead.

The system consists of two cooperating servers:
- HTTPS server (port 8433) that ensures secure communication.
- Vitruv server (port 8080) that processes requests based on the integrated Vitruv project.

The application runs containerized in a Docker environment on a bwCloud instance.
A cron job automatically renews the required TLS certificate from Let’s Encrypt via Certbot.

---

## Useful Links
- **Live Server:** [www.vitruv-server.org](https://www.vitruv-server.org) (Hosted on [bwCloud](https://www.bw-cloud.org/))
- **Docker Images:** [Docker Hub - Vitruv Server](https://hub.docker.com/r/bluesbird/vitruvserver/tags)
- **OIDC Client Configuration:** [FeLS Project](https://fels.scc.kit.edu/project)
- **Original Development Repository:** [GitHub - Vitruv Server](https://github.com/bluesbird/VitruvServer)

---

## **How to Deploy**

This guide explains how to deploy the **Vitruv Security Server**.
Important files are located in the [`deployment`](./deployment) folder.
Note that these files are linked.

### **1. Build a new Docker Image (optional)**
#### **1.1 Build the image**
Run the following command at the root level of the project:
```sh
docker build -t vitruvserver:vX.Y .
```
#### **1.2 Push to Docker Hub**
```sh
docker tag vitruvserver:vX.Y bluesbird/vitruvserver:vX.Y
docker push bluesbird/vitruvserver:vX.Y
```
_Note: `bluesbird` is the Docker Hub username._  
_Note: `vX.Y` is a placeholder for the version number (e.g.: `v1.7`)._

---

### **2. Pull Docker Image on the deployment platform (e.g. [bwCloud](https://www.bw-cloud.org/))**
```sh
docker pull bluesbird/vitruvserver:vX.Y
```

---

### **3. Get TLS Certificates from [Let's Encrypt](https://letsencrypt.org/) via [Certbot](https://certbot.eff.org/)**
```sh
certbot certonly --standalone -d vitruv-server.org
```
_Note: `standalone` mode is used since no web server is running._  
_Note: This generates the certificate `fullchain.pem` and `privkey.pem` at `/etc/letsencrypt/live/vitruv-server.org`._

---

### **4. Configure Environment Variables**
Edit [`deployment/.env`](./deployment/.env) and set important environment variables, especially the OIDC client credentials.

---

### **5. Start the Docker Container**
1. Run the following command to start the container using [`docker-compose.yml`](./deployment/docker-compose.yml):
   ```sh
   docker-compose up -d
   ```
   _Note: Opens port 443 (HTTPS)_  
   _Note: `-d` flag for detached mode_  


2. Monitor container (optional):  
   ```sh
   docker logs -f vitruvserver
   ```
   Alternatively, the `server.log` file can be monitored inside the container:
   ```sh
   docker exec -it vitruvserer /bin/bash
   cat logs/server.log
   ```

---

### **6. Set Up Automatic Certificate Renewal**
#### **6.1 Create Renewal Script**
Create Bash script [`renew_certificates.sh`](./deployment/renew_certificates.sh) to automatically renew the TLS certificate.

#### **6.2 Add Cronjob**
1. Open the crontab:
   ```sh
   crontab -e
   ```
2. Add the following line:
   ```sh
   0 4 * * * /path/to/renew_certificates.sh >> /path/to/renew_certificates.log 2>&1
   ```  
   _Note: Logs will be stored in `renew_certificates.log`._  
   _Note: This cronjob runs the script every day at 4 AM._  
