# Full Node Setup

## Ports

Port 5161 is required to be open for running a full Semux node.

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

### Automatic wallet unlock

The standard wallet.data file of Semux is always encrypted even if you entered an empty password during wallet creation. Therefore the wallet password is required to be provided for automatic unlock when you setup a full node.

The following ways are available for automatic wallet unlock which will be applied in sequence:

1. Set `--password` CLI option as your wallet password when starting `semux-cli.sh` executable. This is considered as an insecured way as the way will expose your wallet password to all users through process monitor.
2. Set `wallet.password` property as your wallet password in your `config/semux.properties` config file.
3. Set `SEMUX_WALLET_PASSWORD` environment variable as your wallet password.

### Create a systemd service

A systemd service unit can be created at `/etc/systemd/system/semux.service` using this template file: [semux.service](../misc/systemd/semux.service).

```bash
sudo cp semux.service /etc/systemd/system/semux.service
sudo systemctl daemon-reload
sudo systemctl enable semux.service
sudo systemctl start semux.service
```