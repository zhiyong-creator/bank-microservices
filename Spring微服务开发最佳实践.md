# Spring 微服务开发最佳实践

> 本文档基于本课程的 `accounts` / `cards` / `loans` 微服务项目，结合实际代码讲解六项关键最佳实践，
> 供同学们在开发微服务时参考遵循。

---

## 目录

1. [JPA 审计（Audit）](#1-jpa-审计audit)
2. [集中异常处理](#2-集中异常处理)
3. [用 ResponseEntity 统一包装返回值](#3-用-responseentity-统一包装返回值)
4. [用 DTO 在层间传输数据](#4-用-dto-在层间传输数据)
5. [用 Swagger / OpenAPI 文档化 API](#5-用-swagger--openapi-文档化-api)
6. [API Endpoint 的暴露方式](#6-api-endpoint-的暴露方式)

---

## 1. JPA 审计（Audit）

### 为什么需要审计？

在企业级系统中，每条数据的**创建时间、创建人、最后修改时间、最后修改人**都是必须追踪的信息。
如果在每个 Service 方法里手动设置这些字段，代码会大量重复且容易遗漏。
Spring Data JPA 的审计功能可以**自动填充**这四个字段。

### 实现步骤

**Step 1：定义 `BaseEntity` — 所有实体的父类**

```java
// accounts/entity/BaseEntity.java
@MappedSuperclass                               // ① 声明为"映射超类"，本身不建表
@EntityListeners(AuditingEntityListener.class)  // ② 注册 JPA 审计监听器
@Getter @Setter @ToString
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)       // 创建后不允许更新
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(insertable = false)      // 首次插入时不写入，只在更新时写入
    private LocalDateTime updateAt;

    @LastModifiedBy
    @Column(insertable = false)
    private String updatedBy;
}
```

**Step 2：实现 `AuditorAware` — 告诉 JPA 当前操作者是谁**

```java
// accounts/audit/AuditAwareImpl.java
@Component("auditAwareImpl")
public class AuditAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // 生产环境：从 SecurityContext 读取当前登录用户名
        // 课程阶段：用微服务名称作为操作者标识
        return Optional.of("ACCOUNTS_MS");
    }
}
```

**Step 3：在启动类开启审计**

```java
// accounts/AccountsApplication.java
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")  // 指向上面的 Bean
public class AccountsApplication { ... }
```

**Step 4：业务实体继承 `BaseEntity`**

```java
// accounts/entity/Customer.java
@Entity
public class Customer extends BaseEntity {  // 继承即可，四个审计字段自动生效
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;
    private String name;
    // ...
}
```

### 关键要点

| 注解 | 作用 |
|------|------|
| `@MappedSuperclass` | 父类字段映射到子类表，父类本身不建表 |
| `@EntityListeners` | 挂载 JPA 审计事件监听器 |
| `@CreatedDate` / `@LastModifiedDate` | 自动填充时间戳 |
| `@CreatedBy` / `@LastModifiedBy` | 自动填充操作人（来自 `AuditorAware`） |
| `@EnableJpaAuditing` | 在启动类开启整个审计机制 |

> ⚠️ **常见错误**：忘记在启动类加 `@EnableJpaAuditing`，审计字段将永远为 `null`。
> 本项目在 Service 层中已将手动设置审计字段的旧代码注释掉（如 `// customer.setCreatedAt(...)`），
> 这正是引入 JPA 审计后应有的做法——**让框架自动处理，不要手动设置**。

---

## 2. 集中异常处理

### 反模式：分散式异常处理

```java
// ❌ 错误做法：在每个 Controller 方法里写 try-catch，代码膨胀且错误格式不一致
@GetMapping("/fetch")
public ResponseEntity<?> fetchAccount(String mobileNumber) {
    try {
        ...
    } catch (ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage()); // 格式随意
    }
}
```

### 最佳实践：`@ControllerAdvice` + 全局异常处理器

**Step 1：定义业务异常类（语义清晰，自带 HTTP 状态码）**

```java
// accounts/exception/ResourceNotFoundException.java
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with the given input data %s: '%s'",
                resourceName, fieldName, fieldValue));
    }
}

// accounts/exception/CustomerAlreadyExistsException.java
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException(String message) { super(message); }
}
```

**Step 3：全局异常处理器 — 用 `@ControllerAdvice` 统一拦截**

```java
// accounts/exception/GlobalExceptionHandler.java
@ControllerAdvice  // ← 核心注解：拦截所有 Controller 抛出的异常
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 处理参数校验失败（@Valid 触发的 Bean Validation 错误）
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->
            validationErrors.put(((FieldError) error).getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    // 处理"客户已存在"业务异常
    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomerAlreadyExistsException(
            CustomerAlreadyExistsException ex, WebRequest request) {
        return new ResponseEntity<>(new ErrorResponseDto(request.getDescription(false),
                HttpStatus.BAD_REQUEST, ex.getMessage(), LocalDateTime.now()), HttpStatus.BAD_REQUEST);
    }

    // 处理"资源未找到"业务异常
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(new ErrorResponseDto(request.getDescription(false),
                HttpStatus.NOT_FOUND, ex.getMessage(), LocalDateTime.now()), HttpStatus.NOT_FOUND);
    }

    // 兜底：处理所有未预期的异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception ex, WebRequest request) {
        return new ResponseEntity<>(new ErrorResponseDto(request.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), LocalDateTime.now()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### 核心优势

- ✅ **Controller 保持干净**：业务方法不写任何 try-catch，职责单一
- ✅ **错误格式统一**：所有错误响应均为 `ErrorResponseDto`，前端可靠解析
- ✅ **易于扩展**：新增异常类型只需在 `GlobalExceptionHandler` 中加一个带 `@ExceptionHandler` 的方法
- ✅ **语义化状态码**：`404 Not Found`、`400 Bad Request`、`500 Internal Server Error` 各司其职

---

## 3. 用 ResponseEntity 统一包装返回值

### 为什么不能直接返回对象？

直接返回 POJO 时，HTTP 状态码永远是 `200 OK`，调用方无法通过状态码区分"创建成功"和"查询成功"等不同语义。`ResponseEntity<T>` 是 Spring MVC 提供的完整 HTTP 响应封装，包含：**状态码 + 响应头 + 响应体**。

### 定义成功响应 DTO 与常量

```java
// accounts/dto/ResponseDto.java
@Data
public class ResponseDto {
    private String statusCode;   // 如 "200"、"201"
    private String statusMsg;    // 如 "Account created successfully"

    public ResponseDto(String statusCode, String statusMsg) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
    }
}

// accounts/constants/AccountsConstants.java（集中管理，避免魔法字符串）
public class AccountsConstants {
    private AccountsConstants() {}  // 禁止实例化
    public static final String STATUS_201 = "201";
    public static final String MESSAGE_201 = "Account created successfully";
    public static final String STATUS_200 = "200";
    public static final String MESSAGE_200 = "Request processed successfully";
    public static final String STATUS_417 = "417";
    public static final String MESSAGE_417_UPDATE = "Update operation failed. Please try again or contact Dev team";
    public static final String MESSAGE_417_DELETE = "Delete operation failed. Please try again or contact Dev team";
}
```

### Controller 中的标准写法

```java
// accounts/controller/AccountsController.java

// ① 创建资源 → 201 Created
@PostMapping("/create")
public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody CustomerDto customerDto) {
    iAccountsService.createAccount(customerDto);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ResponseDto(AccountsConstants.STATUS_201, AccountsConstants.MESSAGE_201));
}

// ② 查询资源 → 200 OK，Body 是业务数据
@GetMapping("/fetch")
public ResponseEntity<CustomerDto> fetchAccountDetails(@RequestParam String mobileNumber) {
    return ResponseEntity.status(HttpStatus.OK).body(iAccountsService.fetchAccount(mobileNumber));
}

// ③ 更新资源 → 200 / 417，根据业务结果决定
@PutMapping("/update")
public ResponseEntity<ResponseDto> updateAccountDetails(@Valid @RequestBody CustomerDto customerDto) {
    boolean isUpdated = iAccountsService.updateAccount(customerDto);
    if (isUpdated) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto(AccountsConstants.STATUS_200, AccountsConstants.MESSAGE_200));
    } else {
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                .body(new ResponseDto(AccountsConstants.STATUS_417, AccountsConstants.MESSAGE_417_UPDATE));
    }
}

// ④ 删除资源 → 200 / 417
@DeleteMapping("/delete")
public ResponseEntity<ResponseDto> deleteAccountDetails(@RequestParam String mobileNumber) {
    boolean isDeleted = iAccountsService.deleteAccount(mobileNumber);
    return isDeleted
        ? ResponseEntity.status(HttpStatus.OK).body(new ResponseDto(AccountsConstants.STATUS_200, AccountsConstants.MESSAGE_200))
        : ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseDto(AccountsConstants.STATUS_417, AccountsConstants.MESSAGE_417_DELETE));
}
```

### HTTP 状态码使用规范

| 场景 | 状态码 | Spring 常量 |
|------|--------|-------------|
| 创建资源成功 | 201 | `HttpStatus.CREATED` |
| 查询/更新/删除成功 | 200 | `HttpStatus.OK` |
| 参数校验失败 | 400 | `HttpStatus.BAD_REQUEST` |
| 资源不存在 | 404 | `HttpStatus.NOT_FOUND` |
| 业务操作未完成 | 417 | `HttpStatus.EXPECTATION_FAILED` |
| 服务器内部错误 | 500 | `HttpStatus.INTERNAL_SERVER_ERROR` |

---

## 4. 用 DTO 在层间传输数据

### 为什么不能直接暴露 Entity？

| 问题 | 说明 |
|------|------|
| **安全风险** | Entity 可能含有密码、内部主键等敏感字段，直接暴露极不安全 |
| **过度耦合** | API 字段随数据库 Schema 变动而变动，无法单独演进 |
| **序列化陷阱** | JPA 懒加载关联对象在序列化时可能触发额外 SQL 或抛异常 |
| **校验污染** | 数据库约束和 API 入参校验混在一起，难以维护 |

### 数据流向

```
HTTP 请求/响应
      ↕  (DTO)
  Controller 层   ← 只接触 DTO，负责参数校验
      ↕  (DTO)
  Service 层      ← 业务逻辑 + DTO ↔ Entity 转换
      ↕  (Entity)
  Repository 层   ← 只接触 Entity，与数据库交互
```

**DTO 示例（含参数校验注解）**

```java
// accounts/dto/CustomerDto.java
@Data
public class CustomerDto {
    @NotEmpty(message = "Name cannot be null or empty.")
    @Size(min = 5, max = 30)
    private String name;

    @NotEmpty @Email(message = "Email address should be a valid value")
    private String email;

    @Pattern(regexp = "(^$|[0-9]{10})", message = "mobile number must be 10 digit")
    private String mobileNumber;

    private AccountsDto accountsDto;  // ← 嵌套 DTO，而非嵌套 Entity
}
```

**Mapper — 负责 DTO ↔ Entity 转换**

```java
// accounts/mapper/CustomerMapper.java
public class CustomerMapper {

    // Entity → DTO（查询时）
    public static CustomerDto mapToCustomerDto(Customer customer, CustomerDto customerDto) {
        customerDto.setName(customer.getName());
        customerDto.setEmail(customer.getEmail());
        customerDto.setMobileNumber(customer.getMobileNumber());
        return customerDto;
    }

    // DTO → Entity（保存/更新时）
    public static Customer mapToCustomer(CustomerDto customerDto, Customer customer) {
        customer.setName(customerDto.getName());
        customer.setEmail(customerDto.getEmail());
        customer.setMobileNumber(customerDto.getMobileNumber());
        return customer;
    }
}
```

**Service 层 — 转换示例**

```java
// accounts/service/impl/AccountsService.java
public CustomerDto fetchAccount(String mobileNumber) {
    // 1. 用 Entity 在 Repository 层查询
    Customer customer = customerRepo.findByMobileNumber(mobileNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
    Accounts accounts = accountsRepo.findByCustomerId(customer.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Account", "customerId",
                    customer.getCustomerId().toString()));

    // 2. 转为 DTO 后返回（Entity 不越过 Service 层边界）
    CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
    customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));
    return customerDto;
}
```

### 设计要点

- **每个 DTO 只包含该场景真正需要的字段**，不要直接复用 Entity
- **嵌套关系用嵌套 DTO** 表达（如 `CustomerDto` 内嵌 `AccountsDto`），不嵌套 Entity
- **Mapper 保持纯静态工具方法**，便于测试；大型项目可引入 MapStruct 自动生成
- **校验注解（`@NotEmpty`、`@Pattern` 等）写在 DTO 上**，Entity 上只保留数据库约束

---

## 5. 用 Swagger / OpenAPI 文档化 API

### 依赖引入（pom.xml）

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### 三个层面的文档注解

**层面一：全局 API 信息 — 启动类**

```java
// accounts/AccountsApplication.java
@OpenAPIDefinition(
    info = @Info(
        title = "Accounts 微服务 REST API 文档", version = "V1",
        contact = @Contact(name = "老李", email = "laoli@yiqixue.org", url = "http://www.yiqixue.org"),
        license = @License(name = "Apache 2.0", url = "http://www.yiqixue.org")
    ),
    externalDocs = @ExternalDocumentation(description = "银行账号系统微服务文档", url = "http://www.yiqixue.org")
)
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
public class AccountsApplication { ... }
```

**层面二：Controller — Tag + Operation + ApiResponse**

```java
// accounts/controller/AccountsController.java
@Tag(name = "Accounts CRUD REST APIs", description = "创建、查询、更新、删除账号的接口")
@RestController
public class AccountsController {

    @Operation(summary = "创建账号", description = "为新客户创建银行账号")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "账号创建成功"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误",
            content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody CustomerDto customerDto) { ... }
}
```

**层面三：DTO — Schema + 示例值**

```java
// accounts/dto/CustomerDto.java
@Schema(name = "Customer", description = "存储客户与账户数据的 Schema")
public class CustomerDto {
    @Schema(description = "客户姓名，长度 5-30 个字符", example = "扎西德勒")
    private String name;

    @Schema(description = "客户邮箱地址", example = "student@yiqixue.org")
    private String email;

    @Schema(description = "客户手机号（10位数字）", example = "9345432123")
    private String mobileNumber;
}
```

### 访问 Swagger UI

| 服务 | 地址 |
|------|------|
| accounts | `http://localhost:8080/swagger-ui/index.html` |
| cards | `http://localhost:9000/swagger-ui/index.html` |
| loans | `http://localhost:8090/swagger-ui/index.html` |

原始 OpenAPI JSON 规范（可导入 Postman）：`http://localhost:{port}/v3/api-docs`

### 文档化检查清单

- ✅ 每个 Controller 加 `@Tag`（分组）
- ✅ 每个接口加 `@Operation`（summary + description）
- ✅ 每个接口列出**所有可能**的 `@ApiResponse`，包括错误响应
- ✅ 错误响应关联 `ErrorResponseDto`：`@Content(schema = @Schema(implementation = ErrorResponseDto.class))`
- ✅ 每个 DTO 类加 `@Schema(name, description)`
- ✅ 每个 DTO 字段加 `@Schema(description, example)` 并提供有意义的示例值

---

## 6. API Endpoint 的暴露方式

### Controller 级别配置

```java
@RestController
@RequestMapping(
    path = "/api",                                  // ① 统一前缀
    produces = {MediaType.APPLICATION_JSON_VALUE}   // ② 类级别声明只产出 JSON
)
@AllArgsConstructor  // ③ Lombok 生成全参构造，配合构造器注入（推荐方式）
@Validated           // ④ 开启方法级参数校验（@RequestParam 上的 @Pattern 需要它）
public class AccountsController {
    private IAccountsService iAccountsService;  // ⑤ 依赖接口，而非实现类
    ...
}
```

### RESTful URL 设计规范

| 操作 | HTTP 方法 | URL | 请求体 |
|------|-----------|-----|--------|
| 创建账号 | `POST` | `/api/create` | Body: `CustomerDto` |
| 查询账号 | `GET` | `/api/fetch?mobileNumber=xxx` | 无 |
| 更新账号 | `PUT` | `/api/update` | Body: `CustomerDto` |
| 删除账号 | `DELETE` | `/api/delete?mobileNumber=xxx` | 无 |

> **命名原则**：URL 使用名词，HTTP 方法表达动作；避免在 URL 里出现动词（如 `/getAccount`、`/createAccount`）。

### 参数校验

校验应在进入 Service 层之前完成，Controller 是最佳位置：

```java
// RequestBody 校验：@Valid 触发 DTO 上的所有 Bean Validation 注解
@PostMapping("/create")
public ResponseEntity<ResponseDto> createAccount(@Valid @RequestBody CustomerDto customerDto) { ... }

// RequestParam 校验：直接在参数上写约束注解（需 Controller 类上有 @Validated）
@GetMapping("/fetch")
public ResponseEntity<CustomerDto> fetchAccountDetails(
        @RequestParam
        @Pattern(regexp = "$|[0-9]{10}", message = "手机号必须是10位长数字")
        String mobileNumber) { ... }
```

### Actuator 端点管理

```yaml
# accounts/cards/loans 的 application.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"       # 开发阶段：全部暴露；生产：改为 "health,info"
  endpoint:
    shutdown:
      enabled: true        # 允许 POST /actuator/shutdown 优雅停机
  info:
    env:
      enabled: true        # 允许 /actuator/info 展示自定义应用信息
```

> ⚠️ 生产环境务必将 `include: "*"` 改为只暴露必要端点，并配合 Spring Security 保护这些端点。

### Eureka 客户端注册配置

```yaml
# accounts/cards/loans 的 application.yml
eureka:
  instance:
    preferIpAddress: true        # 用 IP 注册，而非主机名，容器环境下更可靠
  client:
    fetchRegistry: true          # 从 Eureka Server 拉取服务列表（服务发现）
    registerWithEureka: true     # 将自身注册到 Eureka Server
    service-url:
      defaultZone: http://localhost:8070/eureka/
```

加入 `spring-cloud-starter-netflix-eureka-client` 依赖 + 以上配置，Spring Boot 自动完成注册，**无需额外注解**。

---

## 总结

| 最佳实践 | 核心价值 | 本项目实现位置 |
|----------|----------|---------------|
| JPA 审计 | 自动追踪数据变更，代码零重复 | `BaseEntity` + `AuditAwareImpl` |
| 集中异常处理 | 统一错误格式，Controller 代码干净 | `GlobalExceptionHandler` |
| ResponseEntity | 语义化 HTTP 状态码，规范 API 契约 | 所有 Controller 方法 |
| DTO 模式 | 安全隔离，API 稳定，校验集中 | `dto/` 包 + `mapper/` 包 |
| Swagger/OpenAPI | 文档与代码同步，团队协作高效 | 启动类 + Controller + DTO 注解 |
| Endpoint 暴露规范 | RESTful 语义 + 输入安全 + 运维可观测 | Controller 注解 + `application.yml` |

> 这六项实践相互配合，共同构成一个**生产就绪（Production-Ready）微服务**的基础骨架。
> 建议同学们在每个新微服务的初始阶段就将这些实践落地，而非后期补救。


