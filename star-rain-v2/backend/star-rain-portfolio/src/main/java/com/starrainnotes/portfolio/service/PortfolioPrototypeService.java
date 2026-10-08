package com.starrainnotes.portfolio.service;
import com.starrainnotes.portfolio.dto.WorkPrototypeDTO;
public interface PortfolioPrototypeService {
    void bind(Long workId, WorkPrototypeDTO request);
    byte[] read(String slug, String path);
    String contentType(String path);
}
