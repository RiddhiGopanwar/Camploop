package com.camploop.service;

import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.Report;
import com.camploop.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ProductService productService;

    @Autowired
    public ReportService(ReportRepository reportRepository, ProductService productService) {
        this.reportRepository = reportRepository;
        this.productService = productService;
    }

    public Report create(Long productId, String reason, Profile reporter) {
        Product product = productService.getById(productId);
        Report report = new Report();
        report.setProduct(product);
        report.setReporter(reporter);
        report.setReason(reason);
        return reportRepository.save(report);
    }
}
