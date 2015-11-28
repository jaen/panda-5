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

BOOT_JVM_OPTIONS="-Xmx4g -server -XX:+UseCompressedOops -Xbootclasspath/p:$alpn_jar -Dclojure.compiler.disable-locals-clearing=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" boot "$@";
