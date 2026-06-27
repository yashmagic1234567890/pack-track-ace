package com.freshtrack.service;

import com.freshtrack.dto.CreateWarehouseRequest;
import com.freshtrack.dto.WarehouseDto;
import com.freshtrack.entity.Warehouse;
import com.freshtrack.exception.BadRequestException;
import com.freshtrack.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseDto create(CreateWarehouseRequest req) {
        if (warehouseRepository.existsByWarehouseCode(req.warehouseCode())) {
            throw new BadRequestException("Warehouse code already exists: " + req.warehouseCode());
        }
        Warehouse w = warehouseRepository.save(Warehouse.builder()
                .warehouseCode(req.warehouseCode())
                .name(req.name())
                .location(req.location())
                .build());
        return toDto(w);
    }

    @Transactional(readOnly = true)
    public List<WarehouseDto> listAll() {
        return warehouseRepository.findAll().stream().map(this::toDto).toList();
    }

    public WarehouseDto toDto(Warehouse w) {
        return new WarehouseDto(w.getId(), w.getWarehouseCode(), w.getName(), w.getLocation());
    }
}
