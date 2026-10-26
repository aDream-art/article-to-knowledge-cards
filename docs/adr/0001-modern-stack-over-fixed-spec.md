# 用 Spring Boot 3.5 + Vue 3 替换需求里写死的 Spring Boot 2.x + Vue 2

原始需求把技术栈写成「固定：SpringBoot 2.x + Vue2」。经确认这只是软偏好，于是改用 Spring Boot 3.5.14 + JDK 17（后端）与 Vue 3.5 + Vite + Element Plus + Pinia + TypeScript（前端）。

决定的原因：用户既有的 `mall-master` 项目已经是 Boot 3.5 / JDK 17 / Spring Security 6 / 纯 MyBatis XML + PageHelper 的组合，`CommonResult`、`GlobalExceptionHandler`、`RedisService`、JWT 过滤器可以直接沿用，省 3~5 天摸索；而两条老栈都已 EOL（Vue 2 开源支持止于 2023-12-31，Spring Boot 2.7 OSS 止于 2023-11），留在上面只会让 README 需要为版本写辩护，换不来任何信号。

## Considered Options

- **停在各自产品线终版**（Boot 2.7.18 + JDK 17、Vue 2.7 + Vue CLI）：最贴近原始需求，但 mall-master 的代码全部不可直接复用，且仍需在文档里解释 EOL。
- **维持原栈不动**：与已确认的软约束相悖，属于"跟着老教程走"的信号。

## Consequences

- 全量 `javax.*` → `jakarta.*`；Security 6 的 `SecurityFilterChain` 写法；Springfox 不可用，改用 springdoc 2。
- MyBatis 用 `mybatis-spring-boot-starter` 3.x（MyBatis 3.5.x 兼容 Boot 3），业务代码不变。
- 原始需求文档中"技术栈固定"一行为无效表述，简历与 README 按 Boot 3.5 / Vue 3 描述。
- 不引入 Spring Cloud 的约束保持不变；MySQL + Redis + MyBatis 保持不变。
