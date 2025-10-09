@echo off
echo Iniciando servidor de Minecraft...
echo.

REM Configuración de memoria (ajusta según tus necesidades)
set JAVA_OPTS=-Xms1G -Xmx2G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=100 -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -XX:+UseCompressedOops -XX:+OptimizeStringConcat -XX:+UseStringDeduplication

REM Ejecutar servidor Paper
java %JAVA_OPTS% -jar paper.jar nogui

echo.
echo Servidor detenido.
pause
