# CXF async client issue
To reproduce the issue
- mvn spring-boot:run

There is a timer that calls the cxf producers every 4 seconds that should automatically show the generated errors.

To fix the issue uncomment this dependency in the pom.xm and run it again:
``` 
<dependency>
    <groupId>org.apache.cxf</groupId>
    <artifactId>cxf-rt-transports-http-hc</artifactId>
</dependency>
```