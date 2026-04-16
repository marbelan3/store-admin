# Marketplace (CJ Dropshipping) -- Інтеграція

## Що це

Повна інтеграція з CJ Dropshipping всередині G2U Store Admin. Дозволяє шукати товари в каталозі CJ, імпортувати їх як локальні продукти, автоматично синхронізувати ціни та залишки, і розміщувати замовлення через CJ API.

## Архітектура

```
Frontend (SvelteKit)           Backend (Spring Boot)           CJ API
┌──────────────────┐     ┌──────────────────────────┐     ┌──────────┐
│ 7 сторінок UI    │────>│ 5 контролерів            │────>│ CJ REST  │
│ API клієнт       │     │ 6 сервісів               │     │ API v2   │
│ TypeScript типи  │     │ 6 доменних сутностей     │     └──────────┘
└──────────────────┘     │ Adapter pattern          │
                         │ Scheduled sync           │
                         └──────────────────────────┘
```

**Backend шари:**
- `infrastructure/marketplace/` -- адаптер CJ API (`MarketplaceAdapter` інтерфейс, `CjDropshippingAdapter` реалізація)
- `domain/marketplace/` -- JPA сутності та репозиторії (6 таблиць)
- `service/` -- бізнес-логіка (Connection, Import, Sync, Watchlist, Order сервіси + Scheduler)
- `web/controller/` -- REST API (5 контролерів)

**Frontend шари:**
- `lib/types/marketplace.ts` -- всі TypeScript інтерфейси
- `lib/api/marketplace.ts` -- 18+ API функцій
- `routes/(app)/marketplace/` -- 7 сторінок UI

## Сторінки та як користуватись

### 1. Connections (`/marketplace/connections`)

Управління підключеннями до CJ Dropshipping.

**Що можна:**
- Створити підключення (API ключ, дефолтний склад, метод доставки)
- Тестувати з'єднання (перевірка токена)
- Увімкнути/вимкнути автосинхронізацію
- Переглянути логи синхронізації
- Видалити підключення

**Як почати:**
1. Отримати API ключ в [CJ Dropshipping Developer Portal](https://developers.cjdropshipping.com/)
2. Натиснути "Add Connection"
3. Вибрати провайдера, ввести API ключ
4. Вказати дефолтний склад (CN, US, DE, UK, AU, TH)
5. Натиснути "Create" -- система автоматично отримає access token

### 2. Import Products (`/marketplace/import`)

Пошук і імпорт товарів з каталогу CJ.

**Як імпортувати:**
1. Вибрати підключення зі списку
2. Ввести пошуковий запит (назва товару англійською)
3. Натиснути на картку товару -- відкриється діалог деталей
4. Відмітити потрібні варіанти (розміри, кольори)
5. Налаштувати ціноутворення:
   - **Margin %** -- націнка у відсотках (дефолт 30%)
   - **Fixed Markup** -- фіксована надбавка до ціни CJ
   - **Manual** -- ручна ціна для кожного варіанту
6. Вказати мінімальну маржу для алертів та поріг низького залишку
7. Натиснути "Import"

**Batch Import:** Можна вибрати декілька товарів чекбоксами та імпортувати пакетно з єдиними налаштуваннями.

При імпорті створюється:
- Локальний `Product` (статус DRAFT, source = MARKETPLACE)
- `MarketplaceProduct` з налаштуваннями ціноутворення
- `VariantMapping` для кожного варіанту (зв'язок local variant <-> CJ SKU)
- Медіа (зображення) автоматично підтягуються з CJ

### 3. Products (`/marketplace/products`)

Список всіх імпортованих marketplace товарів.

**Таблиця показує:**
- Назва (з бейджами Excluded / Margin Alert)
- Статус синхронізації (SYNCED, PENDING, ERROR, DELISTED)
- Ціна CJ, поточна маржа (кольорова: зелена >= 20%, жовта 10-20%, червона < 10%)
- Залишки (кольорові відносно порогу)
- Inline-редагування порогу та перемикач excluded

**Дії:** клік на рядок переходить на сторінку деталей.

### 4. Product Detail (`/marketplace/products/[id]`)

Детальна інформація по конкретному marketplace товару.

**Картки зверху:**
- Pricing Rule -- inline зміна правила ціноутворення (Select + Input для margin %)
- Current Margin -- поточна маржа з кольоровою індикацією
- Sync Status -- статус + бейдж excluded
- Variants -- кількість та CJ ID

**Таблиця варіантів:**
- Variant name, CJ SKU (з попереднім SKU якщо змінився), склад, ціна CJ, доставка, залишок, статус SKU, дата перевірки
- **Клік на рядок** розгортає історію цін: міні-графік SVG + список останніх змін (стара ціна -> нова ціна, маржа)

**Контролі:**
- Перемикач "Excluded from sync" -- виключити товар із синхронізації
- Поле "Low stock threshold" -- поріг для алертів

### 5. Watchlist (`/marketplace/watchlist`)

Список товарів CJ, які ви хочете моніторити без імпорту.

**Як додати:** На сторінці Import натисніть іконку ока на картці товару.

**Показує:** назву, ціну, статус залишку (IN_STOCK / LOW / OUT_OF_STOCK), дату перевірки.

### 6. Alerts (`/marketplace/alerts`)

Сторінка алертів по всіх marketplace товарах.

**Типи алертів:**
- **MARGIN_VIOLATION** (червоний) -- маржа впала нижче мінімуму
- **SKU_CHANGED** (жовтий) -- CJ змінив SKU варіанту
- **OUT_OF_STOCK** (оранжевий) -- товар закінчився на CJ
- **LOW_STOCK** (жовтий) -- залишок нижче порогу
- **DELISTED** (сірий) -- товар знято з продажу на CJ

Зверху -- підсумкові бейджі з кількістю алертів по типах. Кожен алерт має кнопку "View" для переходу до товару.

### 7. Sync Logs (`/marketplace/sync-logs`)

Логи синхронізації по конкретному підключенню.

**Показує:** тип синку (PRICE / STOCK / FULL), статус, кількість перевірених/оновлених/помилок, тривалість, час.

**Як запустити синк вручну:** кнопка "Trigger Sync" зверху (або на сторінці Connections).

## Автоматична синхронізація

Scheduled job (`MarketplaceSyncScheduler`) автоматично запускає синхронізацію для всіх активних підключень з увімкненим `syncEnabled`. Перевіряє:
- Зміни цін на CJ -- оновлює `sourcePrice`, записує в `price_history`
- Зміни залишків -- оновлює `stockQuantity`
- Зміни SKU -- детектує CHANGED / DISCONTINUED / NOT_FOUND статуси
- Генерує алерти при порушенні маржі, низькому залишку, зміні SKU

## Фільтр source на сторінці Products

На головній сторінці Products (`/products`) доданий фільтр "Source":
- **All Sources** -- всі товари
- **Own Products** -- тільки створені вручну
- **Marketplace** -- тільки імпортовані з CJ

Marketplace товари позначені синім бейджем "CJ" з іконкою Globe.

## Ціноутворення

| Правило | Формула | Коли використовувати |
|---------|---------|---------------------|
| MARGIN | `cjPrice * (1 + margin/100)` | Стандартний випадок, прибуток у % |
| FIXED_MARKUP | `cjPrice + fixedAmount` | Фіксована надбавка в $ |
| MANUAL | Ручна ціна за варіант | Повний контроль над ціною |

Ціни автоматично перераховуються при зміні правила або параметрів.

## Структура БД (marketplace таблиці)

```
marketplace_connections     -- підключення до CJ API
marketplace_products        -- зв'язок local product <-> CJ product
marketplace_variant_mappings -- зв'язок local variant <-> CJ SKU + ціна/залишок
marketplace_price_history   -- історія змін цін
marketplace_sync_logs       -- логи синхронізації
marketplace_watchlist       -- товари для моніторингу
```

Міграція: `025-create-marketplace.yaml`

## API Endpoints

| Метод | URL | Опис |
|-------|-----|------|
| GET | `/api/marketplace/connections` | Список підключень |
| POST | `/api/marketplace/connections` | Створити підключення |
| PUT | `/api/marketplace/connections/{id}` | Оновити підключення |
| DELETE | `/api/marketplace/connections/{id}` | Видалити підключення |
| POST | `/api/marketplace/connections/{id}/test` | Тестувати з'єднання |
| GET | `/api/marketplace/cj/catalog` | Пошук в каталозі CJ |
| GET | `/api/marketplace/cj/products/{pid}` | Деталі товару CJ |
| POST | `/api/marketplace/import` | Імпорт товару |
| GET | `/api/marketplace/products` | Список marketplace товарів |
| GET | `/api/marketplace/products/{id}` | Деталі marketplace товару |
| PATCH | `/api/marketplace/products/{id}/pricing` | Оновити ціноутворення |
| PATCH | `/api/marketplace/products/{id}/exclude` | Toggle excluded |
| PATCH | `/api/marketplace/products/{id}/threshold` | Оновити поріг залишку |
| GET | `/api/marketplace/variants/{id}/price-history` | Історія цін варіанту |
| GET | `/api/marketplace/watchlist` | Список watchlist |
| POST | `/api/marketplace/watchlist` | Додати до watchlist |
| DELETE | `/api/marketplace/watchlist/{id}` | Видалити з watchlist |
| GET | `/api/marketplace/sync-logs/{connectionId}` | Логи синхронізації |
| POST | `/api/marketplace/sync/trigger/{connectionId}` | Запустити синк |
| GET | `/api/marketplace/alerts` | Отримати алерти |
| POST | `/api/marketplace/orders/{orderId}/place` | Розмістити замовлення |

Всі ендпоінти вимагають роль `SUPER_ADMIN` або `TENANT_ADMIN`.

## Тести

**215 тестів, всі проходять.**

Marketplace-specific:
- `MarketplaceConnectionServiceIntegrationTest` -- CRUD підключень, автентифікація
- `MarketplaceImportServiceIntegrationTest` -- імпорт, pricing, exclude, threshold, watchlist, price history
- `MarketplaceSyncServiceIntegrationTest` -- синхронізація цін/залишків
- `MarketplaceOrderServiceIntegrationTest` -- розміщення замовлень
- `MarketplaceCatalogControllerTest` -- HTTP endpoints (pricing, history, source filter, auth)

Тестовий адаптер: `TestMarketplaceConfig` -- мок CJ API для інтеграційних тестів.
