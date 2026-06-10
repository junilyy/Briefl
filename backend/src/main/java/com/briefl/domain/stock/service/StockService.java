package com.briefl.domain.stock.service;

import com.briefl.domain.stock.dto.StockResponse;
import com.briefl.domain.stock.repository.StockRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;

    public List<StockResponse> getStocks() {
        return stockRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(StockResponse::from)
                .toList();
    }
}
