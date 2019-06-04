
SET IP=%1
SET WD=%2

IF "%2"=="" (
    SET WD=continuity-cli
)

docker run -d --name continuity-cli -it -e DISPLAY=%IP%:0.0 -v /tmp/.X11-unix:/tmp/.X11-unix -v %WD%:/working-dir continuityproject/cli