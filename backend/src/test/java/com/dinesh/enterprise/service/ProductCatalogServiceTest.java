package com.dinesh.enterprise.service;

import com.dinesh.enterprise.dto.common.PageResponse;
import com.dinesh.enterprise.dto.product.ProductRequest;
import com.dinesh.enterprise.dto.product.ProductResponse;
import com.dinesh.enterprise.entity.Category;
import com.dinesh.enterprise.entity.Product;
import com.dinesh.enterprise.enums.ProductStatus;
import com.dinesh.enterprise.exception.DuplicateResourceException;
import com.dinesh.enterprise.exception.ResourceNotFoundException;
import com.dinesh.enterprise.mapper.ProductMapper;
import com.dinesh.enterprise.repository.CategoryRepository;
import com.dinesh.enterprise.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — search and filter tests")
class ProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Category electronics;
    private Product laptop;
    private Product mouse;
    private ProductResponse laptopResponse;
    private ProductResponse mouseResponse;

    @BeforeEach
    void setUp() {
        electronics = Category.builder()
                .id(1L)
                .name("Electronics")
                .active(true)
                .build();

        laptop = Product.builder()
                .id(1L)
                .sku("SKU-LAPTOP-001")
                .name("Enterprise Laptop")
                .price(new BigDecimal("1499.99"))
                .costPrice(new BigDecimal("1100.00"))
                .status(ProductStatus.ACTIVE)
                .category(electronics)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mouse = Product.builder()
                .id(2L)
                .sku("SKU-MOUSE-001")
                .name("Ergonomic Mouse")
                .price(new BigDecimal("49.99"))
                .costPrice(new BigDecimal("25.00"))
                .status(ProductStatus.INACTIVE)
                .category(electronics)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        laptopResponse = ProductResponse.builder()
                .id(1L)
                .sku("SKU-LAPTOP-001")
                .name("Enterprise Laptop")
                .price(new BigDecimal("1499.99"))
                .status(ProductStatus.ACTIVE)
                .categoryId(1L)
                .categoryName("Electronics")
                .build();

        mouseResponse = ProductResponse.builder()
                .id(2L)
                .sku("SKU-MOUSE-001")
                .name("Ergonomic Mouse")
                .price(new BigDecimal("49.99"))
                .status(ProductStatus.INACTIVE)
                .categoryId(1L)
                .categoryName("Electronics")
                .build();
    }

    // -------------------------------------------------------------------------
    // searchProducts — no filters
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts with no filters returns all products paginated")
    void searchProducts_noFilters_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(laptop, mouse), pageable, 2);

        when(productRepository.searchProducts(null, null, null, null, pageable))
                .thenReturn(productPage);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);
        when(productMapper.toResponse(mouse)).thenReturn(mouseResponse);

        PageResponse<ProductResponse> result = productService.searchProducts(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    // -------------------------------------------------------------------------
    // searchProducts — filter by name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts by name returns matching products")
    void searchProducts_filterByName_returnsMatch() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(laptop), pageable, 1);

        when(productRepository.searchProducts(eq("Laptop"), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);

        PageResponse<ProductResponse> result = productService.searchProducts("Laptop", null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Enterprise Laptop");
    }

    @Test
    @DisplayName("searchProducts by name with no match returns empty page, not exception")
    void searchProducts_filterByName_noMatch_returnsEmpty() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productRepository.searchProducts(eq("NonExistent"), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(emptyPage);

        PageResponse<ProductResponse> result = productService.searchProducts("NonExistent", null, null, null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // -------------------------------------------------------------------------
    // searchProducts — filter by SKU
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts by SKU returns matching product")
    void searchProducts_filterBySku_returnsMatch() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(mouse), pageable, 1);

        when(productRepository.searchProducts(isNull(), eq("SKU-MOUSE"), isNull(), isNull(), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.toResponse(mouse)).thenReturn(mouseResponse);

        PageResponse<ProductResponse> result = productService.searchProducts(null, "SKU-MOUSE", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSku()).isEqualTo("SKU-MOUSE-001");
    }

    // -------------------------------------------------------------------------
    // searchProducts — filter by categoryId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts by existing categoryId returns products in that category")
    void searchProducts_filterByCategory_returnsProducts() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(laptop, mouse), pageable, 2);

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.searchProducts(isNull(), isNull(), eq(1L), isNull(), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);
        when(productMapper.toResponse(mouse)).thenReturn(mouseResponse);

        PageResponse<ProductResponse> result = productService.searchProducts(null, null, 1L, null, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("searchProducts with invalid categoryId throws ResourceNotFoundException")
    void searchProducts_invalidCategoryId_throwsNotFound() {
        Pageable pageable = PageRequest.of(0, 20);
        when(categoryRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> productService.searchProducts(null, null, 999L, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(productRepository, never()).searchProducts(any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // searchProducts — filter by status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts by ACTIVE status returns only active products")
    void searchProducts_filterByActiveStatus_returnsActiveOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(laptop), pageable, 1);

        when(productRepository.searchProducts(isNull(), isNull(), isNull(), eq(ProductStatus.ACTIVE), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);

        PageResponse<ProductResponse> result =
                productService.searchProducts(null, null, null, ProductStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("searchProducts by INACTIVE status returns only inactive products")
    void searchProducts_filterByInactiveStatus_returnsInactiveOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(mouse), pageable, 1);

        when(productRepository.searchProducts(isNull(), isNull(), isNull(), eq(ProductStatus.INACTIVE), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.toResponse(mouse)).thenReturn(mouseResponse);

        PageResponse<ProductResponse> result =
                productService.searchProducts(null, null, null, ProductStatus.INACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    // -------------------------------------------------------------------------
    // searchProducts — combined filters
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts with name and categoryId returns correctly filtered results")
    void searchProducts_combinedFilters_returnsCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> productPage = new PageImpl<>(List.of(laptop), pageable, 1);

        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.searchProducts(eq("Laptop"), isNull(), eq(1L), isNull(), eq(pageable)))
                .thenReturn(productPage);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);

        PageResponse<ProductResponse> result =
                productService.searchProducts("Laptop", null, 1L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Enterprise Laptop");
    }

    // -------------------------------------------------------------------------
    // searchProducts — pagination
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts respects pagination metadata")
    void searchProducts_pagination_returnsCorrectMetadata() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Product> productPage = new PageImpl<>(List.of(laptop), pageable, 2);

        when(productRepository.searchProducts(null, null, null, null, pageable))
                .thenReturn(productPage);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);

        PageResponse<ProductResponse> result = productService.searchProducts(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    // -------------------------------------------------------------------------
    // getProductById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProductById with valid ID returns product")
    void getProductById_validId_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(laptop));
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSku()).isEqualTo("SKU-LAPTOP-001");
    }

    @Test
    @DisplayName("getProductById with invalid ID throws ResourceNotFoundException")
    void getProductById_invalidId_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -------------------------------------------------------------------------
    // createProduct — duplicate SKU
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createProduct with duplicate SKU throws DuplicateResourceException")
    void createProduct_duplicateSku_throwsConflict() {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-LAPTOP-001")
                .name("Another Laptop")
                .price(new BigDecimal("999.00"))
                .costPrice(new BigDecimal("700.00"))
                .build();

        when(productRepository.existsBySku("SKU-LAPTOP-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-LAPTOP-001");
    }

    @Test
    @DisplayName("createProduct with invalid categoryId throws ResourceNotFoundException")
    void createProduct_invalidCategoryId_throwsNotFound() {
        ProductRequest request = ProductRequest.builder()
                .sku("SKU-NEW-001")
                .name("New Product")
                .price(new BigDecimal("100.00"))
                .costPrice(new BigDecimal("70.00"))
                .categoryId(999L)
                .build();

        when(productRepository.existsBySku("SKU-NEW-001")).thenReturn(false);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -------------------------------------------------------------------------
    // Sorting — verify pageable is passed through
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("searchProducts passes sort direction to repository correctly")
    void searchProducts_sortByNameDesc_passedToRepository() {
        Pageable descPageable = PageRequest.of(0, 20,
                org.springframework.data.domain.Sort.by("name").descending());
        Page<Product> productPage = new PageImpl<>(List.of(mouse, laptop), descPageable, 2);

        when(productRepository.searchProducts(null, null, null, null, descPageable))
                .thenReturn(productPage);
        when(productMapper.toResponse(mouse)).thenReturn(mouseResponse);
        when(productMapper.toResponse(laptop)).thenReturn(laptopResponse);

        PageResponse<ProductResponse> result =
                productService.searchProducts(null, null, null, null, descPageable);

        assertThat(result.getContent()).hasSize(2);
        // mouse comes first (M > E alphabetically descending)
        assertThat(result.getContent().get(0).getSku()).isEqualTo("SKU-MOUSE-001");
    }
}
