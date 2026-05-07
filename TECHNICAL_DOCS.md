# Rosswood Management System — Technical Documentation

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture Overview](#3-architecture-overview)
4. [Database Schema](#4-database-schema)
5. [Domain Model](#5-domain-model)
6. [Security & Authentication](#6-security--authentication)
7. [Role-Based Access Control](#7-role-based-access-control)
8. [Module Reference](#8-module-reference)
   - [Dashboard](#81-dashboard)
   - [Inventory & Items](#82-inventory--items)
   - [Production](#83-production)
   - [Sales & Invoicing](#84-sales--invoicing)
   - [Customers](#85-customers)
   - [Stock Management](#86-stock-management)
   - [Reports](#87-reports)
   - [User Management](#88-user-management)
   - [Setup (UOM & Item Types)](#89-setup-uom--item-types)
9. [Stock & Cost Accounting Engine](#9-stock--cost-accounting-engine)
10. [Business Rules](#10-business-rules)
11. [HTMX Interaction Pattern](#11-htmx-interaction-pattern)
12. [Template System](#12-template-system)
13. [API Endpoint Reference](#13-api-endpoint-reference)
14. [Configuration](#14-configuration)
15. [Database Migrations](#15-database-migrations)

---

## 1. Project Overview

The **Rosswood Management System** is a web-based ERP application built specifically for Rosswood's operational requirements. It covers:

- **Inventory management** — items, units of measure, stock levels, reorder alerts
- **Production management** — batch tracking, raw material consumption, finished-good output
- **Sales & invoicing** — invoice lifecycle (DRAFT → CONFIRMED → DELIVERED), payment methods, credit management
- **Customer management** — customer profiles, branch networks, credit limits
- **Stock accounting** — Weighted Average Cost (WAC), lot traceability, expiry tracking
- **Reports** — stock on hand, sales summaries, production reports, batch traceability
- **User management** — role-based access, account enable/disable

The system is a **server-rendered monolith** with thin JavaScript layers powered by HTMX for partial-page updates, built on Quarkus with the Renarde extension.

---

## 2. Technology Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 |
| Framework | Quarkus 3.x |
| Web layer | Quarkus Renarde (MVC controller extension) |
| Persistence | Hibernate ORM 6 via Panache (active-record pattern) |
| Database | PostgreSQL |
| Migrations | Flyway (auto-run at startup) |
| Templating | Qute (Quarkus native template engine) |
| UI framework | Materialize CSS 1.0 |
| Partial updates | HTMX |
| Security | Quarkus Security + Renarde auth (bcrypt passwords) |
| Build | Maven |

---

## 3. Architecture Overview

```
Browser
  │  (full page load / HTMX partial swap)
  ▼
Renarde HxController
  │  (route → method → template)
  ▼
Panache Entity / Service
  │  (active-record queries / @ApplicationScoped services)
  ▼
Hibernate ORM
  │
  ▼
PostgreSQL
```

### Key architectural decisions

**Renarde HxController** — All controllers extend `HxController` which provides:
- `isHxRequest()` — returns `true` when the request came from HTMX
- `onlyHxRequest()` — throws if called outside an HTMX request
- `{#authenticityToken/}` — CSRF token injection into Qute templates

**Panache Active Record** — Entities extend `PanacheEntity` (auto-generated `id` field) or `AuditableEntity` (adds `createdBy`, `createdAt`, `updatedBy`, `updatedAt`). Queries live as static methods on the entity class itself.

**Fragment rendering pattern** — Every list page renders either the full page (on direct navigation) or just the rows fragment (on HTMX requests). The `CheckedTemplate` inner class defines two variants: `index(...)` for full page and `index$rows(...)` for the rows fragment. The `render()` helper method in each controller switches between them based on `isHxRequest()`.

**Event-driven production finish** — When a production batch is finished, `ProductionBatchController` fires a CDI `ProductionFinishedEvent`. `InventoryObserver` handles this asynchronously: posts consumption OUT transactions, computes finished-good unit cost via WAC rollup, posts production output IN transaction, and marks the batch FINISHED. This decouples the controller from inventory side-effects.

---

## 4. Database Schema

### Entity-Relationship Overview

```
app_user
unit_of_measure
item_type ──< item >── item_uom
                 └──< item_uom_conversion
                 └──< stock_opening
                 └──< stock_transaction >── production_batch
                                        └── sales_invoice

customer ──< customer_branch ──< sales_invoice ──< sales_invoice_item >── item_uom
                                                                       └── item

production_batch ──< production_consumption >── item
```

### Table Definitions

#### `app_user`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Sequence-generated |
| username | VARCHAR(255) UNIQUE NOT NULL | Login credential |
| email | VARCHAR(255) UNIQUE NOT NULL | |
| password | VARCHAR(255) NOT NULL | bcrypt hash |
| firstName | VARCHAR(255) | |
| lastName | VARCHAR(255) | |
| isAdmin | BOOLEAN NOT NULL | Grants full access |
| status | VARCHAR(255) NOT NULL | REGISTERED / UNCONFIRMED / DISABLED |
| roles | VARCHAR(255) | Comma-separated role names |

#### `customer`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| customer_code | VARCHAR(255) UNIQUE NOT NULL | Auto-generated ROSS-NNNNN |
| name | VARCHAR(255) NOT NULL | |
| address | VARCHAR(255) | |
| email | VARCHAR(255) | |
| phone | VARCHAR(255) | |
| customer_type | VARCHAR(20) NOT NULL DEFAULT 'CASH' | CASH / CREDIT |
| credit_limit | DECIMAL(12,2) | NULL for CASH customers |
| + audit columns | | created_by, created_at, updated_by, updated_at |

#### `customer_branch`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| customer_id | BIGINT FK → customer | |
| branch_name | VARCHAR(255) NOT NULL | |
| branchAddress | VARCHAR(255) | |
| contactPerson | VARCHAR(255) | |
| contactPhone | VARCHAR(255) | |
| active_flag | BOOLEAN | |
| + audit columns | | |

#### `item`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| item_code | VARCHAR(255) UNIQUE NOT NULL | |
| item_name | VARCHAR(255) NOT NULL | |
| item_type_id | BIGINT FK → item_type | |
| uom_id | BIGINT FK → unit_of_measure | Base UOM |
| selling_price_ex_vat | DECIMAL(12,4) | |
| vat_rate | DECIMAL(5,2) | Default 0 |
| average_cost | DECIMAL(12,4) | WAC — maintained by StockTransactionService |
| shelf_life_days | INTEGER | Used for expiry projection |
| reorder_level | DECIMAL(12,4) | Triggers low-stock alert on dashboard |
| active_flag | BOOLEAN | |
| + audit columns | | |

#### `item_uom`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| item_id | BIGINT FK → item | |
| uom_code | VARCHAR(255) NOT NULL | e.g. PCS, CASE-12, KG |
| is_base | BOOLEAN NOT NULL | Only one base UOM per item |

#### `item_uom_conversion`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| item_id | BIGINT FK → item | |
| from_uom | VARCHAR(255) NOT NULL | |
| to_uom | VARCHAR(255) NOT NULL | |
| conversion_factor | DECIMAL(12,4) | e.g. 1 CASE-12 = 12 PCS |

#### `stock_opening`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| stock_date | DATE NOT NULL | |
| item_id | BIGINT FK → item | |
| opening_qty | DECIMAL(12,2) NOT NULL | |
| unit_cost | DECIMAL(12,4) | Seeds WAC |
| UNIQUE | (stock_date, item_id) | One opening per item per date |
| + audit columns | | |

#### `stock_transaction`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| transaction_date | DATE NOT NULL | |
| item_id | BIGINT FK → item | |
| batch_code | VARCHAR(255) | Production lot reference |
| expiry_date | DATE | |
| direction | VARCHAR(10) NOT NULL | IN / OUT |
| transaction_type | VARCHAR(255) NOT NULL | See TransactionType enum |
| quantity | DECIMAL(12,4) NOT NULL | Always positive |
| unit_cost | DECIMAL(12,4) | WAC at time of transaction |
| uom_code | VARCHAR(255) | |
| sales_invoice_id | BIGINT FK → sales_invoice | |
| production_batch_id | BIGINT FK → production_batch | |
| reference_no | VARCHAR(255) | |
| remarks | VARCHAR(255) | |
| + audit columns | | |

Indexed on: transaction_date, item_id, batch_code (lot traceability).

#### `production_batch`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| batch_code | VARCHAR(255) UNIQUE NOT NULL | Auto-generated PRD-NNNNN |
| production_date | DATE NOT NULL | |
| finished_item_id | BIGINT FK → item | |
| output_qty | DECIMAL(12,2) | |
| expiry_date | DATE | |
| status | VARCHAR(255) | OPEN / FINISHED / CANCELLED |
| remarks | VARCHAR(255) | |
| + audit columns | | |

#### `production_consumption`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| batch_id | BIGINT FK → production_batch | |
| raw_item_id | BIGINT FK → item | |
| consumed_qty | DECIMAL(12,2) NOT NULL | |
| + audit columns | | |

#### `sales_invoice`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| rosswood_invoice_no | VARCHAR(255) UNIQUE NOT NULL | Auto RW-INV-NNNNN |
| vat_invoice_no | VARCHAR(255) | Optional VAT receipt number |
| customer_branch_id | BIGINT FK → customer_branch | |
| invoice_date | DATE NOT NULL | |
| delivery_date | DATE | Set on DELIVERED |
| delivery_note_no | VARCHAR(255) | Waybill reference |
| status | VARCHAR(255) NOT NULL | DRAFT/CONFIRMED/DELIVERED/CANCELLED |
| payment_method | VARCHAR(20) NOT NULL DEFAULT 'CASH' | CASH/MOMO/CHEQUE/CREDIT |
| payment_ref | VARCHAR(255) | MoMo transaction ref / cheque number |
| payment_info | VARCHAR(255) | MoMo sender phone / bank name |
| payment_date | DATE | Cheque date only |
| total_amount_ex_vat | DECIMAL(12,2) | |
| vat_amount | DECIMAL(12,2) | |
| total_amount | DECIMAL(12,2) | |
| remarks | VARCHAR(255) | |
| created_by | VARCHAR(255) | Username who created the invoice |
| + audit columns | | |

#### `sales_invoice_item`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| invoice_id | BIGINT FK → sales_invoice | |
| item_id | BIGINT FK → item | |
| uom_id | BIGINT FK → item_uom | UOM used for this line |
| batch_code | VARCHAR(255) | |
| quantity | DECIMAL(12,4) NOT NULL | |
| unit_price_ex_vat | DECIMAL(12,2) NOT NULL | |
| vat_rate | DECIMAL(5,2) DEFAULT 0 | |
| discount_pct | DECIMAL(5,2) | Optional line discount |
| line_total | DECIMAL(12,2) | Ex-VAT, after discount |
| vat_amount | DECIMAL(12,2) | |
| line_total_inc_vat | DECIMAL(12,2) | |
| + audit columns | | |

---

## 5. Domain Model

### Enumerations

#### `Customer.CustomerType`
| Value | Description |
|-------|-------------|
| `CASH` | Customer pays upfront; no credit allowed |
| `CREDIT` | Customer has a credit limit; can have outstanding balances |

#### `SalesInvoice.InvoiceStatus`
| Value | Description |
|-------|-------------|
| `DRAFT` | Being built; editable; no stock impact |
| `CONFIRMED` | Locked; stock OUT transactions posted |
| `DELIVERED` | Goods handed over; delivery date recorded |
| `CANCELLED` | Voided; stock transactions reversed if was CONFIRMED |

#### `SalesInvoice.PaymentMethod`
| Value | Description |
|-------|-------------|
| `CASH` | Physical cash payment |
| `MOMO` | Mobile money (paymentRef = transaction ID, paymentInfo = sender phone) |
| `CHEQUE` | Cheque payment (paymentRef = cheque no, paymentInfo = bank, paymentDate = cheque date) |
| `CREDIT` | Outstanding balance on customer account; only valid for CREDIT-type customers |

#### `StockTransaction.StockDirection`
| Value | Description |
|-------|-------------|
| `IN` | Stock received / returned / produced |
| `OUT` | Stock issued / sold / consumed / written off |

#### `StockTransaction.TransactionType`
| Direction | Values |
|-----------|--------|
| IN | `PURCHASE`, `PRODUCTION_OUTPUT`, `RETURN_FROM_CUSTOMER`, `OPENING_STOCK`, `ADJUSTMENT_IN` |
| OUT | `SALE`, `PRODUCTION_CONSUME`, `DAMAGE`, `EXPIRY_WRITEOFF`, `ADJUSTMENT_OUT` |

#### `ProductionBatch.BatchStatus`
| Value | Description |
|-------|-------------|
| `OPEN` | Batch created; consumptions being added |
| `FINISHED` | Batch completed; stock transactions posted; WAC updated |
| `CANCELLED` | Batch voided |

#### `UserStatus`
| Value | Description |
|-------|-------------|
| `REGISTERED` | Active account; can log in |
| `UNCONFIRMED` | Created but not yet confirmed (reserved for future email flow) |
| `DISABLED` | Deactivated by admin; login blocked |

---

## 6. Security & Authentication

### Login Flow
1. User submits credentials to `POST /_renarde/security/login` (Renarde's built-in endpoint)
2. Renarde calls `MySecuritySetup.findById(username)` which delegates to `User.findByUserName(username)`
3. Password is verified against the bcrypt hash stored in `app_user.password`
4. On success: session cookie set; user redirected to originally-requested URL or dashboard
5. `User.registered()` always returns `true`; actual account blocking is enforced by checking `UserStatus.DISABLED` in the toggle endpoint (not at login time — a future improvement)

### Password Hashing
All passwords are hashed with **bcrypt** via `io.quarkus.elytron.security.common.BcryptUtil.bcryptHash(password)`. The admin seed password hash in V5 was generated at cost factor 10.

### CSRF Protection
Every mutating form includes `{#authenticityToken/}` — a Renarde-provided Qute tag that renders a hidden input with a server-generated CSRF token. The `quarkus.rest-csrf.require-form-url-encoded=false` setting allows CSRF tokens from both form-encoded and JSON bodies.

### Proactive Auth
`quarkus.http.auth.proactive=false` — authentication is not eagerly checked on every request. Controllers that require login use `@Authenticated` on the class.

### Security Identity in Templates
`NavHelper` (`@Named("nav") @RequestScoped`) is injected into every Qute template via `{inject:nav}`. It wraps `SecurityIdentity` and exposes boolean visibility flags that the layout template uses to show/hide nav items and action buttons.

---

## 7. Role-Based Access Control

### Role Definitions

| Role | Description |
|------|-------------|
| `sales` | Can create/manage invoices; sees only own invoices; can create CASH customers only |
| `inventory` | Can manage items, UOMs, item types, stock openings, stock transactions |
| `production` | Can manage production batches and consumptions |
| `finance` | Can view sales; can manage customer types and credit limits |
| `dispatch` | Delivery-focused role (reserved for future delivery workflow) |
| `customer_manager` | Can manage customers, branches, credit limits and types |
| `reports` | Can access reports module |
| `admin` | Full system access; user management |

`isAdmin = true` on the user record grants blanket access regardless of role assignments.

### NavHelper Visibility Matrix

| Nav item | Roles that can see it |
|----------|-----------------------|
| Dashboard | All authenticated users |
| Items | `inventory`, `production`, `admin` |
| Production | `production`, `admin` |
| Sales | `sales`, `finance`, `admin` |
| Customers | `sales`, `customer_manager`, `admin` |
| Reports | All authenticated users |
| Setup (UOM/Item Types/Users) | `admin` only |

### Customer Type Permission

The `canManageCustomerType()` check controls:
- Whether the "Customer Type" radio buttons appear in the new-customer form
- Whether the "Edit" button appears on credit settings in the branch detail pane
- Whether the server accepts a CREDIT type during customer create/update

| Role | Can set CREDIT type / credit limit |
|------|------------------------------------|
| `admin` | ✅ |
| `customer_manager` | ✅ |
| `finance` | ✅ |
| `sales` | ❌ — always forced to CASH |
| All others | ❌ |

### Sales Rep Invoice Scoping

A user is considered "sales-only" if they have the `sales` role but do NOT have `inventory`, `production`, `finance`, or `admin`. Sales-only users:
- See only invoices where `created_by = their username`
- Cannot view other users' invoices
- Cannot access the Items, Production, or Setup modules

---

## 8. Module Reference

### 8.1 Dashboard

**Controller:** `DashboardResource` (`GET /d`)  
**Template:** `pub/dashboard.html`

Two views are served based on the caller's role:

**Full operational dashboard** (non-sales-only users):
| KPI | Source |
|-----|--------|
| Finished goods stock value | `SUM(item.averageCost × stockOnHand)` for FINISHED_GOOD items |
| Low-stock items count | Items where `stockOnHand < reorderLevel` |
| Today's revenue | Confirmed + Delivered invoices with `invoiceDate = today` |
| Pending deliveries | CONFIRMED invoices not yet DELIVERED |
| Outstanding credit | CREDIT-method invoices in CONFIRMED or DELIVERED status |
| Recent stock movements | Latest 5 stock transactions |
| Pending production batches | OPEN production batches |
| Expiring soon | Batches expiring within 7 days |

**Personal sales rep dashboard** (sales-only users):
| KPI | Source |
|-----|--------|
| My revenue today | Invoices where `createdBy = me` and `invoiceDate = today` |
| My pending deliveries | Invoices where `createdBy = me` and status = CONFIRMED |
| My outstanding credit | CREDIT-method invoices where `createdBy = me` |
| My recent invoices | Latest 10 invoices where `createdBy = me` |

---

### 8.2 Inventory & Items

**Controller:** `ItemController` (`GET|POST /items`)  
**Templates:** `ItemController/index.html`, `itemDetailPane.html`, `itemFormFragment.html`

**Data model:**
- Each item belongs to one `ItemType` and has a base `UnitOfMeasure`
- Alternate UOMs are stored in `ItemUom` (e.g. a case of 12 PCS)
- Conversion factors between UOMs are stored in `ItemUomConversion`
- `averageCost` is the Weighted Average Cost, maintained automatically by `StockTransactionService`
- `reorderLevel` triggers a low-stock warning on the dashboard

**Operations:**
| Endpoint | Action |
|----------|--------|
| `GET /items` | Paginated list (page size 10), searchable by name/code |
| `GET /items/form-fragment` | HTMX — renders new item form |
| `POST /items/add` | Create item; auto-creates base ItemUom |
| `GET /items/{id}/details` | HTMX — renders item detail pane with UOMs, prices, conversions |
| `POST /items/{id}/uom` | Add alternate UOM |
| `POST /items/{id}/price` | Update selling price and VAT rate |
| `POST /items/{id}/conversion` | Add UOM conversion factor |
| `POST /items/{id}/reorder-level` | Update reorder level |

---

### 8.3 Production

**Controllers:** `ProductionBatchController` (`/production`), `ProductionConsumptionController` (`/consumption`)  
**Templates:** `ProductionBatchController/batch.html`, `consumptionDetail.html`, `editQtyForm.html`

**Batch lifecycle:**
```
OPEN → (add consumptions, adjust output qty) → FINISHED
                                             → CANCELLED
```

Only OPEN batches can be edited or deleted. Finishing a batch triggers the CDI event:

```
ProductionBatchController.finish()
  └─ fires ProductionFinishedEvent
       └─ InventoryObserver.onFinished()
            ├─ POST PRODUCTION_CONSUME OUT for each consumption line
            ├─ Compute unit cost of finished good via WAC rollup:
            │    unitCost = Σ(consumedQty × rawItem.averageCost) / batchOutputQty
            └─ POST PRODUCTION_OUTPUT IN with computed unit cost → updates finished-good WAC
```

**Operations:**
| Endpoint | Action |
|----------|--------|
| `GET /production` | Paginated list (page 5), searchable |
| `GET /production/form-fragment` | HTMX — new batch form |
| `POST /production` | Create batch (OPEN); auto-generates PRD-NNNNN code |
| `GET /production/{id}/consumptions` | HTMX — consumption detail pane |
| `POST /production/{id}/finish` | Fire ProductionFinishedEvent → stock transactions |
| `POST /production/{id}/qty` | Update output quantity (OPEN only) |
| `DELETE /production/{id}` | Delete batch (OPEN only) |
| `POST /consumption` | Add consumption line to a batch |
| `DELETE /consumption/{id}` | Remove consumption line |

---

### 8.4 Sales & Invoicing

**Controller:** `SalesInvoiceController` (`/invoices`)  
**Templates:** `SalesInvoiceController/index.html`, `invoiceDetail.html`, `invoiceFormFragment.html`

**Invoice lifecycle:**
```
DRAFT → CONFIRMED → DELIVERED
      ↘ CANCELLED ←────────┘
```

- **DRAFT**: Editable. No stock impact. Payment method can be changed. Line items can be added.
- **CONFIRMED**: Locked. Stock OUT posted for each line item. Credit limit check applied.
- **DELIVERED**: Final state. Delivery date and note number recorded.
- **CANCELLED**: Stock OUT reversed (IN posted with type RETURN_FROM_CUSTOMER) if was CONFIRMED.

**Line item calculation** (`SalesInvoiceItem.calculate()`):
```
lineTotal      = quantity × unitPriceExVat × (1 − discountPct/100)
vatAmount      = lineTotal × (vatRate/100)
lineTotalIncVat = lineTotal + vatAmount
```

**Credit limit enforcement** (on Confirm):
1. Check `customer.customerType == CREDIT` (CASH customers cannot use CREDIT payment method)
2. Compute `outstanding = Σ(totalAmount for CONFIRMED/DELIVERED CREDIT invoices for this customer)`
3. If `outstanding + thisInvoice.totalAmount > customer.creditLimit` → reject with error message

**Payment detail capture** (DRAFT only, via Payment form):
| Method | `paymentRef` | `paymentInfo` | `paymentDate` |
|--------|-------------|----------------|----------------|
| MOMO | MoMo transaction reference | Sender phone number | null |
| CHEQUE | Cheque number | Bank name | Cheque date |
| CASH / CREDIT | null | null | null |

**Sales rep scoping:** If the logged-in user is "sales-only" (sales role without inventory/production/finance/admin), all list and count queries are filtered by `createdBy = currentUser`.

**Operations:**
| Endpoint | Action |
|----------|--------|
| `GET /invoices` | Paginated list (page 10), searchable |
| `GET /invoices/form-fragment` | HTMX — new invoice form |
| `POST /invoices` | Create DRAFT invoice; stamps `createdBy` |
| `GET /invoices/{id}/items` | HTMX — invoice detail pane with line items |
| `GET /invoices/item/uoms` | HTMX — UOM options for a given item |
| `POST /invoices/{id}/items` | Add line item; recalculates totals |
| `POST /invoices/{id}/confirm` | Confirm invoice; posts stock; credit check |
| `POST /invoices/{id}/payment` | Update payment method and MoMo/cheque details |
| `POST /invoices/{id}/deliver` | Mark delivered; record delivery date and note |
| `POST /invoices/{id}/cancel` | Cancel; reverse stock if was CONFIRMED |

---

### 8.5 Customers

**Controller:** `CustomerController` (`/customers`) `@Authenticated`  
**Templates:** `CustomerController/index.html`, `branchDetail.html`, `customerFormFragment.html`

Customers have a 1-to-many relationship with branches. Invoices are always raised against a specific branch, not the parent customer.

**Customer code format:** `ROSS-NNNNN` (zero-padded sequential, e.g. `ROSS-00001`)

**Branch detail pane** (HTMX accordion) shows:
- Customer type badge (CASH in green, CREDIT in blue) and credit limit
- Inline credit settings edit form (visible only to `canManageCustomerType` users)
- Grid of all branches with address
- Add branch form

**Operations:**
| Endpoint | Action |
|----------|--------|
| `GET /customers` | Paginated list (page 10), searchable |
| `GET /customers/new-form-fragment` | HTMX — new customer form |
| `POST /customers` | Create customer; sales reps forced to CASH |
| `GET /customers/{id}/branches` | HTMX — branch detail pane |
| `POST /customers/{id}/credit-settings` | Update type/limit; blocked for sales reps |
| `POST /customers/{id}/branches` | Add branch |

---

### 8.6 Stock Management

**StockOpeningController** (`/stock-opening`): Records opening stock balances for items. Each opening entry seeds the WAC for that item if no prior transactions exist.

**StockTransactionController** (`/stock-transactions`): Manual stock movements. Accepts PURCHASE, DAMAGE, EXPIRY_WRITEOFF, ADJUSTMENT_IN, ADJUSTMENT_OUT. Rejects PRODUCTION_OUTPUT, PRODUCTION_CONSUME, SALE, RETURN_FROM_CUSTOMER (these are system-generated via their respective workflows).

---

### 8.7 Reports

**Controller:** `ReportsController` (`/reports`)  
**Templates:** `ReportsController/` (7 templates)

| Report | Endpoint | Description |
|--------|----------|-------------|
| Stock on Hand | `GET /reports/stock` | Current qty, WAC value, next expiry per item |
| Lots / Lot Trace | `GET /reports/stock/{itemId}/lots` | Per-lot breakdown with expiry dates |
| Sales Summary | `GET /reports/sales` | Date-range filter; customer breakdown; top items |
| Production Summary | `GET /reports/production` | Batches in range; material consumption summary |
| Expiry Report | `GET /reports/expiry` | Batches expiring in 7 and 30 days |
| Batch Trace | `GET /reports/trace?batchCode=` | Full movement history for a production lot |

Reports are read-only pages — no mutations.

---

### 8.8 User Management

**Controller:** `UserController` (`/admin/users`) `@Authenticated`  
**Template:** `UserController/index.html`

Accessible only to users with `isAdmin = true` (enforced via the Setup nav guard in the layout).

**Defined roles** (as constants in `UserController.ALL_ROLES`):
`sales`, `inventory`, `production`, `finance`, `dispatch`, `customer_manager`, `reports`, `admin`

**Operations:**
| Endpoint | Action |
|----------|--------|
| `GET /admin/users/index` | List all users |
| `POST /admin/users/create` | Create user with bcrypt password; role list → comma-separated `rolesRaw`; sets `isAdmin` if `admin` role selected |
| `POST /admin/users/{id}/roles` | Replace role set for user |
| `POST /admin/users/{id}/toggle` | Toggle REGISTERED ↔ DISABLED |

---

### 8.9 Setup (UOM & Item Types)

**`Uom` controller** (`/uom`): CRUD for `unit_of_measure`. UOM codes must be unique. Used as the base UOM reference for items.

**`ItemTypeController`** (`/itemtype`): CRUD for `item_type`. Standard codes are `RAW_MATERIAL`, `FINISHED_GOOD`, `PACKAGING`. The `SalesInvoiceController` filters item searches to `itemType.itemTypeCode = 'FINISHED_GOOD'`.

---

## 9. Stock & Cost Accounting Engine

### Weighted Average Cost (WAC)

Every item has an `averageCost` field that is maintained by `StockTransactionService`. The formula on any IN transaction:

```
newWac = (currentSOH × currentWac + incomingQty × incomingCost) / (currentSOH + incomingQty)
```

Where `currentSOH = StockTransaction.stockOnHand(item.id)` — the net of all prior IN minus OUT quantities.

**WAC update triggers:**
| Transaction type | Updates WAC? |
|-----------------|-------------|
| PURCHASE | ✅ |
| PRODUCTION_OUTPUT | ✅ (cost rolled up from raw material WACs at finish time) |
| OPENING_STOCK | ✅ |
| ADJUSTMENT_IN | ✅ |
| SALE / PRODUCTION_CONSUME / DAMAGE / EXPIRY_WRITEOFF / ADJUSTMENT_OUT | ❌ (OUT doesn't change WAC) |

### Finished-Good Cost Rollup

When a production batch is finished:
```
finishedGoodUnitCost = Σ(consumption.consumedQty × consumption.rawItem.averageCost) / batch.outputQty
```

This cost is then used as the `unitCost` for the PRODUCTION_OUTPUT IN transaction, and subsequently incorporated into the finished good's WAC.

### Stock on Hand Query

```java
StockTransaction.stockOnHand(itemId):
  SELECT COALESCE(SUM(CASE WHEN direction='IN' THEN quantity ELSE -quantity END), 0)
  FROM stock_transaction WHERE item_id = ?
```

---

## 10. Business Rules

### Invoice Rules
1. Only `DRAFT` invoices can have line items added or payment method changed.
2. On **Confirm**, stock OUT transactions are posted for every line item at the current WAC.
3. A CREDIT-method invoice can only be confirmed if the customer is of type CREDIT.
4. A CREDIT-method invoice can only be confirmed if `outstanding credit + this invoice ≤ credit limit` (when a credit limit is set).
5. Cancelling a CONFIRMED invoice reverses all stock OUTs (posts RETURN_FROM_CUSTOMER IN transactions).
6. Cancelling a DRAFT invoice posts no stock transactions.
7. `DELIVERED` invoices cannot be cancelled through the current UI.

### Customer Rules
1. Customer codes are auto-generated (`ROSS-NNNNN`) and cannot be changed.
2. Only users with `canManageCustomerType()` permission (admin, customer_manager, finance) can set a customer to CREDIT type or assign a credit limit.
3. Sales reps always create CASH customers regardless of what is submitted in the form (server-side enforcement).

### Production Rules
1. Only OPEN batches can have consumptions added or removed.
2. Only OPEN batches can be deleted.
3. Finishing a batch is irreversible through the UI.
4. Batch codes are auto-generated (`PRD-NNNNN`).

### Stock Rules
1. Manual transactions cannot be posted for PRODUCTION_OUTPUT, PRODUCTION_CONSUME, SALE, or RETURN_FROM_CUSTOMER — these must go through their respective domain flows.
2. Opening stock is unique per (date, item) — you cannot post two opening balances for the same item on the same date.

---

## 11. HTMX Interaction Pattern

The application uses a consistent interaction model:

### Accordion Detail Panes
List rows have a hidden `<div id="detail-{id}">` that is populated via HTMX GET on row click:
```html
<div onclick="..."
     data-url="{uri:Controller.getDetail(item.id)}"
     data-target="#detail-{item.id}">
```
The JS function calls `htmx.ajax('GET', url, { target, swap: 'innerHTML' })`.

### Inline Forms
Mutating actions (add, edit, delete) target `#containerDiv` and swap `innerHTML` to replace the entire list with the updated state. This is safer than targeted row swaps as it avoids stale IDs.

### Fragment Templates
The `{#fragment id="rows"}...{/fragment}` Qute tag wraps the repeating portion of a list. The corresponding `$rows` template variant renders only this fragment. On HTMX requests, only the fragment is rendered and swapped; on direct navigation, the full page layout is rendered.

### OOB Swaps
Some detail templates use `hx-swap-oob="true"` to update elements outside the primary target. Example: `invoiceDetail.html` updates the invoice total in the card header while swapping in the detail pane:
```html
<div id="total-{invoice.id}" hx-swap-oob="true">GHS {invoice.totalAmount}</div>
```

### Datepicker Init
Materialize CSS datepickers require JS initialization. Delivery forms init the datepicker when the form is shown:
```javascript
const dp = form.querySelectorAll('.datepicker');
M.Datepicker.init(dp, { format: 'yyyy-mm-dd', autoClose: true, container: 'body' });
```

---

## 12. Template System

### Qute Key Concepts

**Type-safe templates** — Declared via `@CheckedTemplate` inner class on the controller. Each method maps to a `.html` file under `src/main/resources/templates/{ControllerSimpleName}/`.

**CDI injection in templates** — `{inject:nav.showItems}` accesses `NavHelper` (a `@Named("nav") @RequestScoped` CDI bean) from any template. This is the mechanism for role-based UI gating.

**Template extensions** — `PaginationExtensions` (`@TemplateExtension`) adds `.range(n)`, `.plus(n)`, `.minus(n)` helpers to Integer, enabling `{#for i in totalPages.range}` to iterate pages.

**Ternary expressions in attributes:**
```html
<!-- Supported in {#if} blocks -->
{#if user.isAdmin}...{/if}

<!-- Supported in inline ternary — single condition only -->
style="color: {user.isAdmin ? 'red' : 'grey'};"

<!-- NOT supported — compound conditions in inline ternary -->
<!-- Use a Java helper method instead -->
{user.hasRole('admin') ? 'checked' : ''}  ✅
{user.rolesRaw != null && user.rolesRaw.contains('admin') ? 'checked' : ''}  ❌
```

**Fragment syntax:**
```html
{#fragment id="rows"}
  {#for item in items}...{/for}
{/fragment}
```
The controller references `Templates.index$rows(...)` (note the `$` separator).

### Layout Structure (`layout.html`)

- Materialize CSS nav with responsive sidenav
- Desktop: dropdown menus for Reports and Setup
- Mobile: flat sidenav with section labels
- All nav items gated by `{#if inject:nav.showX}..{/if}`
- User display name from `{inject:nav.displayName}`
- Nav links use `{uri:ControllerClass.method()}` for type-safe URL generation

---

## 13. API Endpoint Reference

### Full Endpoint List

| Method | Path | Controller | Description |
|--------|------|-----------|-------------|
| GET | `/d` | DashboardResource | Dashboard |
| GET | `/items/index` | ItemController | Items list |
| GET | `/items/form-fragment` | ItemController | New item form |
| POST | `/items/add` | ItemController | Create item |
| GET | `/items/{id}/details` | ItemController | Item detail pane |
| POST | `/items/{id}/uom` | ItemController | Add alternate UOM |
| POST | `/items/{id}/price` | ItemController | Update price/VAT |
| POST | `/items/{id}/conversion` | ItemController | Add UOM conversion |
| POST | `/items/{id}/reorder-level` | ItemController | Update reorder level |
| GET | `/uom` | Uom | UOM list |
| POST | `/uom` | Uom | Create UOM |
| POST | `/uom/{id}` | Uom | Update UOM |
| DELETE | `/uom/{id}` | Uom | Delete UOM |
| GET | `/itemtype` | ItemTypeController | Item type list |
| POST | `/itemtype` | ItemTypeController | Create item type |
| POST | `/itemtype/{id}` | ItemTypeController | Update item type |
| DELETE | `/itemtype/{id}` | ItemTypeController | Delete item type |
| GET | `/customers` | CustomerController | Customer list |
| GET | `/customers/new-form-fragment` | CustomerController | New customer form |
| POST | `/customers` | CustomerController | Create customer |
| GET | `/customers/{id}/branches` | CustomerController | Branch detail pane |
| POST | `/customers/{id}/credit-settings` | CustomerController | Update credit settings |
| POST | `/customers/{id}/branches` | CustomerController | Add branch |
| GET | `/invoices` | SalesInvoiceController | Invoice list |
| GET | `/invoices/form-fragment` | SalesInvoiceController | New invoice form |
| POST | `/invoices` | SalesInvoiceController | Create DRAFT invoice |
| GET | `/invoices/{id}/items` | SalesInvoiceController | Invoice detail pane |
| GET | `/invoices/item/uoms` | SalesInvoiceController | UOM options for item |
| POST | `/invoices/{id}/items` | SalesInvoiceController | Add line item |
| POST | `/invoices/{id}/confirm` | SalesInvoiceController | Confirm invoice |
| POST | `/invoices/{id}/payment` | SalesInvoiceController | Update payment details |
| POST | `/invoices/{id}/deliver` | SalesInvoiceController | Mark delivered |
| POST | `/invoices/{id}/cancel` | SalesInvoiceController | Cancel invoice |
| GET | `/production` | ProductionBatchController | Batch list |
| GET | `/production/form-fragment` | ProductionBatchController | New batch form |
| POST | `/production` | ProductionBatchController | Create batch |
| GET | `/production/{id}/consumptions` | ProductionBatchController | Consumption detail |
| POST | `/production/{id}/finish` | ProductionBatchController | Finish batch |
| POST | `/production/{id}/qty` | ProductionBatchController | Update output qty |
| DELETE | `/production/{id}` | ProductionBatchController | Delete batch |
| POST | `/consumption` | ProductionConsumptionController | Add consumption |
| DELETE | `/consumption/{id}` | ProductionConsumptionController | Delete consumption |
| GET | `/stock-opening` | StockOpeningController | Opening stock list |
| POST | `/stock-opening` | StockOpeningController | Post opening stock |
| DELETE | `/stock-opening/{id}` | StockOpeningController | Delete opening |
| GET | `/stock-transactions` | StockTransactionController | Transaction list |
| POST | `/stock-transactions` | StockTransactionController | Post manual transaction |
| DELETE | `/stock-transactions/{id}` | StockTransactionController | Delete transaction |
| GET | `/reports` | ReportsController | Reports index |
| GET | `/reports/stock` | ReportsController | Stock on hand report |
| GET | `/reports/stock/{itemId}/lots` | ReportsController | Lot breakdown |
| GET | `/reports/sales` | ReportsController | Sales summary |
| GET | `/reports/production` | ReportsController | Production summary |
| GET | `/reports/expiry` | ReportsController | Expiry report |
| GET | `/reports/trace` | ReportsController | Batch traceability |
| GET | `/admin/users/index` | UserController | User list |
| POST | `/admin/users/create` | UserController | Create user |
| POST | `/admin/users/{id}/roles` | UserController | Update user roles |
| POST | `/admin/users/{id}/toggle` | UserController | Enable/disable user |
| GET | `/Login/login` | Renarde built-in | Login page |
| POST | `/_renarde/security/login` | Renarde built-in | Process login |
| GET | `/_renarde/security/logout` | Renarde built-in | Logout |

---

## 14. Configuration

**`src/main/resources/application.properties`**

```properties
# Database
quarkus.datasource.db-kind=postgresql

# Flyway — run migrations on every startup
quarkus.flyway.migrate-at-start=true

# Dev profile only — wipe and re-seed DB on every restart
%dev.quarkus.flyway.clean-at-start=true

# Dev — allow raw SQL in the dev UI
%dev.quarkus.datasource.dev-ui.allow-sql=true

# Don't eagerly check auth on every request
quarkus.http.auth.proactive=false

# Allow CSRF tokens in non-form-urlencoded requests
quarkus.rest-csrf.require-form-url-encoded=false

# Renarde login redirect target
quarkus.renarde.security.login-page=/Login/login
```

**Notable dev-profile setting:** `%dev.quarkus.flyway.clean-at-start=true` drops and recreates the entire schema on each dev server restart. This means all migrations including V1 run fresh on every boot. Any migration that attempts to add a column that already exists (e.g. `ADD COLUMN created_by` when V1 already creates it) will fail. This was the root cause of the V18 migration conflict.

---

## 15. Database Migrations

Migrations live in `src/main/resources/db/migration/` and follow Flyway's `V{version}__{description}.sql` naming convention.

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__.sql` | Full initial schema — all tables, sequences, FKs |
| V2 | `V2__add_uom_fk_to_sales_invoice_item.sql` | Add `uom_id` FK to sales_invoice_item |
| V3 | `V3__user_master.sql` | Create `app_user` table |
| V4 | `V4__add_roles.sql` | Add `roles` column to app_user |
| V5 | `V5__admin_user.sql` | Seed initial admin account |
| V6 | `V6__stock_transaction_tbl_update.sql` | Add direction, batch_code, lot fields, indexes to stock_transaction |
| V7 | `V7__added_batch_code_to_sales_invoice_item.sql` | Add batch_code, price/VAT/discount columns to sales_invoice_item |
| V8 | `V8__added_expiry_date_to_production_batch.sql` | Add expiry_date to production_batch |
| V9 | `V9__.sql` | Seed initial product catalogue and sample data |
| V10 | `V10__changes_to_item_structure.sql` | Add selling_price, shelf_life_days, vat_rate to item |
| V11 | `V11__sales_invoice_additional_columns.sql` | Add delivery_date, delivery_note_no, status, vat totals to sales_invoice |
| V12 | `V12__sample_data.sql` | Large seed: real Rosswood products, customers, production batches |
| V13 | `V13__add_cost_fields.sql` | Add unit_cost to stock_transaction/opening, average_cost to item |
| V14 | `V14__add_discount_to_invoice_item.sql` | Add discount_pct to sales_invoice_item |
| V15 | `V15__payment_method_and_customer_type.sql` | Add payment_method to invoice; customer_type and credit_limit to customer |
| V16 | `V16__payment_details.sql` | Add payment_ref, payment_info, payment_date to sales_invoice |
| V17 | `V17__add_reorder_level.sql` | Add reorder_level to item |
| V18 | `V18__add_created_by_to_invoice.sql` | ⚠️ Legacy — `created_by` already exists from V1 via AuditableEntity; this migration will fail on a clean schema due to duplicate column |

> **Note on V18:** The `created_by` column on `sales_invoice` is inherited from `AuditableEntity` and was present from V1. V18 should be changed to `ALTER TABLE sales_invoice ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);` or removed entirely to prevent startup failures in dev mode (where `clean-at-start=true` reruns all migrations from scratch).

---

*Documentation generated: May 2026*  
*System: Rosswood Management System*  
*Built by: DataChefs*
