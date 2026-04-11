include dev.env
export

API_URL ?= http://localhost:$(APP_PORT)

# Usage:
#   make populate-history MIN_DATE=2024-01-01 MAX_DATE=2024-12-31
#   make populate-history MIN_DATE=2024-01-01 MAX_DATE=2024-12-31 QUOTES=USD,BRL,EUR
.PHONY: populate-history
populate-history:
ifndef MIN_DATE
	$(error MIN_DATE is required. Usage: make populate-history MIN_DATE=2024-01-01 MAX_DATE=2024-12-31 [QUOTES=USD,BRL])
endif
ifndef MAX_DATE
	$(error MAX_DATE is required. Usage: make populate-history MIN_DATE=2024-01-01 MAX_DATE=2024-12-31 [QUOTES=USD,BRL])
endif
	@quotes_params=""; \
	if [ -n "$(QUOTES)" ]; then \
		for q in $$(echo "$(QUOTES)" | tr ',' ' '); do \
			quotes_params="$$quotes_params&quotes=$$q"; \
		done; \
	fi; \
	echo "Populating exchange rate history from $(MIN_DATE) to $(MAX_DATE)$${quotes_params:+ (quotes: $(QUOTES))}"; \
	current="$(MIN_DATE)"; \
	while [ "$$current" \< "$(MAX_DATE)" ] || [ "$$current" = "$(MAX_DATE)" ]; do \
		echo -n "Syncing $$current ... "; \
		response=$$(curl -s -w "\n%{http_code}" -X POST \
			"$(API_URL)/exchange-rates/sync?date=$$current$$quotes_params" \
			-H "Authorization: $(APP_SERVICE_SECRET)"); \
		status=$$(echo "$$response" | tail -1); \
		body=$$(echo "$$response" | sed '$$d'); \
		echo "HTTP $$status - $$body"; \
		current=$$(date -d "$$current + 1 day" +%Y-%m-%d); \
	done; \
	echo "Done."
