# Drift Codec

Drift Codec is a simple library specifying how Java objects are converted to and
from Thrift.  This library is similar to JaxB (XML) and Jackson (JSON), but
for Thrift.  Drift codec supports field, method, constructor, and builder
injection.

# Structs

To make a Java class a Thrift struct simply add the `@ThriftStruct` annotation.
Drift will assume the Java class and the Thrift struct have the same name, so
if the Thrift struct has a different name, you will need to add a value to
annotation like this: `@ThriftStruct("MyStructName")`.

## Field

The simplest way to add a Thrift field is to annotate a public Java field with
`@ThriftField(42)`.  As with structs, Drift will assume the Java field and
Thrift field have the same name, so if they don't just add a name to the
annotation like this: `@ThriftField(value = 1, name="myFieldName")`.

```java
@ThriftStruct
public class Bonk
{
    @ThriftField(1)
    public String message;

    @ThriftField(2)
    public int type;

    public BonkField()
    {
    }
}
```

## Beans

Traditional Java beans can easily be converted to Thrift structs by annotating
the getters and setters.  Drift will link the getter and setter by name, so you
only need to specify the Thrift field id on one of them.  You can override the
Thrift field name in the annotation if necessary.

```java
@ThriftStruct
public class Bonk
{
    private String message;
    private int type;

    @ThriftField(1)
    public String getMessage()
    {
        return message;
    }

    @ThriftField
    public void setMessage(String message)
    {
        this.message = message;
    }

    @ThriftField(2)
    public int getType()
    {
        return type;
    }

    @ThriftField
    public void setType(int type)
    {
        this.type = type;
    }
}
```

## Constructor

Drift supports immutable Java objects using constructor injection.  Simply,
annotate the constructor you want Drift to use with `@ThriftConstructor`, and
Drift will automatically supply the constructor with the specified fields.
Assuming you have compiled with debug symbols on, the parameters are
automatically matched to a Thrift field (getter or Java field) by name.
Otherwise, you will need to annotate the parameters with
`@ThriftField(name = "myName")`.

```java
@ThriftStruct
public class Bonk
{
    private final String message;
    private final int type;

    @ThriftConstructor
    public Bonk(String message, int type)
    {
        this.message = message;
        this.type = type;
    }

    @ThriftField(1)
    public String getMessage()
    {
        return message;
    }

    @ThriftField(2)
    public int getType()
    {
        return type;
    }
}
```

## Builder

For larger immutable objects, Drift supports the builder pattern.  The Thrift
struct is linked to the builder class using the `builder` property on the
`@ThriftStruct` annotation.  Drift will look for a factory method annotated
with `@ThriftConstructor` on the builder class.  The builder can use field,
method and/or constructor injection in addition to injection into the factory
method itself.

```java
@ThriftStruct(builder = Builder.class)
public class Bonk
{
    private final String message;
    private final int type;

    public Bonk(String message, int type)
    {
        this.message = message;
        this.type = type;
    }

    @ThriftField(1)
    public String getMessage()
    {
        return message;
    }

    @ThriftField(2)
    public int getType()
    {
        return type;
    }

    public static class Builder
    {
        private String message;
        private int type;

        @ThriftField
        public Builder setMessage(String message)
        {
            this.message = message;
            return this;
        }

        @ThriftField
        public Builder setType(int type)
        {
            this.type = type;
            return this;
        }

        @ThriftConstructor
        public Bonk create()
        {
            return new Bonk(message, type);
        }
    }
}
```

# Enumerations

Drift automatically maps Java enumerations to a Thrift int.
The enumeration must be annotated with `@ThriftEnum` and have a method
annotated with `@ThriftEnumValue` that supplies an int value.
Drift does *not* support the potentially error-prone method of using
the Java ordinal for automatic mapping.

One enumeration constant may be annotated with `@ThriftEnumUnknownValue`,
and this constant will be used when an unknown value is encountered during
deserialization.  If no enum constant is designated as the unknown value,
an exception will be thrown instead.

```java
@ThriftEnum
public enum Letter
{
    A(65), B(66), C(67), D(68);

    private final int asciiValue;

    Letter(int asciiValue)
    {
        this.asciiValue = asciiValue;
    }

    @ThriftEnumValue
    public int getAsciiValue()
    {
        return asciiValue;
    }
}
```

# Guice Support

A `ThriftCodec` can be bound into Guice adding the `ThriftCodecModule` to the injector
and bind the codec with the fluent `ThriftCodecBinder` as follows:

```java
Injector injector = Guice.createInjector(Stage.PRODUCTION,
        new ThriftCodecModule(),
        binder -> thriftCodecBinder(binder).bindThriftCodec(Bonk.class));
```

Then, simply add the `ThriftCodec` type to any `@Inject` annotated field, method or constructor:

```java
@Inject
private ThriftCodec<Bonk> bonkCodec;

public void write(Bonk bonk, TProtocol protocol) throws Exception
{
    bonkCodec.write(bonk, protocol);
}
```
