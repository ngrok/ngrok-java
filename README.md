# The ngrok Agent SDK for Java

[![MIT licensed][mit-badge]][mit-url]
[![Apache-2.0 licensed][apache-badge]][apache-url]
[![Continuous integration][ci-badge]][ci-url]


[mit-badge]: https://img.shields.io/badge/license-MIT-blue.svg
[mit-url]: https://github.com/ngrok/ngrok-rs/blob/main/LICENSE-MIT
[apache-badge]: https://img.shields.io/badge/license-Apache_2.0-blue.svg
[apache-url]: https://github.com/ngrok/ngrok-rs/blob/main/LICENSE-APACHE
[ci-badge]: https://github.com/ngrok/ngrok-java/actions/workflows/ci.yml/badge.svg
[ci-url]: https://github.com/ngrok/ngrok-java/actions/workflows/ci.yml

[Website](https://ngrok.com)

**Note: This is alpha-quality software. Interfaces are subject to change without warning.**

ngrok is a globally distributed reverse proxy commonly used for quickly getting a public URL to a
service running inside a private network, such as on your local laptop. The ngrok agent is usually
deployed inside a private network and is used to communicate with the ngrok cloud service.

This is the ngrok agent in library form, suitable for integrating directly into Java applications. 
This allows you to quickly build ngrok into your application with no separate process to manage.

# Installation

## Maven
To use `ngrok-java`, you need to add the following to your `pom.xml`:

```xml
<project>
   <!-- your project definition -->

   <dependencies>
      <dependency>
         <groupId>com.ngrok</groupId>
         <artifactId>ngrok-java</artifactId>
         <version>${ngrok.version}</version>
      </dependency>
      <dependency>
         <groupId>com.ngrok</groupId>
         <artifactId>ngrok-java-native</artifactId>
         <version>${ngrok.version}</version>
         <!-- automatically detect classifier, you can also pin manually -->
         <classifier>${os.detected.classifier}</classifier>
         <scope>runtime</scope>
      </dependency>
   </dependencies>
   
   <build>
     <extensions>
         <extension>
             <groupId>kr.motd.maven</groupId>
             <artifactId>os-maven-plugin</artifactId>
             <version>1.7.0</version>
         </extension>
     </extensions>
   </build>
</project>
```

If you want to use [jetty](https://www.eclipse.org/jetty/) integration, also add:
```xml
<dependency>
   <groupId>com.ngrok</groupId>
   <artifactId>ngrok-jetty</artifactId>
   <version>${ngrok.version}</version>
</dependency>
```

For example of how to setup your project, check out [ngrok-java-demo](https://github.com/ngrok/ngrok-java-demo/blob/main/pom.xml)

# Documentation

## Quickstart

```java
import com.ngrok.Session;
import com.ngrok.HttpTunnel;

public class Echo {
   public static void main(String[] args) throws IOException {
      // Session.newBuilder() will create a new session builder, pulling NGROK_AUTHTOKEN env variable. 
      // You can get your authtoken by registering at https://dashboard.ngrok.com
      var sessionBuilder = Session.newBuilder().metadata("my session");
      // Session.Builder let you customize different aspects of the session, see docs for details.
      // After customizing the builder, you connect:
      try (var session = Session.connect(sessionBuilder)) {
         // Creates an http tunnel that will be using oauth to secure it
         var tunnelBuilder = new HttpTunnel.Builder().metadata("my tunnel")
                                                     .oauthOptions(new HttpTunnel.OAuthOptions("google")));
         // Now start the tunnel with the above configuration
         try (var tunnel = session.httpTunnel(tunnelBuilder)) {
            System.out.println("ngrok url: " + tunnel.getUrl());
            var buf = ByteBuffer.allocateDirect(1024);

            while (true) {
               // Accept a new connection
               var conn = tunnel.accept();

               // Read from the connection
               conn.read(buf);

               System.out.println(buf.asCharBuffer());

               // Or write to it
               conn.write(buf);
            }
         }
      }
   }
}
```

## Configuring Logging
Log level is set from the your `slf4j` implementation's configuration. This level must be assigned before creating a session, as it is read on creation.

As an example, to configure `slf4j-simple`, you can do:
```java
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
```

# License

This project is licensed under either of

 * Apache License, Version 2.0, ([LICENSE-APACHE](LICENSE-APACHE) or
   http://www.apache.org/licenses/LICENSE-2.0)
 * MIT license ([LICENSE-MIT](LICENSE-MIT) or
   http://opensource.org/licenses/MIT)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in ngrok-java by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.
