#
# Java 1.8 & Maven Dockerfile
#

FROM niczhao/jdk1.8.0_221:v1

# Define project name/version(from pom.xml[artifactId/version])
ARG PROJECT_NAME
ARG PROJECT_VERSION

ENV MEMORY_SIZE 1024M
ENV NAME ${PROJECT_NAME}-${PROJECT_VERSION}.jar

ENV JAVA_OPTS "-server -Xms$MEMORY_SIZE -Xmx$MEMORY_SIZE -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m \
               -XX:+AggressiveOpts -XX:+UseBiasedLocking \
               -XX:+DisableExplicitGC -XX:MaxTenuringThreshold=4 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
               -XX:+CMSParallelRemarkEnabled -XX:LargePageSizeInBytes=128m \
               -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true \
               -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/gc.log -Dfile.encoding=utf-8 \
               -XX:+UnlockCommercialFeatures -XX:+FlightRecorder"
RUN set -x \
#    \
    && sed -i "s/override_install_langs=en_US.utf8/#override_install_langs=en_US.utf8/g" /etc/yum.conf \
#    && yum install -y net-tools \
    && localedef -c -f UTF-8 -i zh_CN zh_CN.utf8 \
    && export LC_ALL="zh_CN.utf8" && export LANG="zh_CN.utf8" \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' > /etc/timezone

ENV LC_ALL zh_CN.UTF-8
ENV LANG zh_CN.UTF-8
ADD target/$NAME /$NAME

#COPY dev/ /ccks/

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar $JAVA_OPTS /$NAME --spring.config.location=/mnt/