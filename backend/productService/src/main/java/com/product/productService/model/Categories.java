package com.product.productService.model;

import java.util.Arrays;
import java.util.Locale;

public enum Categories {
	ELECTRONICS,
	CLOTHING,
	SHOES,
	HOME,
	FURNITURE,
	BEAUTY,
	SPORTS,
	OUTDOORS,
	TOYS,
	BOOKS,
	MUSIC,
	ART,
	JEWELRY,
	AUTOMOTIVE,
	PETS,
	GROCERY,
	HEALTH,
	OFFICE,
	GARDEN,
	TRAVEL;

	public static boolean isValid(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}

		String normalized = value.trim().toUpperCase(Locale.ROOT);
		return Arrays.stream(values()).anyMatch(category -> category.name().equals(normalized));
	}

	public static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}