@echo off
echo Corrigiendo server.properties...

(
echo #Minecraft server properties - Optimizado para pruebas de plugins
echo accepts-transfers=false
echo allow-flight=true
echo allow-nether=false
echo broadcast-console-to-ops=true
echo broadcast-rcon-to-ops=true
echo bug-report-link=
echo debug=false
echo difficulty=easy
echo enable-command-block=true
echo enable-jmx-monitoring=false
echo enable-query=false
echo enable-rcon=false
echo enable-status=true
echo enforce-secure-profile=false
echo enforce-whitelist=false
echo entity-broadcast-range-percentage=100
echo force-gamemode=false
echo function-permission-level=2
echo gamemode=creative
echo generate-structures=false
echo generator-settings={}
echo hardcore=false
echo hide-online-players=false
echo initial-disabled-packs=
echo initial-enabled-packs=vanilla
echo level-name=world
echo level-seed=
echo level-type=minecraft:normal
echo log-ips=true
echo max-chained-neighbor-updates=1000000
echo max-players=5
echo max-tick-time=60000
echo max-world-size=29999984
echo motd=Servidor de Pruebas - TorneoMMT
echo network-compression-threshold=256
echo online-mode=false
echo op-permission-level=4
echo player-idle-timeout=0
echo prevent-proxy-connections=false
echo pvp=false
echo query.port=25565
echo rate-limit=0
echo rcon.password=
echo rcon.port=25575
echo region-file-compression=deflate
echo require-resource-pack=false
echo resource-pack=
echo resource-pack-id=
echo resource-pack-prompt=
echo resource-pack-sha1=
echo server-ip=
echo server-port=25565
echo simulation-distance=4
echo spawn-animals=false
echo spawn-monsters=false
echo spawn-npcs=false
echo spawn-protection=0
echo sync-chunk-writes=false
echo text-filtering-config=
echo use-native-transport=true
echo view-distance=4
echo white-list=false
) > server.properties

echo.
echo Eliminando mundos antiguos...
rmdir /s /q world 2>nul
rmdir /s /q world_nether 2>nul
rmdir /s /q world_the_end 2>nul

echo.
echo ===================================
echo Configuracion aplicada correctamente!
echo ===================================
echo - Tipo de mundo: NORMAL (optimizado)
echo - Gamemode: CREATIVE
echo - View Distance: 4 chunks
echo - Simulation Distance: 4 chunks
echo - Max Players: 5
echo - Spawn: Desactivado
echo - Nether/End: Desactivados
echo - PvP: Desactivado
echo - Estructuras: Desactivadas
echo ===================================
echo.
echo NOTA: El mundo sera normal pero sin estructuras
echo para evitar errores de generacion. Es perfecto
echo para pruebas de plugins.
echo ===================================
echo.
echo Ahora puedes iniciar el servidor con: .\start.bat
pause
