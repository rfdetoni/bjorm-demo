package com.github.rfdetoni.bjorm.demo;

import com.github.rfdetoni.bjorm.Bjorm;
import com.github.rfdetoni.bjorm.demo.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Regression: BJORM calls inside Spring @Transactional must roll back together. */
@SpringBootTest
class BjormTransactionTest {
    @Autowired Bjorm db;
    @Autowired TransactionWorker worker;
    @Test void springRollbackUsesBoundConnection() {
        UUID id=UUID.randomUUID();
        assertThrows(IllegalStateException.class,()->worker.failAfterInsert(id));
        assertNull(db.find(Product.class,id),"@Transactional rollback must undo BJORM insert");
    }
    @Test void springCommitPersists() {
        UUID id=UUID.randomUUID();
        worker.commitInsert(id);
        Product saved=db.find(Product.class,id);
        assertNotNull(saved,"@Transactional commit must persist BJORM insert");
        db.delete(saved);
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class TestBeans {
        @Bean TransactionWorker transactionWorker(Bjorm db){return new TransactionWorker(db);}
    }
    public static class TransactionWorker {
        private final Bjorm db;
        public TransactionWorker(Bjorm db){this.db=db;}
        @Transactional
        public void failAfterInsert(UUID id){db.insert(product(id));throw new IllegalStateException("forced rollback");}
        @Transactional
        public void commitInsert(UUID id){db.insert(product(id));}
        private static Product product(UUID id){return new Product(id,"Tx test",null,BigDecimal.ONE,1,true,0);}
    }
}
