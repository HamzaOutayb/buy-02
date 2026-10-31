export interface CartItem {
  productId: string;
  name: string;
  category: string | null;
  sellerId: string;
  imageUrls: string[] | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  items: CartItem[];
  subtotal: number;
}

export interface DeliveryAddress {
  recipientName: string;
  phone: string;
  street: string;
  city: string;
  postalCode: string;
}

export type OrderStatus = 'PLACED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  productId: string;
  productName: string;
  category: string | null;
  sellerId: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  buyerId: string;
  sellerId: string;
  items: OrderItem[];
  deliveryAddress: DeliveryAddress;
  total: number;
  paymentMethod: 'PAY_ON_DELIVERY';
  status: OrderStatus;
  createdAt: string;
}

export interface CheckoutRequest {
  checkoutKey: string;
  deliveryAddress: DeliveryAddress;
}
