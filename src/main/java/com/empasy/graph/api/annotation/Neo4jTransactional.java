package com.empasy.graph.api.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Neo4j(GDB) 전용 트랜잭션을 위한 커스텀 어노테이션입니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional("neo4jTransactionManager")
public @interface Neo4jTransactional {

    @AliasFor(annotation = Transactional.class)
    String value() default "neo4jTransactionManager";

    @AliasFor(annotation = Transactional.class)
    String transactionManager() default "neo4jTransactionManager";

    @AliasFor(annotation = Transactional.class)
    boolean readOnly() default false;
}
