# Lambda表达式
## 1. 什么是 Lambda 表达式？
### 1.1 定义
Lambda 表达式是 Java 8 引入的最重要特性之一。它允许将**函数作为参数**传递给方法，或者将代码块视为数据。本质上，它是**匿名方法**的一种简洁表示。

### 1.2 核心优势
*   **减少样板代码**：消除了大量的匿名内部类。
*   **支持函数式编程**：使 Java 能够以声明式风格处理数据（配合 Stream API）。
*   **并行处理**：更容易利用多核处理器（通过 Stream 的 parallel 模式）。

### 1.3 语法结构
```java
(parameters) -> expression
// 或
(parameters) -> { statements; }
```
## 2. 前置知识：函数式接口 (Functional Interface)

Lambda 表达式只能用于**函数式接口**。

### 2.1 定义
*   接口中**有且仅有一个抽象方法**。
*   可以有多个默认方法（`default`）或静态方法（`static`）。
*   建议使用 `@FunctionalInterface` 注解，编译器会检查是否符合规范。

### 2.2 常见的内置函数式接口 (`java.util.function` 包)
这是日常开发中最常用的四个核心接口，建议背诵：

| 接口 | 方法签名 | 描述 | 示例 |
| :--- | :--- | :--- | :--- |
| **Predicate<T>** | `boolean test(T t)` | 断言，接收参数返回布尔值 | `list.stream().filter(s -> s.length() > 5)` |
| **Consumer<T>** | `void accept(T t)` | 消费，接收参数无返回值 | `list.forEach(s -> System.out.println(s))` |
| **Function<T, R>** | `R apply(T t)` | 函数，接收参数返回结果 | `list.stream().map(s -> s.length())` |
| **Supplier<T>** | `T get()` | 供给，无参数返回结果 | `() -> new ArrayList<>()` |

*其他常用接口*: `Comparator<T>` (比较), `Runnable` (无参无返), `Callable<V>` (无参有返)。

## 3. Lambda 语法详解与简化规则

### 3.1 基础对比
**传统匿名内部类：**
```java
new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
}).start();
```

**Lambda 表达式：**
```java
new Thread(() -> System.out.println("Hello")).start();
```

### 3.2 语法简化规则 (糖衣语法)
1.  **类型推断**：参数类型可以省略（编译器自动推断）。
    *   `(String s) -> ...`  =>  `s -> ...`
2.  **单参数省略括号**：如果只有一个参数，圆括号可以省略。
    *   `(s) -> ...`  =>  `s -> ...`
    *   *注意：0 个参数或多参数必须保留括号。*
3.  **单语句省略花括号**：如果方法体只有一条语句，花括号和 `return` 可省略。
    *   `(x, y) -> { return x + y; }`  =>  `(x, y) -> x + y`

### 3.3 代码示例
```java
// 1. 无参数，无返回
Runnable r = () -> System.out.println("No args");

// 2. 一个参数，无返回
Consumer<String> c = s -> System.out.println(s);

// 3. 多个参数，有返回
Comparator<Integer> comp = (a, b) -> Integer.compare(a, b);

// 4. 复杂逻辑，需花括号
Function<String, Integer> func = s -> {
    int len = s.length();
    return len * 2;
};
```

## 4. 方法引用 (Method References)

方法引用是 Lambda 表达式的**进一步简化**。当 Lambda 体仅仅是调用一个已有方法时，使用方法引用更清晰。

### 4.1 语法
使用双冒号 `::` 操作符。
```java
// Lambda
Consumer<String> c = s -> System.out.println(s);
// 方法引用
Consumer<String> c = System.out::println;
```

### 4.2 四种常见形式
1.  **引用静态方法**: `Class::staticMethod`
    *   `(x, y) -> Integer.compare(x, y)`  =>  `Integer::compare`
2.  **引用对象的实例方法**: `instance::method`
    *   `s -> System.out.println(s)`  =>  `System.out::println`
3.  **引用类的实例方法**: `Class::method` (第一个参数作为调用者)
    *   `(s, t) -> s.equals(t)`  =>  `String::equals`
4.  **引用构造器**: `Class::new`
    *   `() -> new ArrayList<>()`  =>  `ArrayList::new`

## 5. 变量作用域与 `this` 关键字

### 5.1 外部变量访问 (Effectively Final)
Lambda 表达式可以访问外部局部变量，但该变量必须是 **final** 或 **有效 final** (即初始化后未被修改)。
```java
int count = 0;
// 错误：count 在 Lambda 中被修改或外部被修改
// list.forEach(s -> { count++; }); 

// 正确：只读访问
list.forEach(s -> System.out.println(count)); 
```
*原因：Lambda 捕获的是变量的副本（闭包），而非变量本身。*

### 5.2 `this` 的含义
*   **匿名内部类**：`this` 指向内部类实例。
*   **Lambda 表达式**：`this` 指向**外部类实例**（包围 Lambda 的类）。
*   *影响*：Lambda 中不能使用 `this` 来引用 Lambda 自身，也无法像内部类那样访问外部类的特定实例上下文（如果存在歧义）。

## 6. 实际应用场景

### 6.1 集合遍历与处理 (配合 Stream)
```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

// 1. 遍历
names.forEach(name -> System.out.println(name));
// 方法引用优化
names.forEach(System.out::println);

// 2. 过滤与转换
List<String> longNames = names.stream()
    .filter(name -> name.length() > 3)      // Predicate
    .map(String::toUpperCase)               // Function
    .collect(Collectors.toList());
```

### 6.2 集合排序
```java
List<Person> people = ...;
// 传统
Collections.sort(people, new Comparator<Person>() {
    public int compare(Person a, Person b) {
        return a.getAge() - b.getAge();
    }
});
// Lambda
people.sort((a, b) -> Integer.compare(a.getAge(), b.getAge()));
// 方法引用 + 比较器构建器
people.sort(Comparator.comparingInt(Person::getAge));
```

### 6.3 线程任务
```java
// 启动线程
new Thread(() -> {
    // 业务逻辑
}).start();

// 提交任务到线程池
executor.submit(() -> doWork());
```

### 6.4 策略模式消除
传统策略模式需要定义接口和多个实现类。使用 Lambda 可以直接传入行为。
```java
// 定义
public void process(Data data, Function<Data, Result> strategy) { ... }

// 调用
process(data, d -> calculateA(d));
process(data, d -> calculateB(d));
```

## 7. 最佳实践与避坑指南

1.  **可读性优先**：如果 Lambda 表达式过于复杂（超过 3 行），建议提取为独立的方法，使用方法引用。
2.  **避免副作用**：在 Stream 操作中，尽量保持无状态，不要在 `forEach` 中修改外部集合（线程安全问题）。
3.  **异常处理**：Lambda 中受检异常（Checked Exception）处理较麻烦，可能需要包装在 try-catch 块中或自定义函数式接口。
4.  **调试困难**：Lambda 在堆栈跟踪中显示为 `lambda$0` 等，调试时不如命名方法直观。
5.  **序列化**：Lambda 对象序列化依赖于编译器实现，不同编译器生成的序列化 ID 可能不同，**不建议将 Lambda 作为字段序列化**。

## 8. 总结 (Cheat Sheet)

| 特性 | 关键点 |
| :--- | :--- |
| **本质** | 匿名函数，函数式接口的实例 |
| **前提** | 目标类型必须是函数式接口 (1 个抽象方法) |
| **语法** | `(args) -> body`，支持类型推断、省略括号/花括号 |
| **核心接口** | `Predicate` (判断), `Consumer` (消费), `Function` (转换), `Supplier` (供给) |
| **方法引用** | `::` 操作符，进一步简化代码 |
| **变量捕获** | 外部局部变量必须是 effectively final |
| **This 指针** | 指向外部类实例，非 Lambda 本身 |
| **最佳搭档** | Stream API, Optional, Date/Time API |
