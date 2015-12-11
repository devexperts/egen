
# EGEN - Tool for compile-time optimization of Java classes serialization

[ ![Download](https://api.bintray.com/packages/devexperts/Maven/egen/images/download.svg) ](https://bintray.com/devexperts/Maven/egen/_latestVersion)

It's aimed to provide efficient custom serialization strategy for dedicated classes without compromising compatibility with
default java serialization approach and writing serialization code by hand.


Java serialization API provides following interface for custom serialization:

````java
    void writeObject(ObjectOutputStream out) throws IOException;

    void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException;
````

The tool is implemented as annotation processor invoked by javac during compilation. It analyzes classes marked with
`@AutoSerializable` annotation and generates methods on the fly.

EGEN stands for Externalizable GENerator, but actually it generates only writeObject()/readObject() methods. We have refused of implementing java.io.Externalizable interface due to inheritance issues. 


## Usage


To activate tool the egen-\<version\>.jar provided by package shall be placed in compilation class path with its dependencies. 
If you use maven for build just add EGEN as dependency. 

**Note:** Bundled variant with all needed classes provided as egen-\<version\>-bundled.jar for convenience.  

The tool will process all classes marked with `com.devexperts.egen.processor.annotations.AutoSerializable` annotation and generate appropriate code:

````java
    import com.devexperts.egen.processor.annotations.Autoserializable;

    @AutoSerializable
    class VeryLargeClass {
        private int count;
        private String text;

        protected double[] prices;
        protected Object cachedDataBaseObject;
        ...
    }
````


**Important: generated code will use class [IOUtil](http://docs.dxfeed.com/dxlib/api/com/devexperts/io/IOUtil.html) from `com.devexperts.io` package of dxlib library which can be found on [dxFeed](http://www.dxfeed.com/) distribution repository:**

Manual download:

<http://maven.dxfeed.com/release/com/devexperts/qd/dxlib/3.135/dxlib-3.135.jar>

Maven:

```xml
    <repositories>
        <repository>
            <id>maven-dxfeed-com</id>
            <url>http://maven.dxfeed.com/release</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.devexperts.qd</groupId>
            <artifactId>dxlib</artifactId>
            <version>3.135</version>
        </dependency>
    </dependencies>
```  

### Serialization directives

EGEN supports various directives implemented as annotations for adjusting serialization algorithm:

#### @Compact

Compact representation of primitives, arrays, collections and maps.

`@Compact int a;` — field serialized by IOUtils.writeCompactInt(a)

`@Compact long[] barr;` — array length serialized as compact int and elements by `writeCompactLong(v[i])`

`@Compact V[];`  
`@Compact ArrayList<V>;`  
`@Compact HashMap<K, V>;` — compact int for size, then elements one by one. Compaction applied recursively: if types `K`,`V` are compactible then compaction will be applied.

**Supported types:** `int`, `long`, `Integer`, `Long`, `String`, `int[]`, `long[]`, array of objects, non-abstract implementations [1] of `Collection<V>` and `Map<K, V>`.

#### @PresenceBit

Marks field (field group) that usually has default value (all fields at once for group).

````java
    @PresenceBit(value = "6") int a;
    @PresenceBit(value = "7e3", groupId = 1) double d1;
    @PresenceBit(value = "5", groupId = 1) double d2;
````

If group (single field is a group of one) is in default state it's coded as 1 bit in mask or all fields in group serialized using `@Compact` method. Group state mask serialized by `writeCompactLong` (so maximum 64 groups per class supported).

**Supported types:** `String`, all primitives and box-classes

#### @Inline

Skip object type descriptor for `@AutoSerializable` field.

**Important!** Inlined class shall has public default constructor and all its ancestors shall be `@AutoSerializable` also (except Object). The field shall be exactly of declared type (using subclasses is not allowed).

**Supported types:** any class marked by `@AutoSerializable` [2]

#### @Ordinal

Support for nonstandard ordinal types (like enum surrogates appeared before native java support of enum).

`@Ordinal` class A shell provide following methods:

* Instance method `int code()` shall return code of an object.
* Class method `static A findByCode(Class<A>, int)` shell return instance by type token and code.

#### @Delta

Coding numeric fields and arrays with usually close values.

````java
    int a1;
    @Delta("a1") int a2; // a2 encoded as writeCompactInt(a2 - a1)
    @Delta("42") int a3; // a3 encoded as writeCompactInt(a3 - 42)

    @Delta long[] v; // length and v[0] encoded usual way, following elements as writeCompactLong(v[i]-v[i-1])
````
**Important!** Generated code requires classes from egen.jar.

**Supported types:** int, long, int[], long[]

#### @AutoSerializationStrategy

Meta annotation (annotation for annotations) to specify new serialization strategies.
Has following parameters:

* `targetStrategy` - IOUtil method of encoding
* `toTarget` - method to convert original value to presentation suitable for `targetStrategy`
* `fromTarget` - method to convert strategy-value to original

Encoding will be performed by template `com.devexperts.io.IOUtil.write<targetStrategy>(out,<toTarget>(field));`, decoding - simmetrically - `fromTarget(...)`

Example: 

Given Decimal class with methods `int compose(double)` and `double toDouble(int)` for doubles with limited precision

````java
    @AutoSerializationStrategy(targetStrategy=”CompactInt”,
        toTarget=”util.Decimal.compose”,
        fromTarget=”util.Decimal.toDouble”)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Decimal {}

````
Now we can use it following way:

```java
    @AutoSerializable 
    class C {
        @Decimal double d;
        ...
    }
```


**Footnotes:**

[1]: By default recursive compaction provided for standard containers `ArrayList`, `LinkedList`, `HashSet`, `TreeSet`, `HashMap` and `TreeMap`. To activate compaction for some other classes they shall be specified by following options of `javac`: `-Aordinals`, `-Amaps` and `-Acollections`.

Example: `javac -Acollections=java.util.concurrent.LinkedBlockingDeque ...`

[2]: If class A contains  @Inline-field of class B, then A and B shall be compiled by single invocation of javac (otherwise it won't compile). 
