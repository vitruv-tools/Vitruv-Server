# Vitruv Security Server
Vitruv Security Server is a Java-based server application built with Maven,
that ensures secure communication between clients and the internal Vitruv Server.
It follows a lightweight and direct architecture without relying on frameworks like Spring.

The system is designed to:
- Authenticate clients via the _FeLS_ identity provider (using the OIDC protocol).
- Protect communication through TLS encryption.
- _Security Server_ checks if request is authorized.
- Forward authorized requests to the internal _Vitruv Server_.

The application runs containerized in a Docker environment on a bwCloud instance.
A cron job automatically renews the required TLS certificate from Let’s Encrypt via Certbot.

---
## Guide: How to Deploy

This guide explains step by setp how to deploy the Vitruv Security Server in a Docker environment.
Important files are located in the [`deployment/`](./deployment) directory.

### 1. Build a new Docker Image (optional)
_Only required if you need to modify or update the Vitruv Security Server._
#### 1.1 Build the image
Run the following command at the root level of the `Vitruv-Server` project:
```sh
docker build -t vitruv-security-server:vX.Y . #e.g.: v1.7
```
#### 1.2 Push to Docker Hub
```sh
docker login 
docker tag vitruv-security-server:vX.Y bluesbird/vitruv-security-server:vX.Y
docker push bluesbird/vitruv-security-server:vX.Y 
```
_Note: `bluesbird` has to be replaced with your Docker Hub username._

---

### 2. Pull Docker Image
The following steps are executed in the target environment (e.g. [bwCloud](https://www.bw-cloud.org/)).

1. Open an SSH session to your target VM:
    ```sh
    ssh -i your_ssh_key ubuntu@193.196.39.34 #user@your-server-ip
    ```

2. Pull latest Docker image:

    ```sh
    docker pull bluesbird/vitruv-security-server:vX.Y
    ```

---

### 3. Get TLS Certificates from [Let's Encrypt](https://letsencrypt.org/)
Run the following command to generate TLS certificates via [Certbot](https://certbot.eff.org/):
```sh
certbot certonly --standalone -d <yourDomain>
``` 
Certificates are generated at `/etc/letsencrypt/live/<yourDomain>`.

_Note: `standalone` mode is used since no web server (such as Traefik or Nginx) is running.
For alternatives, see [Certbot documentation](https://eff-certbot.readthedocs.io/en/latest/using.html#getting-certificates-and-choosing-plugins)._

---

### 4. Copy and Configure important Files
**Note**: For this step you need OIDC client credentials for your domain.
In case you do did not receive them, contact Dr. Matthias Bonn ([matthias.bonn@kit.edu](https://www.scc.kit.edu/dienste/openid-connect.php#:~:text=matthias.bonn%40kit.edu)).

Copy the [`deployment/`](./deployment) directory to your target environment. This includes:
- [`.env`](./deployment/.env) &rarr; configure environment variables. This includes the OIDC client credentials.
- **Important: sensitive data** never commit this file!  
  Note: To recieve OIDC credentials request,
- [`docker-compose.yml`](./deployment/docker-compose.yml) &rarr; adjust the image name, tag and domain.
- [`renew_certificates.sh`](./deployment/renew_certificates.sh) &rarr; adjust paths and domain.

Follow the `TODO` comments in each file.
It is recommended to keep these files in the same directory.
Further information about these files is provided [here](#deployment-directory).

---

### 5. Start the Docker Container
1. Run the following command to start the container using [`docker-compose.yml`](./deployment/docker-compose.yml):
   ```sh
   docker-compose up -d
   ```
   _Note: `-d` flag for detached mode._


2. Monitor container (optional):
   ```sh
   docker logs -f vitruv-security-server
   ```
   Alternatively, the `server.log` file can be monitored inside the container:
   ```sh
   docker exec -it vitruv-security-server /bin/bash
   cat logs/server.log
   ```

---

### 6. Set Up Automatic Certificate Renewal
To automate the renewal process, add a cronjob that uses the [`renew_certificates.sh`](./deployment/renew_certificates.sh) script.
The script must be executable so that the cron daemon can run it without errors.

1. Make the script executable:
   ```sh
   chmod 775 renew_certificates.sh
   ```
   _Note: This sets read, write, and execute permissions._

2. Open the crontab:
   ```sh
   crontab -e
   ```
3. Add the following line:
   ```sh
   0 4 * * * /path/to/renew_certificates.sh >> /path/to/renew_certificates.log 2>&1
   ```  
   _Note: This cronjob runs daily at 4 AM and logs output to `/path/to/renew_certificates.log` (generated automatically)._

---
## Workflows
The following workflows illustrate the interaction as implemented between the client, the Security Server, the Vitruv Server, and FeLS.

### 1. Authentication Process via FeLS
This process occurs when no valid Access Token or Refresh Token is available.

1. The client sends an HTTPS request to the Security Server.
2. The Security Server detects that no valid Access Token or Refresh Token is present.
3. The client is redirected to the FeLS SSO authentication page for authentication.
4. After successful authentication, FeLS sends Access, ID and Refresh Tokens to the Security Server:
5. The Security Server validates the ID Token. If successful, all tokens are send to the client.
6. The client can now send authorized requests to the Vitruv Server (see [next](#2-request-handling-with-tokens) workflow).


### 2. Request Handling with Tokens
This process occurs when the client provides either a valid Access Token or a valid Refresh Token.

1. The client sends an HTTPS request with an Access Token to the Security Server.
2. The server checks the Access Token:
   - If Access Token is valid &rarr; Request is forwarded to the Vitruv Server.
   - If Access Token is invalid/missing:
      - If a valid Refresh Token is available &rarr; The server attempts to refresh the Access Token and issue a new Refresh Token via FeLS before forwarding the request to the Vitruv Server.
      - Else &rarr; The client is redirected to the FeLS SSO authentication page (&rarr; triggering the [Authentication Process](#1-authentication-process-via-fels)).
3. The Security Server returns the Vitruv Server's response to the client.
---

<!-- TOC --><a name="finding-refreshment-of-access-token"></a>
## Finding: Refreshment of Access Token
An unexpected behavior was observed when refreshing expired Access Tokens via the FeLS identity provider.
Instead of receiving a new JWT Access Token, the newly issued Access Token is an opaque token, similar to the Refresh Token.
This opaque token is immediately rejected as invalid when used, triggering a loop in the token refresh process.
While this does not affect the behaviour from an end user perspective, this is still a resource-wasting behaviour and a potential security risk.
The FeLS administrator confirmed this behavior is unintended and will investigate the issue.

**Update (27.02.2025):** After asking again, Mr. Michael Simon (SCC) said that changes were made and the issue should be re-tested.
A quick test revealed a change: no Access Token is returned at all, but the new Refresh Token remains functional and can still be used.  
This needs further investigation.

---

## Further Useful Links
- **Live Server:** [www.vitruv-server.org](https://www.vitruv-server.org) (Hosted on [bwCloud](https://www.bw-cloud.org/))
- **Docker Images:** [Docker Hub - Vitruv Server](https://hub.docker.com/r/bluesbird/vitruvserver/tags)
- **OIDC Client Configuration:** [FeLS Project](https://fels.scc.kit.edu/project)
- **Original Development Repository:** [GitHub - Vitruv Server](https://github.com/bluesbird/VitruvServer)
- **Vitruv Repository:** [GitHub - Vitruv-Server](https://github.com/vitruv-tools/Vitruv-Server)
