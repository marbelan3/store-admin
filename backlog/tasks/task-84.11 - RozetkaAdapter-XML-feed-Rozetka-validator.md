---
id: TASK-84.11
title: RozetkaAdapter (XML feed + Rozetka validator)
status: To Do
assignee: []
created_date: '2026-04-17 15:26'
labels:
  - backend
  - channels
  - rozetka
  - ukraine
dependencies: []
parent_task_id: TASK-84
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Rozetka accepts XML/YML feed: host HTTPS, self-served. Generate feed з мапою категорій, ціною в UAH, атрибутами. Orders приходять через Rozetka Partners API. Validate feed через їх publicly available validator.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 ChannelFeedExporter generates XML з продуктами на signed public URL
- [ ] #2 Категорії: mapping required per product (обов'язково перед publish)
- [ ] #3 Required attributes: name, description, price, vendor, picture (1-15), param (key/value)
- [ ] #4 Feed validation pre-submit через Rozetka schema
- [ ] #5 Orders polling (Partners API) кожні 10min
- [ ] #6 Dropshipper check: якщо product.isDropship → warn перед publish
<!-- AC:END -->
