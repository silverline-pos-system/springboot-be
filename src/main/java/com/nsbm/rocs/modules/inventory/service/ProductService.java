package com.nsbm.rocs.modules.inventory.service;

import com.nsbm.rocs.modules.inventory.dto.ProductDTO;
import com.nsbm.rocs.modules.inventory.dto.ProductDetailsDTO;
import com.nsbm.rocs.entity.inventory.Batch;
import com.nsbm.rocs.entity.inventory.Product;
import com.nsbm.rocs.entity.inventory.Supplier;
import com.nsbm.rocs.entity.inventory.SupplierProduct;
import com.nsbm.rocs.common.exception.DuplicateResourceException;
import com.nsbm.rocs.common.exception.ResourceNotFoundException;
import com.nsbm.rocs.modules.inventory.repository.BatchRepository;
import com.nsbm.rocs.modules.inventory.repository.BrandRepository;
import com.nsbm.rocs.modules.inventory.repository.CategoryRepository;
import com.nsbm.rocs.modules.inventory.repository.ProductRepository;
import com.nsbm.rocs.modules.inventory.repository.StockRepository;
import com.nsbm.rocs.modules.inventory.repository.SubCategoryRepository;
import com.nsbm.rocs.modules.inventory.repository.SupplierProductRepository;
import com.nsbm.rocs.modules.inventory.repository.SupplierRepository;
import com.nsbm.rocs.modules.inventory.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final UnitRepository unitRepository;
    private final StockRepository stockRepository;
    private final BatchRepository batchRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierProductRepository supplierProductRepository;

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getActiveProducts() {
        return productRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDTO(product);
    }

    public ProductDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return convertToDTO(product);
    }

    public ProductDTO getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
        return convertToDTO(product);
    }

    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getProductsBySubCategory(Long subCategoryId) {
        return productRepository.findBySubcategoryId(subCategoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getProductsByBrand(Long brandId) {
        return productRepository.findByBrandId(brandId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public String getNextSku() {
        Long maxId = productRepository.getMaxProductId();
        long nextId = (maxId != null ? maxId : 0) + 1;
        return String.format("SKU%03d", nextId);
    }

    public ProductDTO createProduct(ProductDTO productDTO) {
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

    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

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

        // Update fields
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
        product.setIsSerialized(productDTO.getIsSerialized());
        product.setWarrantyMonths(productDTO.getWarrantyMonths());
        product.setIsActive(productDTO.getIsActive());

        Product updatedProduct = productRepository.save(product);
        return convertToDTO(updatedProduct);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

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
                .map(Batch::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ProductDetailsDTO.BatchSummaryDTO> batchDTOs = batches.stream()
                .map(b -> ProductDetailsDTO.BatchSummaryDTO.builder()
                        .batchId(b.getBatchId())
                        .batchCode(b.getBatchCode())
                        .qty(b.getQty())
                        .expiryDate(b.getExpiryDate() != null ? b.getExpiryDate().toString() : null)
                        .costPrice(b.getCostPrice())
                        .sellingPrice(b.getSellingPrice())
                        .branchName("Branch " + b.getBranchId()) // Ideally fetch branch name
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

    private ProductDTO convertToDTO(Product product) {
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
        dto.setWarrantyMonths(product.getWarrantyMonths());
        dto.setIsActive(product.getIsActive());

        // Set display names
        if (product.getCategoryId() != null) {
            categoryRepository.findById(product.getCategoryId())
                    .ifPresent(category -> dto.setCategoryName(category.getName()));
        }
        if (product.getSubcategoryId() != null) {
            subCategoryRepository.findById(product.getSubcategoryId())
                    .ifPresent(subCategory -> dto.setSubcategoryName(subCategory.getName()));
        }
        if (product.getBrandId() != null) {
            brandRepository.findById(product.getBrandId())
                    .ifPresent(brand -> dto.setBrandName(brand.getName()));
        }
        if (product.getUnitId() != null) {
            unitRepository.findById(product.getUnitId())
                    .ifPresent(unit -> {
                        dto.setUnitName(unit.getName());
                        dto.setUnitSymbol(unit.getSymbol());
                    });
        }

        // Set total stock quantity from stock table (aggregated across all branches)
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
        product.setIsSerialized(dto.getIsSerialized());
        product.setWarrantyMonths(dto.getWarrantyMonths());
        product.setIsActive(dto.getIsActive());
        return product;
    }

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }
}


