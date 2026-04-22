---
id: TASK-85.12
title: AI Catalog Translator — per-channel tone + i18n
status: To Do
assignee: []
created_date: '2026-04-22 07:27'
labels:
  - backend
  - ai
  - i18n
dependencies: []
parent_task_id: TASK-85
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
OpenAI/Claude API. Translate product content UA -> RU/EN/PL/CZ. Per-channel tone (Rozetka - neutral, Shopify - marketing, eBay - SEO-heavy). Batch job: translate all products for channel + target language. Stores translations in ProductTranslation.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 LlmTranslationService interface (OpenAI + Anthropic adapters)
- [ ] #2 Per-channel prompt templates (stored in DB)
- [ ] #3 Batch translation job: /ai/translate?channelId=&fromLang=uk&toLang=en
- [ ] #4 Progress tracking in UI
- [ ] #5 Caching: do not translate if source did not change
- [ ] #6 Cost estimation preview before batch run
- [ ] #7 Admin UI: /settings/ai - API keys, model choice, monthly spend cap
<!-- AC:END -->
