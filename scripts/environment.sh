# Source this file in the prepared cloud environment.
export JAVA_HOME=/workspace/tools/jdk-17.0.21+9
export ANDROID_HOME=/workspace/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
export FIGHTFORGE_SIGNING_DIR=/workspace/fightforge-signing
if [ -n "${HTTPS_PROXY:-}" ]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
