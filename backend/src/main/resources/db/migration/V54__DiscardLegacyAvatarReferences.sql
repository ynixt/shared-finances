UPDATE users
SET photo_url = NULL
WHERE photo_url IS NOT NULL;
