# 1.背景及特性
在 Java 8 之前，处理日期和时间主要依靠 `java.util.Date` 和 `java.util.Calendar`。它们主要有下列的问题：
+ 非线程安全
  + java.util.Date 是非线程安全的，所有的日期类都是可变的，这是Java日期类最大的问题之一
+ 设计糟糕（
  + Date 的月份从 0 开始，年份从 1900 开始，容易出错。
  + java.util.Date同时包含日期和时间，而java.sql.Date仅包含日期，将其纳入java.sql包并不合理，以及名称相同。
+ 时区支持弱
  + 日期类并不提供国际化，没有时区支持，因为Date本身没有时区概念，依赖 Calendar 处理。

针对上述问题，Java 8 在java.time包下提供了很多新的API来解决问题，带来的核心优势如下:
1. 不可变 (Immutable)：所有核心类都是不可变的，线程安全。
2. 清晰分离：机器时间 (Instant) 与人类时间 (LocalDate) 分离。
3. 流畅 API：方法命名清晰（如 plusDays, minusMonths）。
4. 完善的时区支持：内置 ZoneId 和 ZonedDateTime。

## 2. 核心类详解
### 2.1 机器时间 (Machine Time)
用于计算机内部记录时间戳，精度通常为纳秒。

**`Instant`**
  *   **含义**：时间线上的一个瞬时点（自 1970-01-01T00:00:00Z 起的秒数 + 纳秒）。
  *   **用途**：数据库存储、日志记录、计算时间差。

**示例**：
```java
Instant now = Instant.now();
long epochSecond = now.getEpochSecond(); // 秒级时间戳
long nano = now.getNano(); // 纳秒部分
```

### 2.2 本地日期时间 (Local Date/Time)
**不包含时区信息**，仅表示“日历上的日期”或“钟表上的时间”。

*   **`LocalDate`** (日期：2023-10-27)
```java
LocalDate date = LocalDate.now();
LocalDate specific = LocalDate.of(2023, 10, 27);
int year = date.getYear();
Month month = date.getMonth(); // 返回枚举 Month.OCTOBER
```
*   **`LocalTime`** (时间：14:30:00)
```java
LocalTime time = LocalTime.now();
LocalTime specific = LocalTime.of(14, 30, 0);
```
*   **`LocalDateTime`** (日期 + 时间)
*   **注意**：它**不是**时间戳，无法直接转换为毫秒，因为它不知道时区。
```java
LocalDateTime ldt = LocalDateTime.now();
LocalDate datePart = ldt.toLocalDate();
LocalTime timePart = ldt.toLocalTime();
```

### 2.3 时区感知时间 (Zoned Time)
包含时区信息，用于表示全球特定地点的时间。

*   **`ZoneId`** (时区 ID)
```java
ZoneId shanghai = ZoneId.of("Asia/Shanghai");
ZoneId utc = ZoneId.of("UTC");
Set<String> allZones = ZoneId.getAvailableZoneIds();
```
*   **`ZonedDateTime`** (带时区的日期时间)
```java
ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("shanghai"));
ZonedDateTime utcTime = zdt.withZoneSameInstant(ZoneId.of("UTC")); // 转换时区，时刻不变
```
*   **`OffsetDateTime`** (带偏移量的日期时间)
*   比 `ZonedDateTime` 轻量，只记录与 UTC 的偏移量（如 +08:00），不包含夏令时规则。

## 3. 时间量与计算 (Amounts & Calculation)

### 3.1 时间间隔
*   **`Period`** (基于日期的间隔)
  *   用于 `LocalDate` 计算。
*   单位：年、月、日。
```java
Period p = Period.ofDays(5);
Period between = Period.between(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10));
int days = between.getDays();
```

*   **`Duration`** (基于时间的间隔)
  *   用于 `Instant`, `LocalTime`, `LocalDateTime` 计算。
  *   单位：秒、纳秒。
```java
Duration d = Duration.ofHours(2);
Duration between = Duration.between(LocalTime.of(10,0), LocalTime.of(12,0));
long seconds = between.getSeconds();
```

### 3.2 时间运算 (TemporalAdjusters)
*   **链式调用**：
    ```java
    LocalDate nextWeek = LocalDate.now().plusWeeks(1);
    LocalDateTime nextYear = LocalDateTime.now().plusYears(1).minusDays(1);
    ```
*   **调整器 (Adjusters)**：处理复杂逻辑（如“下个月的第一个周一”）。
    ```java
    import static java.time.temporal.TemporalAdjusters.*;

    LocalDate firstDayOfMonth = LocalDate.now().with(firstDayOfMonth());
    LocalDate nextMonday = LocalDate.now().with(nextOrSame(DayOfWeek.MONDAY));
    ```

### 3.3 时间单位枚举 (ChronoUnit)
实现了 `TemporalUnit` 接口，用于更细粒度的计算。
```java
long daysBetween = ChronoUnit.DAYS.between(LocalDate.of(2023,1,1), LocalDate.of(2023,1,10));
```

## 4. 格式化与解析 (Formatting)

**`DateTimeFormatter`** 是线程安全的（不可变），可定义为 `static final` 常量复用。

### 4.1 预定义格式
```java
String isoDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); // 2023-10-27
```

### 4.2 自定义格式
```java
// 定义格式
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

// 格式化 (Object -> String)
String text = LocalDateTime.now().format(formatter);

// 解析 (String -> Object)
LocalDateTime parsed = LocalDateTime.parse("2023-10-27 10:00:00", formatter);
```

### 4.3 本地化格式
```java
DateTimeFormatter chineseFormatter = DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日")
    .withLocale(Locale.CHINA);
```

## 5. 新旧 API 转换 (Migration)

在实际项目中，常需与旧代码 (`java.util.Date`) 交互。

### 5.1 Date ↔ Instant
```java
// Date -> Instant
Date oldDate = new Date();
Instant instant = oldDate.toInstant();

// Instant -> Date
Date newDate = Date.from(instant);
```

### 5.2 Date ↔ LocalDateTime (需借助时区)
```java
// Date -> LocalDateTime
Date oldDate = new Date();
LocalDateTime ldt = LocalDateTime.ofInstant(oldDate.toInstant(), ZoneId.systemDefault());

// LocalDateTime -> Date
LocalDateTime ldt = LocalDateTime.now();
Date newDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
```

### 5.3 Calendar ↔ ZonedDateTime
```java
// Calendar -> ZonedDateTime
Calendar cal = Calendar.getInstance();
ZonedDateTime zdt = cal.toInstant().atZone(ZoneId.systemDefault());

// ZonedDateTime -> Calendar
Calendar newCal = Calendar.from(zdt);
```

## 6. 最佳实践总结 (Cheat Sheet)

| 场景 | 推荐类 | 说明 |
| :--- | :--- | :--- |
| **数据库存储** | `Instant` | 统一存储为 UTC 时间戳，避免时区混乱。 |
| **业务逻辑 (无时区)** | `LocalDate` / `LocalDateTime` | 如生日、会议时间（不关心具体时区）。 |
| **展示给用户** | `ZonedDateTime` | 结合用户所在时区进行格式化展示。 |
| **计算时间差** | `Duration` / `Period` | 根据是算“秒数”还是“天数”选择。 |
| **格式化** | `DateTimeFormatter` | **线程安全**，定义为静态常量。 |
| **避免使用** | `Date`, `Calendar`, `SimpleDateFormat` | 除非维护旧代码，否则不再使用。 |

### ⚠️ 常见坑点
1.  **`LocalDateTime` 不是时间戳**：它没有时区，不能直接转毫秒。必须先转 `ZonedDateTime` 或 `Instant`。
2.  **月份从 1 开始**：`LocalDate.of(2023, 1, 1)` 是 1 月，而旧 `Date` 是 0 月。
3.  **不可变性**：所有 `plus`, `minus`, `with` 方法都会**返回新对象**，原对象不变。
    ```java
    // 错误
    date.plusDays(1); 
    // 正确
    date = date.plusDays(1);
    ```
4.  **数据库支持**：JDBC 4.2+ 直接支持 `java.time` 类型，无需转换。
    ```java
    preparedStatement.setObject(1, localDateTime);
    resultSet.getObject(1, LocalDateTime.class);
    ```
5. 在 Spring Boot 项目中，默认 Jackson 配置可能无法直接序列化 `java.time` 对象，需确保引入 `jackson-module-java8` 或配置 `JavaTimeModule`。
6. MySQL 驱动建议使用 8.0+，以更好地支持 `java.time` 类型映射。

# 附录
## 参考文档
* [Java 8 日期时间 API](https://www.runoob.com/java/java8-datetime-api.html)
* [Oracle Java 8 Date/Time API Guide](https://docs.oracle.com/javase/tutorial/datetime/iso/index.html)
* [廖雪峰-java教程-日期与时间](https://liaoxuefeng.com/books/java/datetime/index.html)
