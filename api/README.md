# json-log-filter-api
Fundamental APIs for this library. 

 * `JsonFilter` - basic filter input / output interface
 * `JsonFilterFactory` - set properties and create corresponding JsonFilters 

The actual implementation of `JsonFilterFactory` depends on your use-case, but for example

```java
JsonFilterFactory jsonFilterFactory = DefaultJsonFilterFactory.newInstance();
```

```xml
<dependency>
    <groupId>com.github.skjolber.json-log-filter</groupId>
    <artifactId>api</artifactId>
    <version>x.x.x</version>
</dependency>
```


