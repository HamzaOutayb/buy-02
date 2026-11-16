package com.product.productService.dto;

import java.util.Arrays;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductReq(
                @NotBlank @Size(max = 500, message = "name must be 500 characters or fewer") String name,
                @NotBlank @Size(max = 500, message = "description must be 500 characters or fewer") String description,
                @NotNull @DecimalMin("0.0") Double price,
                @NotNull @Min(0) Integer quantity,
                @Size(max = 5, message = "imageUrls must contain 5 items or fewer") String[] imageUrls,
                @Size(max = 20, message = "categories must contain 20 items or fewer") String[] categories) {

        public ProductReq(String name, String description, Double price, Integer quantity, String[] imageUrls) {
                this(name, description, price, quantity, imageUrls, new String[0]);
        }

        @Override
        public boolean equals(Object obj) {
                if (this == obj)
                        return true;
                if (!(obj instanceof ProductReq other))
                        return false;

                return java.util.Objects.equals(name, other.name)
                                && java.util.Objects.equals(description, other.description)
                                && java.util.Objects.equals(price, other.price)
                                && java.util.Objects.equals(quantity, other.quantity)
                                && Arrays.equals(imageUrls, other.imageUrls)
                                && Arrays.equals(categories, other.categories);
        }

        @Override
        public int hashCode() {
                int result = java.util.Objects.hash(name, description, price, quantity);
                result = 31 * result + Arrays.hashCode(imageUrls);
                return 31 * result + Arrays.hashCode(categories);
        }

        @Override
        public String toString() {
                return "ProductReq[" +
                                "name=" + name +
                                ", description=" + description +
                                ", price=" + price +
                                ", quantity=" + quantity +
                                ", imageUrls=" + Arrays.toString(imageUrls) +
                                ", categories=" + Arrays.toString(categories) +
                                ']';
        }

}