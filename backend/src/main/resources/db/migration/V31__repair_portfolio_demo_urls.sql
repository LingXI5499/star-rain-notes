-- Bare domains were previously interpreted by browsers as routes within the
-- portfolio page. Store an absolute URL instead.
UPDATE portfolio_project
SET demo_url = CONCAT('https://', TRIM(demo_url))
WHERE demo_url IS NOT NULL
  AND TRIM(demo_url) <> ''
  AND LOWER(TRIM(demo_url)) NOT REGEXP '^[a-z][a-z0-9+.-]*://';

-- A repository is source code, not an online deployment. When both fields
-- contain the same GitHub URL, retain repository_url and clear demo_url so the
-- public UI can fall back to the uploaded static prototype (or unavailable).
UPDATE portfolio_project
SET demo_url = NULL
WHERE demo_url IS NOT NULL
  AND repository_url IS NOT NULL
  AND LOWER(TRIM(TRAILING '/' FROM demo_url)) = LOWER(TRIM(TRAILING '/' FROM repository_url))
  AND LOWER(demo_url) REGEXP '^https?://(www\\.)?github\\.com/';
