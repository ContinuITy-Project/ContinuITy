# ContinuITy CLI

This is a command line interface (CLI) for ContinuITy. It interacts with the [orchestrator](../continuity.orchestrator) via the provided REST interface.

## Execute using Docker

The simplest way to execute the CLI is as a Docker container, which is provided on [Docker Hub](https://hub.docker.com/r/continuityproject/cli).
Please make sure you have [Docker](https://www.docker.com/) installed and running.
The command for starting the CLI depends on the operating system, as the Docker container needs to use the graphical device of the host for opening YAML files and JMeter test plans.

If you want to persist the content of the CLI working directory on the host machine, you can simply add the absolute path to a local folder as first (second on Windows) argument of the respective script.
This will allow for removing the Docker container without loosing the CLI data.

After the CLI has been started, it will run in background. You can attach to the process with the following command: 

```
docker attach continuity-cli
```

If you want to detach from the process again, you can do this with the keystroke ```Ctrl-p``` ```Ctrl-q```.
Please note that ```exit```, ```quit```, and ```Ctrl-c``` will completely stop the CLI.

### Linux

On Linux, you can simply execute the provided shell script (see [here](http://fabiorehm.com/blog/2014/09/11/running-gui-apps-with-docker/) for details):

```
sh ./startCliDockerLinux.sh
```

> **_NOTE_**
> If you encounter the following exception when opening a YAML file from the CLI ```java.awt.AWTError: Can't connect to X11 window server using ':0' as the value of the DISPLAY variable.```, you might need to change the X server security configurations.
> Execute the following command before starting the Docker container:
> ```
> xhost +local:
> ``` 

### macOS

On macOS, you need to install [XQuartz](https://www.xquartz.org/) by downloading it or by using homebrew: ```brew cask install xquartz```.

Then, start XQuartz, e.g., by executing ```open -a XQuartz```, and start the CLI by using the provided shell script:

```
sh ./startCliDockerMac.sh
```

In case of trouble, please refer to [this blog post](https://sourabhbajaj.com/blog/2017/02/07/gui-applications-docker-mac/).

### Windows

> **_WARNING_** Not tested!

On Windows, you need to install [VcXsrv Windows X Server](https://sourceforge.net/projects/vcxsrv/).
Please refer to [this blog post](https://dev.to/darksmile92/run-gui-app-in-linux-docker-container-on-windows-host-4kde) for the configuration.
Then, determine you IP address using ```ipconfig``` and pass it as first argument to the provided script:

```
startCliDockerWindows.bat <your IP>
```

## Build and Execute Locally

Instead of using Docker, you can also build and run the CLI locally.
Go to the root folder of the continuity.cli subproject and build it:

```
../gradlew :continuity.cli:build
``` 

Then, execute the CLI as a java process:

```
java -jar ./build/libs/continuity.cli-0.1.jar
```

We also provide corresponding scripts ```startCli.[sh|bat]``` and ```startCliWithRemoteDebugging.[sh|bat]```.
