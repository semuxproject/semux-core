# Full Node Setup

## Debian/Ubuntu Linux

### Using pre-compiled binaries

**Install JRE 8**

```bash
sudo apt-get install openjdk-8-jre
```

**Download & extract Semux pre-compiled binaries of the latest release**

```bash
wget https://github.com/semuxproject/semux/releases/download/v1.1.1/semux-linux-1.1.1-a089548.tar.gz
tar -zxvf semux-linux-1.1.1-a089548.tar.gz 
```

### Compiling from source code

**Install OpenJDK 8 & Maven**
```bash
sudo apt-get install openjdk-8-jdk maven
```

**Clone source code**
```bash
git clone https://github.com/semuxproject/semux.git
```

**Compile**
```bash
mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V
```

The compiled binaries should be available at `dist/linux` once the compilation has completed.

### Create a wallet.data file

A `wallet.data` file that stores a new private key should be automatically created in your installation directory during the startup of a fresh install of Semux wallet. This file is encrypted by a password specified in a CLI/GUI prompt during the first time of wallet startup.

```bash
$ ./semux-cli.sh                    
Please enter the new password: 
Please re-enter the new password: 
21:06:11.440 INFO     SemuxCli         A new account has been created for you: address = 426ae10caffcc5cbe623866cff1ec00c9501654c
```

### Create a systemd service

A systemd service unit can be created at `/etc/systemd/system/semux.service` using this template file: [semux.service](../misc/systemd/semux.service).

```bash
sudo cp semux.service /etc/systemd/system/semux.service
sudo systemctl daemon-reload
sudo systemctl enable semux.service
sudo systemctl start semux.service
```