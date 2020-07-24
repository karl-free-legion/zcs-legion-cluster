

echo ">>>start 12101"
nohup /usr/local/jdk1.8_221/bin/java -jar -XX:+UnlockCommercialFeatures -XX:+FlightRecorder legion-cluster-2.6.18.jar --server.port=12101 --legion.server.host=172.16.211.169 --legion.server.port=22101 --legion.cluster[0].host=172.16.211.169 --legion.cluster[0].port=22102 --logging.file=12101 >>/dev/null 2>&1 &
echo ">>>end 12101"
echo " "

sleep 5

echo ">>>start 12102"
nohup /usr/local/jdk1.8_221/bin/java -jar -XX:+UnlockCommercialFeatures -XX:+FlightRecorder legion-cluster-2.6.18.jar --server.port=12102 --legion.server.host=172.16.211.169 --legion.server.port=22102 --legion.cluster[0].host=172.16.211.169 --legion.cluster[0].port=22101 --logging.file=12102 >>/dev/null 2>&1 &
echo ">>>end 12102"
echo " "

sleep 5

echo ">>>start 12103"
nohup /usr/local/jdk1.8_221/bin/java -jar -XX:+UnlockCommercialFeatures -XX:+FlightRecorder legion-cluster-2.6.18.jar --server.port=12103 --legion.server.host=172.16.211.169 --legion.server.port=22103 --legion.cluster[0].host=172.16.211.169 --legion.cluster[0].port=22101 --logging.file=12103 >>/dev/null 2>&1 &
echo ">>>end 12103"
echo " "
