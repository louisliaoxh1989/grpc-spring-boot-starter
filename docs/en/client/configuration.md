# Configuration

[<- Back to index](../index)

This section describes how you can configure your grpc-spring-boot-starter clients.

## Table of Contents <!-- omit in toc -->

- [Configuration via Properties](#configuration-via-properties)
  - [Choosing the Target](#choosing-the-target)
- [Configuration via Beans](#configuration-via-beans)
  - [GrpcChannelConfigurer](#grpcchannelconfigurer)
  - [StubTransformer](#stubtransformer)

## Additional Topics <!-- omit in toc -->

- [Getting Started](getting-started)
- *Configuration*
- [Security](security)

## Configuration via Properties

grpc-spring-boot-starter can be
[configured](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html) via
spring's `@ConfigurationProperties` mechanism.

You can find all build-in configuration properties here:

- [`GrpcChannelsProperties`](https://javadoc.io/page/net.devh/grpc-client-spring-boot-autoconfigure/latest/net/devh/boot/grpc/client/config/GrpcChannelsProperties.html)
- [`GrpcChannelProperties`](https://javadoc.io/page/net.devh/grpc-client-spring-boot-autoconfigure/latest/net/devh/boot/grpc/client/config/GrpcChannelProperties.html)
- [`GrpcServerProperties.Security`](https://static.javadoc.io/net.devh/grpc-client-spring-boot-autoconfigure/latest/net/devh/boot/grpc/client/config/GrpcChannelProperties.Security.html)

If you prefer to read the sources instead, you can do so
[here](https://github.com/yidongnan/grpc-spring-boot-starter/blob/master/grpc-client-spring-boot-autoconfigure/src/main/java/net/devh/boot/grpc/client/config/GrpcChannelProperties.java#L58).

The properties for the channels are all prefixed with `grpc.client.__name__.` and `grpc.client.__name__.security.`
respectively. The channel name is taken from the `@GrpcClient` annotation. If you wish to configure some options such as
trusted certificates for all servers at once you can do so using `GLOBAL` as name. Properties that are defined for a
name channel take precedence over global once.

### Choosing the Target

You can change the target server using the following property:

````properties
grpc.client.__name__.address=static://localhost:9090`
````

There are a number of supported schemes, that you can use to determine the target server (Priorities 0 (low) - 10
(high)):

- `static` (Prio 4):
  A simple static list of IPs (both v4 and v6), that can be use connect to the server (Supports `localhost`).
  Example: `static://192.168.1.1:8080,10.0.0.1:1337`
- [`dns`](https://github.com/grpc/grpc-java/blob/master/core/src/main/java/io/grpc/internal/DnsNameResolver.java#L66) (Prio 5):
  Resolves all addresses that are bound to the given DNS name. The addresses will be cached and will only be refreshed
  if an existing connection is shutdown/fails. More options such as `SVC` lookups (useful for kubernetes) can be enabled
  via system properties.
  Example: `dns:///example.my.company`
- `discovery` (Prio 6):
  (Optional) Uses spring-cloud's `DiscoveryClient` to lookup appropriate targets. The connections will be refreshed
  automatically during `HeartbeatEvent`s. Uses the `gRPC.port` metadata to determine the port, otherwise uses the
  service port.
  Example: `discovery:///service-name`
- `self` (Prio 0):
  The self address or scheme is a keyword that is available, if you also use `grpc-server-spring-boot-starter` and
  allows you to connect to the server without specifying the own address/port. This is especially useful for tests
  where you might want to use random server ports to avoid conflicts.
  Example: `self` or `self:self`
- `in-process`:
  This is a special scheme that will bypass the normal channel factory and will use the `InProcessChannelFactory`
  instead. Use it to connect to the [`InProcessServer`](../server/configuration#enabling-the-inprocessserver).
  Example: `in-process:foobar`
- *custom*:
  You can define custom
  [`NameResolverProvider`s](https://javadoc.io/page/io.grpc/grpc-all/latest/io/grpc/NameResolverProvider.html) those
  will be picked up, by either via Java's `ServiceLoader` or from spring's application context and registered in
  the `NameResolverRegistry`.

If you don't define an address it will be guessed:

- First it will try it with just it's name (`<name>`)
- If you have configured a default scheme it will try that next (`<scheme>:/<name>`)
- Then it will use the default scheme of the `NameResolver.Factory` delegate (See the priorities above)

> **Note:** The number of slashes is important! Also make sure that you don't try to connect to a normal
> web/REST/non-grpc server (port).

The `SSL`/`TLS` and other security relevant configuration is explained on the [Client Security](security) page.

## Configuration via Beans

While this library intents to provide most of the features as configuration option, sometimes the overhead for adding it
is too high and thus we didn't add it, yet. If you feel like it is an important feature, feel free to open a feature
request.

If you want to change the application beyond what you can do through the properties, then you can use the existing
extension points that exist in this library.

First of all most of the beans can be replaced by custom ones, that you can configure in every way you want.
If you don't wish to go that far, you can use classes such as `GrpcChannelConfigurer` and `StubTransformer` to configure
the channels, stubs and other components without losing the features provided by this library.

### GrpcChannelConfigurer

The grpc client configurer allows you to add your custom configuration to grpc's `ManagedChannelBuilder`s.

````java
@Bean
public GrpcChannelConfigurer keepAliveClientConfigurer() {
    return (channelBuilder, name) -> {
        if (channelBuilder instanceof NettyChannelBuilder) {
            ((NettyChannelBuilder) channelBuilder)
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(5, TimeUnit.SECONDS);
        }
    };
}
````

> Be aware that depending on your configuration there might be different types of `ManagedChannelBuilder`s in the
> application context (e.g. the `InProcessChannelFactory`).

### StubTransformer

The stub transformer allows you to modify `Stub`s right before they are injected to your beans.

````java
@Bean
public StubTransformer call() {
    return (name, stub) -> {
        if ("serviceA".equals(name)) {
            return stub.withWaitForReady();
        } else {
            return stub;
        }
    };
}
````

## Additional Topics <!-- omit in toc -->

- [Getting Started](getting-started)
- *Configuration*
- [Security](security)

----------

[<- Back to index](../index)