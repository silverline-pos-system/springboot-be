package com.silverline.erp.module.inventory.service.impl;

import com.silverline.erp.common.exception.DuplicateResourceException;
import com.silverline.erp.common.exception.ResourceNotFoundException;
import com.silverline.erp.common.exception.ValidationException;
import com.silverline.erp.domain.inventory.Batch;
import com.silverline.erp.domain.procurement.Supplier;
import com.silverline.erp.domain.procurement.SupplierProduct;
import com.silverline.erp.domain.product.Product;
import com.silverline.erp.domain.product.TrackingType;
import com.silverline.erp.module.inventory.dto.ProductDTO;
import com.silverline.erp.module.inventory.dto.ProductDetailsDTO;
import com.silverline.erp.module.inventory.repository.*;
import com.silverline.erp.module.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;
    private final StockRepository stockRepository;
    private final BatchRepository batchRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;

    private Pageable capPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int cappedSize = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), cappedSize, pageable.getSort());
    }

    @Override
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return mapPageToDTO(productRepository.findAll(capped));
    }

    @Override
    public Page<ProductDTO> getActiveProducts(Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return mapPageToDTO(productRepository.findByIsActiveTrue(capped));
    }

    @Cacheable(value = "products", key = "#id")
    @Override
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    @Cacheable(value = "products", key = "#sku")
    @Override
    public ProductDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return convertToDTO(product);
    }

    @Cacheable(value = "products", key = "#barcode")
    @Override
    public ProductDTO getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
        return convertToDTO(product);
    }

    @Override
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return mapPageToDTO(productRepository.findByCategoryId(categoryId, capped));
    }

    @Override
    public Page<ProductDTO> getProductsBySubCategory(Long subCategoryId, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return mapPageToDTO(productRepository.findBySubcategoryId(subCategoryId, capped));
    }

    @Override
    public Page<ProductDTO> getProductsByBrand(Long brandId, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return mapPageToDTO(productRepository.findByBrandId(brandId, capped));
    }

    @Override
    public Page<ProductDTO> searchProducts(String keyword, Pageable pageable) {
        Pageable capped = capPageable(pageable);
        return mapPageToDTO(productRepository.searchProducts(keyword, capped));
    }

    @Override
    public String getNextSku() {
        Long maxId = productRepository.getMaxProductId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        return String.format("SKU%03d", nextId);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        if (productDTO.getTrackingType() == null || productDTO.getTrackingType().trim().isEmpty()) {
            throw new ValidationException("Inventory Tracking Type is a mandatory field");
        }
        String tt = productDTO.getTrackingType().trim().toUpperCase();
        if (!tt.equals("NORMAL") && !tt.equals("IMEI") && !tt.equals("EXPIRY")) {
            throw new ValidationException("Inventory Tracking Type must be one of: NORMAL, IMEI, EXPIRY");
        }

        if (productRepository.findBySku(productDTO.getSku()).isPresent()) {
            throw new DuplicateResourceException("Product with SKU " + productDTO.getSku() + " already exists");
        }
        if (productDTO.getBarcode() != null && !productDTO.getBarcode().isEmpty()) {
            if (productRepository.findByBarcode(productDTO.getBarcode()).isPresent()) {
                throw new DuplicateResourceException("Product with barcode " + productDTO.getBarcode() + " already exists");
            }
        }
        Product product = convertToEntity(productDTO);
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (productDTO.getTrackingType() == null || productDTO.getTrackingType().trim().isEmpty()) {
            throw new ValidationException("Inventory Tracking Type is a mandatory field");
        }
        String tt = productDTO.getTrackingType().trim().toUpperCase();
        if (!tt.equals("NORMAL") && !tt.equals("IMEI") && !tt.equals("EXPIRY")) {
            throw new ValidationException("Inventory Tracking Type must be one of: NORMAL, IMEI, EXPIRY");
        }

        // Check if user tries to change the tracking type
        if (product.getTrackingType() != null && !product.getTrackingType().name().equalsIgnoreCase(tt)) {
            boolean isUsed = productRepository.isUsedInPurchaseOrders(id) || 
                             productRepository.isUsedInDispatches(id) || 
                             productRepository.isUsedInSales(id);
            if (isUsed) {
                throw new ValidationException("Cannot change inventory tracking type once the product has been used in transactions.");
            }
        }

        if (!product.getSku().equals(productDTO.getSku())) {
            if (productRepository.findBySku(productDTO.getSku()).isPresent()) {
                throw new DuplicateResourceException("Product with SKU " + productDTO.getSku() + " already exists");
            }
        }

        if (productDTO.getBarcode() != null && !productDTO.getBarcode().isEmpty()) {
            if (!productDTO.getBarcode().equals(product.getBarcode())) {
                if (productRepository.findByBarcode(productDTO.getBarcode()).isPresent()) {
                    throw new DuplicateResourceException("Product with barcode " + productDTO.getBarcode() + " already exists");
                }
            }
        }

        product.setSku(productDTO.getSku());
        product.setBarcode(productDTO.getBarcode());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setCategoryId(productDTO.getCategoryId());
        product.setSubcategoryId(productDTO.getSubcategoryId());
        product.setBrandId(productDTO.getBrandId());
        product.setUnitId(productDTO.getUnitId());
        product.setCostPrice(productDTO.getCostPrice());
        product.setSellingPrice(productDTO.getSellingPrice());
        product.setMrp(productDTO.getMrp());
        product.setReorderLevel(productDTO.getReorderLevel());
        product.setMaxStockLevel(productDTO.getMaxStockLevel());
        
        try {
            TrackingType enumTt = TrackingType.valueOf(tt);
            product.setTrackingType(enumTt);
            product.setIsSerialized(enumTt == TrackingType.IMEI);
        } catch (IllegalArgumentException e) {
            product.setTrackingType(TrackingType.NORMAL);
            product.setIsSerialized(false);
        }

        product.setWarrantyMonths(productDTO.getWarrantyMonths());
        product.setIsActive(productDTO.getIsActive());

        Product updatedProduct = productRepository.save(product);
        return convertToDTO(updatedProduct);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Override
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    public ProductDetailsDTO getProductDetails(Long productId) {
        Product product = findProductById(productId);

        String categoryName = product.getCategoryId() != null ?
                categoryRepository.findById(product.getCategoryId()).map(c -> c.getName()).orElse(null) : null;
        String subCategoryName = product.getSubcategoryId() != null ?
                subCategoryRepository.findById(product.getSubcategoryId()).map(s -> s.getName()).orElse(null) : null;
        String brandName = product.getBrandId() != null ?
                brandRepository.findById(product.getBrandId()).map(b -> b.getName()).orElse(null) : null;
        String unitName = product.getUnitId() != null ?
                unitRepository.findById(product.getUnitId()).map(u -> u.getName()).orElse(null) : null;

        List<Batch> batches = batchRepository.findByProductId(productId);
        BigDecimal totalStock = batches.stream()
                .map(b -> b.getQty())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        List<ProductDetailsDTO.BatchSummaryDTO> batchDTOs = batches.stream()
                .map(b -> ProductDetailsDTO.BatchSummaryDTO.builder()
                        .batchId(b.getBatchId())
                        .batchCode(b.getBatchCode())
                        .qty(b.getQty())
                        .expiryDate(b.getExpiryDate() != null ? b.getExpiryDate().toString() : null)
                        .costPrice(b.getCostPrice())
                        .sellingPrice(b.getSellingPrice())
                        .branchName("Branch " + b.getBranchId())
                        .build())
                .collect(Collectors.toList());

        List<SupplierProduct> supplierProducts = supplierProductRepository.findByProductId(productId);
        List<ProductDetailsDTO.SupplierSummaryDTO> supplierDTOs = supplierProducts.stream()
                .map(sp -> {
                    Supplier s = supplierRepository.findById(sp.getSupplierId()).orElse(null);
                    if (s == null) return null;
                    return ProductDetailsDTO.SupplierSummaryDTO.builder()
                            .supplierId(s.getSupplierId())
                            .name(s.getCompanyName())
                            .contactPerson(s.getContactPerson())
                            .phone(s.getPhone())
                            .email(s.getEmail())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return ProductDetailsDTO.builder()
                .product(product)
                .categoryName(categoryName)
                .subCategoryName(subCategoryName)
                .brandName(brandName)
                .unitName(unitName)
                .totalStock(totalStock)
                .batches(batchDTOs)
                .suppliers(supplierDTOs)
                .build();
    }

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public List<Product> findByBranch(Long branchId) {
        return stockRepository.findByBranchId(branchId).stream()
                .map(stock -> productRepository.findById(stock.getProductId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Product findByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode).orElse(null);
    }

    private Page<ProductDTO> mapPageToDTO(Page<Product> products) {
        if (products.isEmpty()) {
            return Page.empty(products.getPageable());
        }
        List<Long> productIds = products.getContent().stream()
                .map(p -> p.getProductId())
                .collect(Collectors.toList());

        List<Object[]> stockData = stockRepository.getTotalStockByProductIds(productIds);
        Map<Long, BigDecimal> stockMap = stockData.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
                        (a, b) -> a
                ));

        return products.map(product -> convertToDTO(product, stockMap));
    }

    private ProductDTO convertToDTOBasic(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setSku(product.getSku());
        dto.setBarcode(product.getBarcode());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategoryId());
        dto.setSubcategoryId(product.getSubcategoryId());
        dto.setBrandId(product.getBrandId());
        dto.setUnitId(product.getUnitId());
        dto.setCostPrice(product.getCostPrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setMrp(product.getMrp());
        dto.setReorderLevel(product.getReorderLevel());
        dto.setMaxStockLevel(product.getMaxStockLevel());
        dto.setIsSerialized(product.getIsSerialized());
        dto.setTrackingType(product.getTrackingType() != null ? product.getTrackingType().name() : "NORMAL");
        dto.setWarrantyMonths(product.getWarrantyMonths());
        dto.setIsActive(product.getIsActive());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getSubCategory() != null) {
            dto.setSubcategoryName(product.getSubCategory().getName());
        }
        if (product.getBrand() != null) {
            dto.setBrandName(product.getBrand().getName());
        }
        if (product.getUnit() != null) {
            dto.setUnitName(product.getUnit().getName());
            dto.setUnitSymbol(product.getUnit().getSymbol());
        }
        return dto;
    }

    private ProductDTO convertToDTO(Product product, Map<Long, BigDecimal> stockMap) {
        ProductDTO dto = convertToDTOBasic(product);
        BigDecimal totalStock = stockMap != null ? stockMap.get(product.getProductId()) : null;
        dto.setQuantity(totalStock != null ? totalStock : BigDecimal.ZERO);
        return dto;
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = convertToDTOBasic(product);
        BigDecimal totalStock = stockRepository.getTotalStockByProduct(product.getProductId());
        dto.setQuantity(totalStock != null ? totalStock : BigDecimal.ZERO);
        return dto;
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setProductId(dto.getProductId());
        product.setSku(dto.getSku());
        product.setBarcode(dto.getBarcode());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategoryId(dto.getCategoryId());
        product.setSubcategoryId(dto.getSubcategoryId());
        product.setBrandId(dto.getBrandId());
        product.setUnitId(dto.getUnitId());
        product.setCostPrice(dto.getCostPrice());
        product.setSellingPrice(dto.getSellingPrice());
        product.setMrp(dto.getMrp());
        product.setReorderLevel(dto.getReorderLevel());
        product.setMaxStockLevel(dto.getMaxStockLevel());
        
        if (dto.getTrackingType() != null) {
            try {
                TrackingType tt = TrackingType.valueOf(dto.getTrackingType().trim().toUpperCase());
                product.setTrackingType(tt);
                product.setIsSerialized(tt == TrackingType.IMEI);
            } catch (IllegalArgumentException e) {
                product.setTrackingType(TrackingType.NORMAL);
                product.setIsSerialized(false);
            }
        } else {
            product.setTrackingType(TrackingType.NORMAL);
            product.setIsSerialized(false);
        }

        product.setWarrantyMonths(dto.getWarrantyMonths());
        product.setIsActive(dto.getIsActive());
        return product;
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    @Override
    public List<Product> findProductsByIds(List<Long> ids) {
        return productRepository.findAllById(ids);
    }

    @Override
    public Product findBySku(String sku) {
        return productRepository.findBySku(sku).orElse(null);
    }

    @Override
    public List<Product> searchProductEntities(String keyword) {
        return productRepository.searchProducts(keyword);
    }

    @Override
    public List<Product> getActiveProductsLimit(int limit) {
        return productRepository.findByIsActiveTrue(org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }
}
