# Demo Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working demo that proves the full tech stack: mini-program displays products from Java backend deployed on WeChat Cloud Run.

**Architecture:** Single Spring Boot 3 application with modular packages, MySQL for storage, Redis for cache/session. uni-app Vue3 mini-program as frontend. Vue3+Element Plus as admin panel. All deployed via WeChat Cloud Run (Docker container).

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus 3.5.6, MySQL 8, Redis 7, uni-app (Vue3+TS+Pinia), uView Plus, Vue3+Element Plus+Vite, Docker, WeChat Cloud Run.

---

## File Structure

```
C:\Users\Tim\Documents\GitHub\weChatMiniprogram\codes\
├── shop-backend/                          # Java后端
│   ├── pom.xml                            # 父POM（模块聚合）
│   ├── Dockerfile                         # 云托管部署
│   ├── container.config.json              # 云托管配置
│   ├── shop-framework/                    # 基础框架
│   │   ├── shop-common/                   # 通用工具/常量/异常
│   │   │   ├── pom.xml
│   │   │   └── src/main/java/com/shop/common/
│   │   │       ├── pojo/CommonResult.java
│   │   │       ├── pojo/PageResult.java
│   │   │       ├── pojo/PageParam.java
│   │   │       ├── exception/ServerException.java
│   │   │       ├── exception/ErrorCode.java
│   │   │       ├── exception/GlobalExceptionHandler.java
│   │   │       └── util/JsonUtils.java
│   │   ├── shop-starter-web/              # Web配置
│   │   │   ├── pom.xml
│   │   │   └── src/main/java/com/shop/framework/web/
│   │   │       └── WebAutoConfiguration.java
│   │   ├── shop-starter-mybatis/          # MyBatis配置
│   │   │   ├── pom.xml
│   │   │   └── src/main/java/com/shop/framework/mybatis/
│   │   │       ├── MybatisAutoConfiguration.java
│   │   │       └── core/BaseMapperX.java
│   │   └── shop-starter-security/         # 安全认证
│   │       ├── pom.xml
│   │       └── src/main/java/com/shop/framework/security/
│   │           ├── SecurityAutoConfiguration.java
│   │           ├── TokenService.java
│   │           └── LoginUser.java
│   ├── shop-module-product/               # 商品模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/shop/module/product/
│   │       ├── controller/
│   │       │   ├── admin/ProductCategoryController.java
│   │       │   ├── admin/ProductSpuController.java
│   │       │   └── app/AppProductController.java
│   │       ├── service/
│   │       │   ├── CategoryService.java
│   │       │   └── ProductSpuService.java
│   │       ├── dal/
│   │       │   ├── mysql/CategoryMapper.java
│   │       │   └── mysql/ProductSpuMapper.java
│   │       └── dal/dataobject/
│   │           ├── CategoryDO.java
│   │           └── ProductSpuDO.java
│   ├── shop-module-member/                # 会员模块（Demo仅登录）
│   │   ├── pom.xml
│   │   └── src/main/java/com/shop/module/member/
│   │       ├── controller/app/AppAuthController.java
│   │       ├── service/MemberAuthService.java
│   │       └── dal/
│   │           ├── mysql/MemberUserMapper.java
│   │           └── dataobject/MemberUserDO.java
│   ├── shop-module-system/                # 系统模块（Demo仅管理员登录）
│   │   ├── pom.xml
│   │   └── src/main/java/com/shop/module/system/
│   │       ├── controller/AdminAuthController.java
│   │       └── service/AdminAuthService.java
│   └── shop-server/                       # 启动入口
│       ├── pom.xml
│       └── src/main/
│           ├── java/com/shop/server/ShopServerApplication.java
│           └── resources/
│               ├── application.yml
│               ├── application-dev.yml
│               └── application-prod.yml
├── shop-miniapp/                          # uni-app小程序
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── src/
│   │   ├── main.ts
│   │   ├── App.vue
│   │   ├── pages.json
│   │   ├── manifest.json
│   │   ├── uni.scss
│   │   ├── pages/
│   │   │   └── index/index.vue           # 首页（商品列表）
│   │   ├── api/
│   │   │   ├── request.ts                # 请求封装
│   │   │   └── product.ts               # 商品API
│   │   ├── store/
│   │   │   └── user.ts                  # 用户状态
│   │   └── utils/
│   │       └── auth.ts                  # Token管理
│   └── index.html
├── shop-admin/                            # 管理后台
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   │   ├── main.ts
│   │   ├── App.vue
│   │   ├── router/index.ts
│   │   ├── api/
│   │   │   ├── request.ts               # Axios封装
│   │   │   └── product.ts              # 商品API
│   │   ├── views/
│   │   │   ├── login/index.vue          # 登录页
│   │   │   ├── dashboard/index.vue      # 首页
│   │   │   └── product/
│   │   │       ├── category.vue         # 分类管理
│   │   │       └── spu.vue              # 商品管理
│   │   └── layout/
│   │       └── index.vue                # 布局框架
│   └── index.html
└── sql/
    └── init.sql                           # 建表SQL
```

---

## Task 1: Initialize Git Repository and Parent POM

**Files:**
- Create: `codes/.gitignore`
- Create: `codes/shop-backend/pom.xml`

- [ ] **Step 1: Initialize git repo**

```bash
cd C:\Users\Tim\Documents\GitHub\weChatMiniprogram
git init
```

- [ ] **Step 2: Create .gitignore**

Create file `codes/.gitignore`:

```gitignore
# Java
target/
*.class
*.jar
*.war
.idea/
*.iml

# Node
node_modules/
dist/
.DS_Store

# uni-app
unpackage/

# Environment
.env.local
*.log
```

- [ ] **Step 3: Create parent POM**

Create file `codes/shop-backend/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.shop</groupId>
    <artifactId>shop-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>shop-backend</name>
    <description>药食同源微信小程序商城</description>

    <modules>
        <module>shop-framework</module>
        <module>shop-module-product</module>
        <module>shop-module-member</module>
        <module>shop-module-system</module>
        <module>shop-server</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>3.2.5</spring-boot.version>
        <mybatis-plus.version>3.5.6</mybatis-plus.version>
        <hutool.version>5.8.27</hutool.version>
        <jwt.version>0.12.5</jwt.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <!-- Internal modules -->
            <dependency>
                <groupId>com.shop</groupId>
                <artifactId>shop-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.shop</groupId>
                <artifactId>shop-starter-web</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.shop</groupId>
                <artifactId>shop-starter-mybatis</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.shop</groupId>
                <artifactId>shop-starter-security</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 4: Commit**

```bash
git add codes/.gitignore codes/shop-backend/pom.xml
git commit -m "init: project skeleton with parent POM"
```

---

## Task 2: Framework Layer — shop-common

**Files:**
- Create: `codes/shop-backend/shop-framework/pom.xml`
- Create: `codes/shop-backend/shop-framework/shop-common/pom.xml`
- Create: `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/pojo/CommonResult.java`
- Create: `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/pojo/PageResult.java`
- Create: `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/pojo/PageParam.java`
- Create: `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/exception/ErrorCode.java`
- Create: `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/exception/ServerException.java`
- Create: `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create framework aggregator POM**

Create `codes/shop-backend/shop-framework/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-framework</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>shop-common</module>
        <module>shop-starter-web</module>
        <module>shop-starter-mybatis</module>
        <module>shop-starter-security</module>
    </modules>
</project>
```

- [ ] **Step 2: Create shop-common POM**

Create `codes/shop-backend/shop-framework/shop-common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-framework</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-common</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create CommonResult (unified API response)**

Create `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/pojo/CommonResult.java`:

```java
package com.shop.common.pojo;

import com.shop.common.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class CommonResult<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }
}
```

- [ ] **Step 4: Create PageParam and PageResult**

Create `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/pojo/PageParam.java`:

```java
package com.shop.common.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageParam implements Serializable {
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

Create `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/pojo/PageResult.java`:

```java
package com.shop.common.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private List<T> list;
    private Long total;

    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }
}
```

- [ ] **Step 5: Create exception classes**

Create `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/exception/ErrorCode.java`:

```java
package com.shop.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统异常"),

    // Business errors 1001+
    USER_NOT_EXISTS(1001, "用户不存在"),
    TOKEN_EXPIRED(1002, "Token已过期"),
    PRODUCT_NOT_EXISTS(1101, "商品不存在"),
    PRODUCT_OFF_SHELF(1102, "商品已下架");

    private final Integer code;
    private final String msg;
}
```

Create `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/exception/ServerException.java`:

```java
package com.shop.common.exception;

import lombok.Getter;

@Getter
public class ServerException extends RuntimeException {

    private final Integer code;

    public ServerException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public ServerException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

Create `codes/shop-backend/shop-framework/shop-common/src/main/java/com/shop/common/exception/GlobalExceptionHandler.java`:

```java
package com.shop.common.exception;

import com.shop.common.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServerException.class)
    public CommonResult<?> handleServerException(ServerException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return CommonResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e) {
        log.error("系统异常", e);
        return CommonResult.error(ErrorCode.INTERNAL_ERROR);
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add codes/shop-backend/shop-framework/
git commit -m "feat: add shop-common module (CommonResult, PageResult, Exception)"
```

---

## Task 3: Framework Layer — shop-starter-mybatis

**Files:**
- Create: `codes/shop-backend/shop-framework/shop-starter-mybatis/pom.xml`
- Create: `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/java/com/shop/framework/mybatis/MybatisAutoConfiguration.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/java/com/shop/framework/mybatis/core/BaseMapperX.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/java/com/shop/framework/mybatis/core/BaseDO.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Create POM**

Create `codes/shop-backend/shop-framework/shop-starter-mybatis/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-framework</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-starter-mybatis</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create BaseDO**

Create `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/java/com/shop/framework/mybatis/core/BaseDO.java`:

```java
package com.shop.framework.mybatis.core;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class BaseDO implements Serializable {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
```

- [ ] **Step 3: Create BaseMapperX**

Create `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/java/com/shop/framework/mybatis/core/BaseMapperX.java`:

```java
package com.shop.framework.mybatis.core;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;

import java.util.List;

public interface BaseMapperX<T> extends BaseMapper<T> {

    default PageResult<T> selectPage(PageParam pageParam, com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<T> wrapper) {
        IPage<T> page = selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
}
```

- [ ] **Step 4: Create MybatisAutoConfiguration**

Create `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/java/com/shop/framework/mybatis/MybatisAutoConfiguration.java`:

```java
package com.shop.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisAutoConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

- [ ] **Step 5: Create auto-configuration registration**

Create `codes/shop-backend/shop-framework/shop-starter-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.shop.framework.mybatis.MybatisAutoConfiguration
```

- [ ] **Step 6: Commit**

```bash
git add codes/shop-backend/shop-framework/shop-starter-mybatis/
git commit -m "feat: add shop-starter-mybatis (pagination, auto-fill, BaseDO)"
```

---

## Task 4: Framework Layer — shop-starter-security (Simplified JWT)

**Files:**
- Create: `codes/shop-backend/shop-framework/shop-starter-security/pom.xml`
- Create: `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/LoginUser.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/TokenService.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/SecurityAutoConfiguration.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/TokenAuthenticationFilter.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Create POM**

Create `codes/shop-backend/shop-framework/shop-starter-security/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-framework</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-starter-security</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create LoginUser and TokenService**

Create `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/LoginUser.java`:

```java
package com.shop.framework.security;

import lombok.Data;

@Data
public class LoginUser {
    private Long userId;
    private Integer userType; // 1=会员 2=管理员
}
```

Create `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/TokenService.java`:

```java
package com.shop.framework.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_PREFIX = "shop:token:";
    private static final long TOKEN_EXPIRE_HOURS = 24 * 7; // 7 days

    private final StringRedisTemplate redisTemplate;

    public String createToken(LoginUser loginUser) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        String value = loginUser.getUserId() + ":" + loginUser.getUserType();
        redisTemplate.opsForValue().set(key, value, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        return token;
    }

    public LoginUser getLoginUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(":");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(Long.parseLong(parts[0]));
        loginUser.setUserType(Integer.parseInt(parts[1]));
        return loginUser;
    }

    public void deleteToken(String token) {
        redisTemplate.delete(TOKEN_PREFIX + token);
    }
}
```

- [ ] **Step 3: Create TokenAuthenticationFilter**

Create `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/TokenAuthenticationFilter.java`:

```java
package com.shop.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            LoginUser loginUser = tokenService.getLoginUser(token);
            if (loginUser != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 4: Create SecurityAutoConfiguration**

Create `codes/shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/SecurityAutoConfiguration.java`:

```java
package com.shop.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.pojo.CommonResult;
import com.shop.common.exception.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenService tokenService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/app-api/member/auth/**").permitAll()
                .requestMatchers("/admin-api/system/auth/**").permitAll()
                .requestMatchers("/app-api/product/**").permitAll()
                // All other endpoints require auth
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        new ObjectMapper().writeValueAsString(CommonResult.error(ErrorCode.UNAUTHORIZED))
                    );
                })
            )
            .addFilterBefore(new TokenAuthenticationFilter(tokenService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 5: Create auto-configuration registration**

Create `codes/shop-backend/shop-framework/shop-starter-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.shop.framework.security.SecurityAutoConfiguration
```

- [ ] **Step 6: Commit**

```bash
git add codes/shop-backend/shop-framework/shop-starter-security/
git commit -m "feat: add shop-starter-security (JWT Token + Redis + Spring Security)"
```

---

## Task 5: Framework Layer — shop-starter-web

**Files:**
- Create: `codes/shop-backend/shop-framework/shop-starter-web/pom.xml`
- Create: `codes/shop-backend/shop-framework/shop-starter-web/src/main/java/com/shop/framework/web/WebAutoConfiguration.java`
- Create: `codes/shop-backend/shop-framework/shop-starter-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Create POM**

Create `codes/shop-backend/shop-framework/shop-starter-web/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-framework</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-starter-web</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create WebAutoConfiguration**

Create `codes/shop-backend/shop-framework/shop-starter-web/src/main/java/com/shop/framework/web/WebAutoConfiguration.java`:

```java
package com.shop.framework.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 3: Create auto-configuration registration**

Create `codes/shop-backend/shop-framework/shop-starter-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.shop.framework.web.WebAutoConfiguration
```

- [ ] **Step 4: Commit**

```bash
git add codes/shop-backend/shop-framework/shop-starter-web/
git commit -m "feat: add shop-starter-web (CORS, validation)"
```

---

## Task 6: Database Schema (Demo Core Tables)

**Files:**
- Create: `codes/sql/init.sql`

- [ ] **Step 1: Create init.sql with demo tables**

Create `codes/sql/init.sql`:

```sql
-- ============================================
-- 药食同源小程序商城 - 初始化SQL (Demo阶段)
-- ============================================

CREATE DATABASE IF NOT EXISTS `shop` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `shop`;

-- ============ 会员相关 ============

CREATE TABLE `member_user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `openid` varchar(64) DEFAULT NULL COMMENT '微信openid',
    `unionid` varchar(64) DEFAULT NULL COMMENT '微信unionid',
    `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
    `nickname` varchar(64) DEFAULT '' COMMENT '昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像URL',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=正常 0=禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_mobile` (`mobile`)
) ENGINE=InnoDB COMMENT='会员用户表';

-- ============ 商品相关 ============

CREATE TABLE `product_category` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类ID(0=一级)',
    `name` varchar(64) NOT NULL COMMENT '分类名称',
    `icon` varchar(512) DEFAULT '' COMMENT '图标URL',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越大越前)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='商品分类表';

CREATE TABLE `product_spu` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `name` varchar(128) NOT NULL COMMENT '商品名称',
    `keyword` varchar(256) DEFAULT '' COMMENT '关键词(搜索用)',
    `introduction` varchar(256) DEFAULT '' COMMENT '简介',
    `description` text COMMENT '详情(富文本)',
    `pic_url` varchar(512) NOT NULL COMMENT '主图URL',
    `slider_pic_urls` varchar(2048) DEFAULT '[]' COMMENT '轮播图JSON数组',
    `video_url` varchar(512) DEFAULT '' COMMENT '视频URL',
    `type` tinyint NOT NULL DEFAULT 1 COMMENT '类型 1=实物 2=虚拟(课程)',
    `price` int NOT NULL COMMENT '最低价格(分)',
    `market_price` int DEFAULT NULL COMMENT '划线价(分)',
    `stock` int NOT NULL DEFAULT 0 COMMENT '总库存',
    `sales_count` int NOT NULL DEFAULT 0 COMMENT '实际销量',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0=下架 1=上架',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='商品SPU表';

CREATE TABLE `product_sku` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `properties` varchar(512) DEFAULT '[]' COMMENT '属性JSON [{id,name,valueId,valueName}]',
    `price` int NOT NULL COMMENT '价格(分)',
    `market_price` int DEFAULT NULL COMMENT '划线价(分)',
    `stock` int NOT NULL DEFAULT 0 COMMENT '库存',
    `pic_url` varchar(512) DEFAULT '' COMMENT 'SKU图片',
    `weight` double DEFAULT NULL COMMENT '重量(kg)',
    `volume` double DEFAULT NULL COMMENT '体积(m3)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB COMMENT='商品SKU表';

-- ============ 系统相关 ============

CREATE TABLE `sys_admin_user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `username` varchar(64) NOT NULL COMMENT '用户名',
    `password` varchar(128) NOT NULL COMMENT '密码(BCrypt)',
    `nickname` varchar(64) DEFAULT '' COMMENT '昵称',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=正常 0=禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='管理员用户表';

-- 插入默认管理员 (密码: admin123)
INSERT INTO `sys_admin_user` (`username`, `password`, `nickname`)
VALUES ('admin', '$2a$10$YMpimV4T/3Cq.UoFqMFJ6eOPHoGRTnr9X8tJLBXvBL7Uh3LQFX6G', '超级管理员');

-- 插入Demo分类
INSERT INTO `product_category` (`name`, `icon`, `sort`, `status`) VALUES
('农副产品', '', 100, 1),
('保健品', '', 90, 1),
('课程研学', '', 80, 1),
('组合套装', '', 70, 1);

-- ============ 内容相关 ============

CREATE TABLE `content_banner` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `title` varchar(128) NOT NULL COMMENT '标题',
    `pic_url` varchar(512) NOT NULL COMMENT '图片URL',
    `url` varchar(512) DEFAULT '' COMMENT '跳转链接',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='轮播图表';
```

- [ ] **Step 2: Commit**

```bash
git add codes/sql/
git commit -m "feat: add database init SQL (member, product, admin, banner)"
```

---

## Task 7: Product Module (Backend CRUD)

**Files:**
- Create: `codes/shop-backend/shop-module-product/pom.xml`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/dataobject/CategoryDO.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/dataobject/ProductSpuDO.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/mysql/CategoryMapper.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/mysql/ProductSpuMapper.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/service/CategoryService.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/service/ProductSpuService.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/controller/app/AppProductController.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/controller/admin/AdminProductController.java`
- Create: `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/controller/admin/AdminCategoryController.java`

- [ ] **Step 1: Create module POM**

Create `codes/shop-backend/shop-module-product/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-module-product</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-mybatis</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create DO classes**

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/dataobject/CategoryDO.java`:

```java
package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_category")
public class CategoryDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
}
```

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/dataobject/ProductSpuDO.java`:

```java
package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_spu")
public class ProductSpuDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private String keyword;
    private String introduction;
    private String description;
    private String picUrl;
    private String sliderPicUrls;
    private String videoUrl;
    private Integer type; // 1=实物 2=虚拟
    private Integer price; // 单位：分
    private Integer marketPrice;
    private Integer stock;
    private Integer salesCount;
    private Integer sort;
    private Integer status; // 0=下架 1=上架
}
```

- [ ] **Step 3: Create Mappers**

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/mysql/CategoryMapper.java`:

```java
package com.shop.module.product.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.product.dal.dataobject.CategoryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapperX<CategoryDO> {
}
```

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/mysql/ProductSpuMapper.java`:

```java
package com.shop.module.product.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSpuMapper extends BaseMapperX<ProductSpuDO> {
}
```

- [ ] **Step 4: Create Services**

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/service/CategoryService.java`:

```java
package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<CategoryDO> getEnabledList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getStatus, 1)
                .orderByDesc(CategoryDO::getSort));
    }

    public List<CategoryDO> getAllList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                .orderByDesc(CategoryDO::getSort));
    }

    public void create(CategoryDO category) {
        categoryMapper.insert(category);
    }

    public void update(CategoryDO category) {
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }
}
```

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/service/ProductSpuService.java`:

```java
package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ErrorCode;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSpuService {

    private final ProductSpuMapper productSpuMapper;

    public PageResult<ProductSpuDO> getSpuPage(PageParam pageParam, Long categoryId) {
        LambdaQueryWrapper<ProductSpuDO> wrapper = new LambdaQueryWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getStatus, 1)
                .eq(categoryId != null, ProductSpuDO::getCategoryId, categoryId)
                .orderByDesc(ProductSpuDO::getSort);
        return productSpuMapper.selectPage(pageParam, wrapper);
    }

    public ProductSpuDO getSpuDetail(Long id) {
        ProductSpuDO spu = productSpuMapper.selectById(id);
        if (spu == null) {
            throw new ServerException(ErrorCode.PRODUCT_NOT_EXISTS);
        }
        return spu;
    }

    public PageResult<ProductSpuDO> getAdminSpuPage(PageParam pageParam) {
        LambdaQueryWrapper<ProductSpuDO> wrapper = new LambdaQueryWrapper<ProductSpuDO>()
                .orderByDesc(ProductSpuDO::getCreateTime);
        return productSpuMapper.selectPage(pageParam, wrapper);
    }

    public void createSpu(ProductSpuDO spu) {
        productSpuMapper.insert(spu);
    }

    public void updateSpu(ProductSpuDO spu) {
        productSpuMapper.updateById(spu);
    }

    public void deleteSpu(Long id) {
        productSpuMapper.deleteById(id);
    }
}
```

- [ ] **Step 5: Create Controllers**

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/controller/app/AppProductController.java`:

```java
package com.shop.module.product.controller.app;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.service.CategoryService;
import com.shop.module.product.service.ProductSpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app-api/product")
@RequiredArgsConstructor
public class AppProductController {

    private final CategoryService categoryService;
    private final ProductSpuService productSpuService;

    @GetMapping("/category/list")
    public CommonResult<List<CategoryDO>> getCategoryList() {
        return CommonResult.success(categoryService.getEnabledList());
    }

    @GetMapping("/spu/page")
    public CommonResult<PageResult<ProductSpuDO>> getSpuPage(PageParam pageParam,
                                                              @RequestParam(required = false) Long categoryId) {
        return CommonResult.success(productSpuService.getSpuPage(pageParam, categoryId));
    }

    @GetMapping("/spu/detail")
    public CommonResult<ProductSpuDO> getSpuDetail(@RequestParam Long id) {
        return CommonResult.success(productSpuService.getSpuDetail(id));
    }
}
```

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/controller/admin/AdminCategoryController.java`:

```java
package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin-api/product/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    public CommonResult<List<CategoryDO>> list() {
        return CommonResult.success(categoryService.getAllList());
    }

    @PostMapping("/create")
    public CommonResult<Boolean> create(@RequestBody CategoryDO category) {
        categoryService.create(category);
        return CommonResult.success(true);
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody CategoryDO category) {
        categoryService.update(category);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        categoryService.delete(id);
        return CommonResult.success(true);
    }
}
```

Create `codes/shop-backend/shop-module-product/src/main/java/com/shop/module/product/controller/admin/AdminProductController.java`:

```java
package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.service.ProductSpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/product/spu")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductSpuService productSpuService;

    @GetMapping("/page")
    public CommonResult<PageResult<ProductSpuDO>> page(PageParam pageParam) {
        return CommonResult.success(productSpuService.getAdminSpuPage(pageParam));
    }

    @PostMapping("/create")
    public CommonResult<Boolean> create(@RequestBody ProductSpuDO spu) {
        productSpuService.createSpu(spu);
        return CommonResult.success(true);
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody ProductSpuDO spu) {
        productSpuService.updateSpu(spu);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        productSpuService.deleteSpu(id);
        return CommonResult.success(true);
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add codes/shop-backend/shop-module-product/
git commit -m "feat: add product module (category + SPU CRUD, app + admin APIs)"
```

---

## Task 8: Server Entry Point + Application Config

**Files:**
- Create: `codes/shop-backend/shop-module-member/pom.xml`
- Create: `codes/shop-backend/shop-module-system/pom.xml`
- Create: `codes/shop-backend/shop-server/pom.xml`
- Create: `codes/shop-backend/shop-server/src/main/java/com/shop/server/ShopServerApplication.java`
- Create: `codes/shop-backend/shop-server/src/main/resources/application.yml`
- Create: `codes/shop-backend/shop-server/src/main/resources/application-dev.yml`
- Create: `codes/shop-backend/Dockerfile`
- Create: `codes/shop-backend/container.config.json`

- [ ] **Step 1: Create member module POM (placeholder)**

Create `codes/shop-backend/shop-module-member/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-module-member</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-mybatis</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create system module POM (placeholder)**

Create `codes/shop-backend/shop-module-system/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-module-system</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-mybatis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create shop-server POM (startup module)**

Create `codes/shop-backend/shop-server/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.shop</groupId>
        <artifactId>shop-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>shop-server</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-module-product</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-module-member</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.shop</groupId>
            <artifactId>shop-module-system</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create Application entry point**

Create `codes/shop-backend/shop-server/src/main/java/com/shop/server/ShopServerApplication.java`:

```java
package com.shop.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.shop")
@MapperScan("com.shop.module.*.dal.mysql")
public class ShopServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopServerApplication.class, args);
    }
}
```

- [ ] **Step 5: Create application config files**

Create `codes/shop-backend/shop-server/src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: dev

server:
  port: 80
```

Create `codes/shop-backend/shop-server/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    com.shop: debug
```

- [ ] **Step 6: Create Dockerfile**

Create `codes/shop-backend/Dockerfile`:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY shop-framework/ shop-framework/
COPY shop-module-product/ shop-module-product/
COPY shop-module-member/ shop-module-member/
COPY shop-module-system/ shop-module-system/
COPY shop-server/ shop-server/
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/shop-server/target/*.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

Create `codes/shop-backend/container.config.json`:

```json
{
  "containerPort": 80,
  "dockerfilePath": "Dockerfile",
  "buildDir": "",
  "minNum": 1,
  "maxNum": 3,
  "cpu": 0.5,
  "mem": 1,
  "policyType": "cpu",
  "policyThreshold": 60,
  "initialDelaySeconds": 30
}
```

- [ ] **Step 7: Verify backend compiles**

```bash
cd C:\Users\Tim\Documents\GitHub\weChatMiniprogram\codes\shop-backend
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add codes/shop-backend/shop-module-member/ codes/shop-backend/shop-module-system/ codes/shop-backend/shop-server/ codes/shop-backend/Dockerfile codes/shop-backend/container.config.json
git commit -m "feat: add server entry point, application config, Dockerfile"
```

---

## Task 9: Mini-program Skeleton (uni-app)

**Files:**
- Create: `codes/shop-miniapp/package.json`
- Create: `codes/shop-miniapp/vite.config.ts`
- Create: `codes/shop-miniapp/tsconfig.json`
- Create: `codes/shop-miniapp/src/main.ts`
- Create: `codes/shop-miniapp/src/App.vue`
- Create: `codes/shop-miniapp/src/pages.json`
- Create: `codes/shop-miniapp/src/manifest.json`
- Create: `codes/shop-miniapp/src/uni.scss`
- Create: `codes/shop-miniapp/src/api/request.ts`
- Create: `codes/shop-miniapp/src/api/product.ts`
- Create: `codes/shop-miniapp/src/pages/index/index.vue`
- Create: `codes/shop-miniapp/index.html`

- [ ] **Step 1: Create package.json**

Create `codes/shop-miniapp/package.json`:

```json
{
  "name": "shop-miniapp",
  "version": "1.0.0",
  "scripts": {
    "dev:mp-weixin": "uni -p mp-weixin",
    "build:mp-weixin": "uni build -p mp-weixin"
  },
  "dependencies": {
    "@dcloudio/uni-app": "3.0.0-4010120250128001",
    "@dcloudio/uni-app-plus": "3.0.0-4010120250128001",
    "@dcloudio/uni-components": "3.0.0-4010120250128001",
    "@dcloudio/uni-h5": "3.0.0-4010120250128001",
    "@dcloudio/uni-mp-weixin": "3.0.0-4010120250128001",
    "vue": "^3.4.21",
    "pinia": "^2.1.7"
  },
  "devDependencies": {
    "@dcloudio/types": "^3.4.8",
    "@dcloudio/uni-automator": "3.0.0-4010120250128001",
    "@dcloudio/uni-cli-shared": "3.0.0-4010120250128001",
    "@dcloudio/uni-stacktracey": "3.0.0-4010120250128001",
    "@dcloudio/vite-plugin-uni": "3.0.0-4010120250128001",
    "typescript": "^5.4.5",
    "vite": "^5.2.8"
  }
}
```

- [ ] **Step 2: Create vite.config.ts**

Create `codes/shop-miniapp/vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
})
```

- [ ] **Step 3: Create tsconfig.json**

Create `codes/shop-miniapp/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ESNext",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "jsx": "preserve",
    "sourceMap": true,
    "resolveJsonModule": true,
    "esModuleInterop": true,
    "lib": ["ESNext", "DOM"],
    "types": ["@dcloudio/types"],
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.vue"]
}
```

- [ ] **Step 4: Create main.ts and App.vue**

Create `codes/shop-miniapp/src/main.ts`:

```typescript
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  return { app }
}
```

Create `codes/shop-miniapp/src/App.vue`:

```vue
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
})
</script>

<style>
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
</style>
```

- [ ] **Step 5: Create pages.json and manifest.json**

Create `codes/shop-miniapp/src/pages.json`:

```json
{
  "pages": [
    {
      "path": "pages/index/index",
      "style": {
        "navigationBarTitleText": "首页"
      }
    }
  ],
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "药食同源商城",
    "navigationBarBackgroundColor": "#ffffff",
    "backgroundColor": "#f5f5f5"
  }
}
```

Create `codes/shop-miniapp/src/manifest.json`:

```json
{
  "name": "药食同源商城",
  "appid": "",
  "description": "药食同源微信小程序商城",
  "versionName": "1.0.0",
  "versionCode": "100",
  "mp-weixin": {
    "appid": "",
    "setting": {
      "urlCheck": false
    },
    "usingComponents": true
  }
}
```

- [ ] **Step 6: Create request wrapper and product API**

Create `codes/shop-miniapp/src/api/request.ts`:

```typescript
interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
}

interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

const BASE_URL = 'http://localhost:80' // Dev环境，后续改为云托管地址

export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token') || ''
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'content-type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header,
      },
      success: (res) => {
        const data = res.data as ApiResponse<T>
        if (data.code === 0) {
          resolve(data.data)
        } else if (data.code === 401) {
          uni.removeStorageSync('token')
          uni.showToast({ title: '请先登录', icon: 'none' })
          reject(data)
        } else {
          uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      },
    })
  })
}
```

Create `codes/shop-miniapp/src/api/product.ts`:

```typescript
import { request } from './request'

export interface Category {
  id: number
  parentId: number
  name: string
  icon: string
  sort: number
}

export interface ProductSpu {
  id: number
  categoryId: number
  name: string
  introduction: string
  picUrl: string
  price: number
  marketPrice: number
  salesCount: number
  status: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export function getCategoryList() {
  return request<Category[]>({ url: '/app-api/product/category/list' })
}

export function getProductPage(params: { pageNo: number; pageSize: number; categoryId?: number }) {
  return request<PageResult<ProductSpu>>({
    url: '/app-api/product/spu/page',
    data: params,
  })
}

export function getProductDetail(id: number) {
  return request<ProductSpu>({ url: '/app-api/product/spu/detail', data: { id } })
}
```

- [ ] **Step 7: Create index page (product list)**

Create `codes/shop-miniapp/src/pages/index/index.vue`:

```vue
<template>
  <view class="container">
    <!-- Categories -->
    <scroll-view scroll-x class="category-bar">
      <view
        v-for="cat in categories"
        :key="cat.id"
        class="category-item"
        :class="{ active: currentCategoryId === cat.id }"
        @tap="selectCategory(cat.id)"
      >
        {{ cat.name }}
      </view>
    </scroll-view>

    <!-- Product List -->
    <view class="product-grid">
      <view v-for="item in products" :key="item.id" class="product-card">
        <image :src="item.picUrl" mode="aspectFill" class="product-img" />
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <text class="product-price">¥{{ (item.price / 100).toFixed(2) }}</text>
        </view>
      </view>
    </view>

    <view v-if="products.length === 0" class="empty">
      <text>暂无商品</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategoryList, getProductPage, type Category, type ProductSpu } from '@/api/product'

const categories = ref<Category[]>([])
const products = ref<ProductSpu[]>([])
const currentCategoryId = ref<number | undefined>(undefined)

async function loadCategories() {
  categories.value = await getCategoryList()
}

async function loadProducts() {
  const res = await getProductPage({
    pageNo: 1,
    pageSize: 20,
    categoryId: currentCategoryId.value,
  })
  products.value = res.list
}

function selectCategory(id: number) {
  currentCategoryId.value = currentCategoryId.value === id ? undefined : id
  loadProducts()
}

onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<style scoped>
.container {
  padding: 20rpx;
}
.category-bar {
  white-space: nowrap;
  margin-bottom: 20rpx;
}
.category-item {
  display: inline-block;
  padding: 12rpx 30rpx;
  margin-right: 16rpx;
  background: #fff;
  border-radius: 30rpx;
  font-size: 26rpx;
}
.category-item.active {
  background: #07c160;
  color: #fff;
}
.product-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.product-card {
  width: calc(50% - 8rpx);
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.product-img {
  width: 100%;
  height: 340rpx;
}
.product-info {
  padding: 16rpx;
}
.product-name {
  font-size: 26rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-price {
  font-size: 30rpx;
  color: #e64340;
  font-weight: bold;
  margin-top: 8rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
}
</style>
```

- [ ] **Step 8: Create index.html and uni.scss**

Create `codes/shop-miniapp/index.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>药食同源商城</title>
  <!--preload-links-->
  <!--app-context-->
</head>
<body>
  <div id="app"><!--app-html--></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

Create `codes/shop-miniapp/src/uni.scss`:

```scss
$primary-color: #07c160;
$price-color: #e64340;
$text-color: #333;
$text-secondary: #999;
$bg-color: #f5f5f5;
```

- [ ] **Step 9: Commit**

```bash
git add codes/shop-miniapp/
git commit -m "feat: add uni-app mini-program skeleton (product list page)"
```

---

## Task 10: Verify End-to-End Demo

- [ ] **Step 1: Start MySQL and Redis locally (or use Docker)**

```bash
docker run -d --name shop-mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8.0
docker run -d --name shop-redis -p 6379:6379 redis:7-alpine
```

- [ ] **Step 2: Initialize database**

```bash
mysql -h 127.0.0.1 -u root -proot < C:/Users/Tim/Documents/GitHub/weChatMiniprogram/codes/sql/init.sql
```

- [ ] **Step 3: Start Spring Boot backend**

```bash
cd C:\Users\Tim\Documents\GitHub\weChatMiniprogram\codes\shop-backend
mvn spring-boot:run -pl shop-server
```

Expected: Application started on port 80

- [ ] **Step 4: Test product API**

```bash
curl http://localhost/app-api/product/category/list
```

Expected:
```json
{"code":0,"msg":"success","data":[{"id":1,"name":"农副产品",...},{"id":2,"name":"保健品",...},...]}
```

- [ ] **Step 5: Start mini-program dev**

```bash
cd C:\Users\Tim\Documents\GitHub\weChatMiniprogram\codes\shop-miniapp
npm install
npm run dev:mp-weixin
```

Open `dist/dev/mp-weixin` in WeChat Developer Tools. Should show category bar + empty product list (no products yet).

- [ ] **Step 6: Insert a test product and verify display**

```bash
curl -X POST http://localhost/admin-api/product/spu/create \
  -H "Content-Type: application/json" \
  -d '{"categoryId":1,"name":"宁夏枸杞 500g","introduction":"头茬大果粒","picUrl":"https://via.placeholder.com/400","price":5900,"marketPrice":9900,"stock":100,"status":1}'
```

Refresh mini-program — should display the product card.

- [ ] **Step 7: Final commit**

```bash
git add -A
git commit -m "feat: demo foundation complete - end-to-end product display working"
```

---

## Demo Milestone Verification

After completing all tasks, verify:

| Check | Expected |
|-------|----------|
| `mvn clean package -DskipTests` | BUILD SUCCESS |
| Backend starts on port 80 | No errors in log |
| `GET /app-api/product/category/list` | Returns 4 categories |
| `POST /admin-api/product/spu/create` | Creates product |
| `GET /app-api/product/spu/page` | Returns product list |
| Mini-program displays categories | Category bar renders |
| Mini-program displays products | Product cards render |
| Clicking category filters products | List updates |

---

## Next Plans

After this demo is working:
- **Plan 2:** Member auth (WeChat login) + Admin auth + Admin panel skeleton
- **Plan 3:** Cart + Order + WeChat Payment
- **Plan 4:** Course module (CRUD + video playback)
- **Plan 5:** Member subscription (paid monthly/yearly)
- **Plan 6:** Promotions (coupons + full reduction)
- **Plan 7:** Full mini-program pages + polish
- **Plan 8:** Cloud deployment + submit for review
