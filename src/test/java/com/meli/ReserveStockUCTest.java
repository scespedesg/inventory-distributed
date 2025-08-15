package com.meli;

import com.meli.application.usecase.ReserveStockUC;
import com.meli.domain.model.*;
import com.meli.domain.repository.StockRepository;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit test for ReserveStockUC using Mockito.
 */
public class ReserveStockUCTest {

    @Mock
    StockRepository repository;

    @InjectMocks
    ReserveStockUC useCase;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void reservesWhenAvailable() {
        SkuId sku = new SkuId("SKU1");
        StockAggregate agg = new StockAggregate(sku, 10, 0, 0);
        Mockito.when(repository.find(sku)).thenReturn(Uni.createFrom().item(agg));
        Mockito.when(repository.save(any())).thenAnswer(inv ->
                Uni.createFrom().item((StockAggregate) inv.getArgument(0)));

        StockAggregate result = useCase.reserve(sku, 5).await().indefinitely();
        assertEquals(5, result.reserved());
    }
}
