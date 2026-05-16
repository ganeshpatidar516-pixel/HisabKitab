-- Phase-9 P2 — sample BigQuery queries (Firebase Analytics export)
-- Replace PROJECT.DATASET with your linked Analytics dataset.

-- OCR funnel volume by phase (last 7 days)
SELECT
  event_date,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'phase') AS phase,
  COUNT(*) AS events
FROM `PROJECT.DATASET.events_*`
WHERE event_name = 'hk_ops_funnel'
  AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'domain') = 'ocr'
  AND _TABLE_SUFFIX BETWEEN FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
    AND FORMAT_DATE('%Y%m%d', CURRENT_DATE())
GROUP BY 1, 2
ORDER BY 1 DESC, 3 DESC;

-- Bill save outcomes
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'phase') AS phase,
  COUNT(*) AS events
FROM `PROJECT.DATASET.events_*`
WHERE event_name = 'hk_ops_funnel'
  AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'domain') = 'invoice'
  AND _TABLE_SUFFIX BETWEEN FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
    AND FORMAT_DATE('%Y%m%d', CURRENT_DATE())
GROUP BY 1
ORDER BY 2 DESC;

-- Sync degraded vs healthy
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'phase') AS phase,
  COUNT(*) AS events
FROM `PROJECT.DATASET.events_*`
WHERE event_name = 'hk_ops_funnel'
  AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'domain') = 'sync'
  AND _TABLE_SUFFIX BETWEEN FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
    AND FORMAT_DATE('%Y%m%d', CURRENT_DATE())
GROUP BY 1;
