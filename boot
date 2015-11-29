#/usr/bin/env bash

boot_classpath=`boot show -c`;
alpn_jar="";

IFS=':' read -ra boot_classpath <<< "$boot_classpath";
for i in "${boot_classpath[@]}"; do
    if [[ `echo "$i" | grep "alpn-boot"` ]]; then
      alpn_jar=$i;
    fi
done

echo "ALPN jar found at: $alpn_jar";

read -d '' BOOT_JVM_OPTIONS<<-EOF
-Xmx2g -client -XX:+UseCompressedOops -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none \
-Xbootclasspath/p:$alpn_jar -Dclojure.compiler.disable-locals-clearing=true \
-XX:-OmitStackTraceInFastThrow -XX:+CMSClassUnloadingEnabled -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
-XX:ParallelGCThreads=10 -XX:ConcGCThreads=4 -XX:InitiatingHeapOccupancyPercent=75 \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
EOF

BOOT_JVM_OPTIONS="$BOOT_JVM_OPTIONS" boot "$@";
