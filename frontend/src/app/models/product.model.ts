export interface Product {
  id: string;
  name: string;
  description: string | null;
  price: number;
  quantity: number;
  sellerId: string;
  imageUrls: string[] | null;
}

export interface ProductInput {
  name: string;
  description: string;
  price: number;
  quantity: number;
  imageUrls: string[];
}
