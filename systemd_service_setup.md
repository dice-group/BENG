Create the service file:
```bash
cd /etc/systemd/system
sudo touch bengserver.service
```

Copy the following content to `bengserver.service`:
```bash
[Unit]
Description=BENG Server Service
After=network.target

[Service]
User=<your-username>
ExecStartPre=/usr/bin/true
ExecStart=bash <your-beng-path>/service_execute.sh
SuccessExitStatus=143
Restart=always

[Install]
WantedBy=default.target
```

To setup the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable bengserver
sudo systemctl start bengserver
sudo systemctl status bengserver
```
