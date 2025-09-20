# Ship-Offshore Proxy

This project implements a simple **proxy system** that reuses a single TCP connection between a ship (client-side) and an offshore server.  
The motivation is to minimize **satellite communication costs** by keeping only one persistent connection open between ship and offshore,  
while still serving multiple HTTP requests from clients.

---

## 🚀 Features
- **ShipProxy**
  - Runs on the ship.
  - Listens on port `8080` for incoming HTTP requests from clients (`curl` or browsers).
  - Queues requests and forwards them sequentially over **one persistent TCP connection** to OffshoreProxy.
  - Sends back the response from OffshoreProxy to the client.

- **OffshoreProxy**
  - Runs offshore (remote side).
  - Listens on port `9090` for requests from ShipProxy.
  - Forwards requests to the target server on the internet.
  - Sends responses back to ShipProxy.

---

## 📂 Project Structure

### 📘 Explanation
- **ShipProxy.java** → The client-side proxy that runs on the ship.  
  - Listens on port `8080`.  
  - Accepts HTTP requests from clients (`curl` or browsers).  
  - Queues and forwards them over a single persistent TCP connection to OffshoreProxy.  
- **OffshoreProxy.java** → The server-side proxy that runs offshore.  
  - Listens on port `9090`.  
  - Accepts requests from ShipProxy.  
  - Forwards them to target websites and returns responses.  
- **bin/** → Folder containing compiled `.class` files (ignored by Git).  
- **.gitignore** → Ensures `bin/` and other unnecessary files are not tracked by Git.  
- **README.md** → Documentation for the project (this file).

## ▶️ How to Run

First, compile the code:

```bash
javac -d bin ShipProxy.java OffshoreProxy.java
```

Then open two terminals:

Terminal 1 – Start Offshore Proxy
```bash
java -cp bin OffshoreProxy
```
```bash
Expected:

Offshore Proxy started on port 9090

```


Terminal 2 – Start Ship Proxy
```bash
java -cp bin ShipProxy
```
```bash
Expected:

Ship Proxy started on port 8080
```
✅ Step 2: Run curl with proxy

From a third terminal, run:
```bash

curl.exe -x http://localhost:8080 http://httpforever.com/
```
Expexted:
```bash
A doctype html with header and body content of the page httpforever.
'''
