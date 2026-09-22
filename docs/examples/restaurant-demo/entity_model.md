# RestoPro — Entity Model

The first vertical slice needs only the entities required to take and track a dine-in order.

```mermaid
erDiagram
    RESTAURANT ||--o{ OUTLET : contains
    OUTLET ||--o{ DINING_TABLE : has
    OUTLET ||--o{ MENU_ITEM : offers
    MENU_CATEGORY ||--o{ MENU_ITEM : contains
    DINING_TABLE ||--o| RESTAURANT_ORDER : serves
    RESTAURANT_ORDER ||--|{ ORDER_ITEM : contains
    MENU_ITEM ||--o{ ORDER_ITEM : selected_as
    RESTAURANT_ORDER ||--o{ KITCHEN_TICKET : creates
    RESTAURANT_ORDER ||--o| BILL : produces
    BILL ||--o{ PAYMENT : receives
```

## Entities

### Restaurant

- id
- name

### Outlet

- id
- restaurantId
- name
- address
- active

### DiningTable

- id
- outletId
- tableNumber
- seats
- status

### MenuCategory

- id
- name
- displayOrder
- active

### MenuItem

- id
- categoryId
- outletId
- name
- price
- active

### RestaurantOrder

- id
- outletId
- tableId
- orderNumber
- orderType
- status
- createdAt
- notes

### OrderItem

- id
- orderId
- menuItemId
- quantity
- unitPrice
- status

### KitchenTicket

- id
- orderId
- status
- createdAt
- readyAt

### Bill

- id
- orderId
- subtotal
- discount
- cgst
- sgst
- total
- status

### Payment

- id
- billId
- method
- amount
- paidAt
- reference

## Modeling rule

The UI does not define these entities by itself. Entities are derived from approved business requirements and use cases; UI evidence can expose missing fields or relationships and should trigger review rather than silent domain invention.
