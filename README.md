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

At this time, `ngrok-java` is only available from [GitHub Packages](https://github.com/features/packages), you'll need to add the following to your `pom.xml`:
```xml
<repositories>
   <repository>
      <id>github</id>
      <name>GitHub Packages</name>
      <url>https://maven.pkg.github.com/ngrok/ngrok-java</url>
   </repository>
</repositories>
```
And then configure your `~/.m2/settings.xml` as per the [docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages)

For example of how to do all that, check out [ngrok-java-demo](https://github.com/ngrok/ngrok-java-demo/blob/main/pom.xml)

# Documentation

# Quickstart

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
