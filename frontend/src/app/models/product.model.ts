export interface Product {
  id: string;
  name: string;
  description: string | null;
  category?: string | null;
  price: number;
  quantity: number;
  sellerId: string;
  imageUrls: string[] | null;
}

export interface ProductInput {
  name: string;
  description: string;
  category?: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}

export interface ProductSearchFilters {
  q?: string;
  category?: string;
  page?: number;
  size?: number;
}

export interface ProductPage {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
