# spring-boot-autoconfigure-logbook
Spring Boot autoconfiguration for logbook JSON filtering. 

Configures a so-called `Sink` which filters request/responses before logging. 

Note: If Logbook adds path as a field to `HttpResponse`, the `Sink` can be replaced by `HttpRequest` / `HttpResponse`.
