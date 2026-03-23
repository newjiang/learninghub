# Java 8 Optional类

## 1. 背景与动机 (Background)

*   **起源**：Java 8 引入，灵感来源于 Haskell 和 Scala。
*   **目的**：解决 **`NullPointerException` (NPE)** 问题。Tony Hoare 曾称引入 null 引用为“价值十亿美元的错误”。
*   **核心思想**：`Optional` 是一个**容器对象**，可能包含也可能不包含非空值。它强制开发者显式处理值存在或不存在的情况，而不是隐式地假设值不为 null。
*   **定位**：主要用于**方法返回值**，不建议用于字段或方法参数。

## 2. 核心 API 详解 

### 2.1 创建 Optional 对象

| 方法 | 签名 | 说明 | 注意事项 |
| :--- | :--- | :--- | :--- |
| **empty** | `Optional<T> empty()` | 返回一个空的 Optional 实例。 | 单例模式，性能好。 |
| **of** | `Optional<T> of(T value)` | 创建一个包含非 null 值的 Optional。 | 如果 `value` 为 null，直接抛 `NullPointerException`。 |
| **ofNullable** | `Optional<T> ofNullable(T value)` | 如果值为 null，返回空 Optional；否则返回包含值的 Optional。 | **最常用**，用于处理可能为 null 的现有值。 |

```java
Optional<String> empty = Optional.empty();
Optional<String> notNull = Optional.of("Hello"); // "Hello" 不能为 null
Optional<String> nullable = Optional.ofNullable(getValue()); // getValue() 可为 null
```

### 2.2 获取值 (Retrieval)

| 方法 | 签名 | 说明 | 推荐度 |
| :--- | :--- | :--- | :--- |
| **get** | `T get()` | 返回值，如果为空抛 `NoSuchElementException`。 | ⭐ (慎用) |
| **orElse** | `T orElse(T other)` | 如果有值返回值，否则返回 `other`。 | ⭐⭐⭐ (注意参数执行时机) |
| **orElseGet** | `T orElseGet(Supplier<? extends T> supplier)` | 如果有值返回值，否则由 Supplier 生成默认值。 | ⭐⭐⭐⭐ (推荐，懒加载) |
| **orElseThrow** | `T orElseThrow(Supplier<? extends X> exceptionSupplier)` | 如果有值返回值，否则抛出指定异常。 | ⭐⭐⭐⭐ (推荐) |

**⚠️ 重要区别：`orElse` vs `orElseGet`**
*   `orElse(new User())`：无论 Optional 是否有值，`new User()` **都会执行**。
*   `orElseGet(() -> new User())`：只有 Optional 为空时，`new User()` **才会执行**。
*   **结论**：如果默认值创建开销大，务必使用 `orElseGet`。

### 2.3 转换与过滤 (Transformation & Filtering)

| 方法 | 签名 | 说明 | 示例场景 |
| :--- | :--- | :--- | :--- |
| **map** | `<U> Optional<U> map(Function...)` | 对值进行处理，结果被包装在新的 Optional 中。 | 提取对象属性：`userOpt.map(User::getName)` |
| **flatMap** | `<U> Optional<U> flatMap(Function...)` | 对值进行处理，处理结果**必须已经是 Optional**，避免嵌套。 | 链式调用返回 Optional 的方法。 |
| **filter** | `Optional<T> filter(Predicate...)` | 如果值存在且满足条件，返回当前 Optional，否则返回 empty。 | 校验值：`filter(name -> name.length() > 5)` |
| **ifPresent** | `void ifPresent(Consumer...)` | 如果值存在，执行消费逻辑；否则什么都不做。 | 替代 `if (obj != null)` 做侧边效操作。 |

## 3. 代码实战与模式 (Patterns)

### 3.1 替代 null 检查 (传统 vs Optional)

**❌ 传统写法 (嵌套地狱)**
```java
User user = getUser();
if (user != null) {
    Address addr = user.getAddress();
    if (addr != null) {
        String city = addr.getCity();
        if (city != null) {
            System.out.println(city);
        }
    }
}
```

**✅ Optional 写法 (链式调用)**
```java
Optional.ofNullable(getUser())
    .map(User::getAddress)
    .map(Address::getCity)
    .ifPresent(System.out::println);
```

### 3.2 提供默认值

```java
// 场景：获取配置，如果没有则使用默认配置
Config config = Optional.ofNullable(getConfigFromDB())
    .orElseGet(Config::defaultConfig); // 推荐
```

### 3.3 异常处理

```java
// 场景：必须找到用户，找不到则抛业务异常
User user = Optional.ofNullable(findUser(id))
    .orElseThrow(() -> new UserNotFoundException(id));
```

### 3.4 map 与 flatMap 的区别

```java
// 假设 getUser() 返回 Optional<User>
// getCompany() 返回 Optional<Company>

// 使用 map: 结果会是 Optional<Optional<Company>> (嵌套，不推荐)
Optional<Optional<Company>> nested = getUser().map(User::getCompany);

// 使用 flatMap: 结果是 Optional<Company> (扁平化，推荐)
Optional<Company> company = getUser().flatMap(User::getCompany);
```



## 4. 最佳实践与反模式 (Best Practices & Anti-Patterns)

### ✅ Do (推荐做法)
1.  **用于返回值**：方法可能返回空时，返回 `Optional<T>` 而不是 `null`。
2.  **链式调用**：利用 `map`, `flatMap`, `filter` 组合逻辑，避免显式判空。
3.  **使用 `orElseGet`**：当默认值创建成本高时。
4.  **使用 `ifPresent`**：当只需要在有值时执行操作，不关心返回值时。

### ❌ Don't (反模式)
1.  **不要用于字段**：
    *   `private Optional<String> name;` (❌)
    *   原因：Optional 不可序列化，增加内存开销，语义不明。字段应为 `private String name;`。
2.  **不要用于方法参数**：
    *   `public void save(Optional<User> user)` (❌)
    *   原因：调用方负担重，直接传 `User` 或 `null` 更清晰，或者使用重载方法。
3.  **不要调用 `isPresent()` + `get()`**：
    *   这退化为传统的 `if (null)` 检查，失去了 Optional 的函数式意义。
    *   ❌ `if (opt.isPresent()) { return opt.get(); }`
    *   ✅ `return opt.orElse(defaultValue);`
4.  **不要嵌套 Optional**：
    *   `Optional<Optional<T>>` 是设计失误，使用 `flatMap` 解决。



## 5. 常见误区总结 (Cheat Sheet)

| 场景 | 错误用法 | 正确用法 |
| :--- | :--- | :--- |
| **判空取值** | `if(opt.isPresent()) return opt.get();` | `return opt.orElse(default);` |
| **默认值开销大** | `opt.orElse(expensiveMethod())` | `opt.orElseGet(() -> expensiveMethod())` |
| **链式获取属性** | 多层 `if != null` | `opt.map(A::getB).map(B::getC)` |
| **链式返回 Optional** | `opt.map(A::getOptB)` (导致嵌套) | `opt.flatMap(A::getOptB)` |
| **执行副作用** | `if(opt.isPresent()) doSomething(opt.get())` | `opt.ifPresent(this::doSomething)` |
| **集合处理** | `Optional<List<T>>` | 返回空集合 `Collections.emptyList()` 通常更好 |



## 6. 进阶：与 Stream 结合

Optional 常作为 Stream 终端操作的结果出现：

```java
// 查找第一个满足条件的元素
Optional<User> firstAdult = users.stream()
    .filter(u -> u.getAge() >= 18)
    .findFirst(); // 返回 Optional<User>

// 处理结果
firstAdult.ifPresent(u -> sendGift(u));
```



## 7. 总结 (Summary)

1.  **Optional 不是银弹**：它不能消除所有的 NPE，只是将空指针异常转化为更明确的逻辑处理。
2.  **核心用途**：**方法返回值**。告诉调用者“这里可能没值，请处理”。
3.  **函数式风格**：尽量使用 `map`, `flatMap`, `ifPresent` 等链式方法，避免使用 `isPresent` 和 `get` 进行命令式编程。
4.  **性能注意**：区分 `orElse` (急求值) 和 `orElseGet` (懒求值)。
5.  **设计原则**：保持代码可读性。如果 Optional 链过长导致难以阅读，拆分成中间变量或回归传统判空也是可接受的。
