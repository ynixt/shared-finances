include dev.env
export

API_URL ?= https://finances.gabrielsilva.dev/api
SECRETS_DIR ?= secrets

up:
	docker compose -p shared-finances -f docker-compose.yml --env-file .env.production up -d

up-b:
	docker compose -p shared-finances -f docker-compose.yml --env-file .env.production up -d --build

down:
	docker compose -f docker-compose.yml --env-file .env.production down --remove-orphans

down-v:
	docker compose -f docker-compose.yml --env-file .env.production down -v

up-dev:
	docker compose -p shared-finances-dev -f docker-compose-dev.yml --env-file dev.env up -d

down-dev:
	docker compose -f docker-compose-dev.yml --env-file dev.env down --remove-orphans

down-dev-v:
	docker compose -f docker-compose-dev.yml --env-file dev.env down -v

# Usage:
#   make populate-history MIN_DATE=2024-01-01 MAX_DATE=2026-04-24
#   make populate-history MIN_DATE=2024-01-01 MAX_DATE=2024-12-31 QUOTES=USD,BRL,EUR
.PHONY: populate-history
populate-history:
	MIN_DATE="$(MIN_DATE)" \
	MAX_DATE="$(MAX_DATE)" \
	QUOTES="$(QUOTES)" \
	SF_APP_SERVICE_SECRET="$(SF_APP_SERVICE_SECRET)" \
	API_URL="$(API_URL)" \
	node utils/populate-history.js

# Begin: JWT area

.PHONY: ensure-secrets-dir
ensure-secrets-dir:
	@mkdir -p $(SECRETS_DIR)

# 1) generate private key PKCS#8 PEM
.PHONY: jwt-private-key
jwt-private-key: ensure-secrets-dir
	@echo "Generating private key: $(SECRETS_DIR)/jwt-private.pem"
	@openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $(SECRETS_DIR)/jwt-private.pem

# 2) generate PEM public key
.PHONY: jwt-public-key
jwt-public-key: ensure-secrets-dir
	@test -f $(SECRETS_DIR)/jwt-private.pem || (echo "Missing $(SECRETS_DIR)/jwt-private.pem. Run: make jwt-private-key" && exit 1)
	@echo "Generating public key: $(SECRETS_DIR)/jwt-public.pem"
	@openssl pkey -in $(SECRETS_DIR)/jwt-private.pem -pubout -out $(SECRETS_DIR)/jwt-public.pem

# 3) convert PEM into binary DER
.PHONY: jwt-der
jwt-der: ensure-secrets-dir
	@test -f $(SECRETS_DIR)/jwt-private.pem || (echo "Missing $(SECRETS_DIR)/jwt-private.pem. Run: make jwt-private-key" && exit 1)
	@test -f $(SECRETS_DIR)/jwt-public.pem || (echo "Missing $(SECRETS_DIR)/jwt-public.pem. Run: make jwt-public-key" && exit 1)
	@echo "Generating DER files in $(SECRETS_DIR)"
	@openssl pkcs8 -topk8 -inform PEM -outform DER -nocrypt \
		-in $(SECRETS_DIR)/jwt-private.pem \
		-out $(SECRETS_DIR)/jwt-private.der
	@openssl pkey -pubin -inform PEM -outform DER \
		-in $(SECRETS_DIR)/jwt-public.pem \
		-out $(SECRETS_DIR)/jwt-public.der

# 4) convert DER into base64
.PHONY: jwt-b64
jwt-b64: ensure-secrets-dir
	@test -f $(SECRETS_DIR)/jwt-private.der || (echo "Missing $(SECRETS_DIR)/jwt-private.der. Run: make jwt-der" && exit 1)
	@test -f $(SECRETS_DIR)/jwt-public.der || (echo "Missing $(SECRETS_DIR)/jwt-public.der. Run: make jwt-der" && exit 1)
	@echo "Generating base64 files in one line"
	@base64 -w 0 $(SECRETS_DIR)/jwt-private.der > $(SECRETS_DIR)/jwt-private.der.b64
	@base64 -w 0 $(SECRETS_DIR)/jwt-public.der > $(SECRETS_DIR)/jwt-public.der.b64
	@echo "Done."

# 5) Update SF_APP_JWT_PUBLIC_KEY and SF_APP_JWT_PRIVATE_KEY in dev.env
.PHONY: jwt-update-dev-env
jwt-update-dev-env:
	@test -f dev.env || (echo "Missing dev.env" && exit 1)
	@test -f $(SECRETS_DIR)/jwt-public.der.b64 || (echo "Missing $(SECRETS_DIR)/jwt-public.der.b64. Run: make jwt-b64" && exit 1)
	@test -f $(SECRETS_DIR)/jwt-private.der.b64 || (echo "Missing $(SECRETS_DIR)/jwt-private.der.b64. Run: make jwt-b64" && exit 1)
	@public_key=$$(cat $(SECRETS_DIR)/jwt-public.der.b64); \
	private_key=$$(cat $(SECRETS_DIR)/jwt-private.der.b64); \
	if grep -q '^SF_APP_JWT_PUBLIC_KEY=' dev.env; then \
		sed -i "s|^SF_APP_JWT_PUBLIC_KEY=.*|SF_APP_JWT_PUBLIC_KEY=$$public_key|" dev.env; \
	else \
		echo "SF_APP_JWT_PUBLIC_KEY=$$public_key" >> dev.env; \
	fi; \
	if grep -q '^SF_APP_JWT_PRIVATE_KEY=' dev.env; then \
		sed -i "s|^SF_APP_JWT_PRIVATE_KEY=.*|SF_APP_JWT_PRIVATE_KEY=$$private_key|" dev.env; \
	else \
		echo "SF_APP_JWT_PRIVATE_KEY=$$private_key" >> dev.env; \
	fi; \
	echo "dev.env updated."

# Remove tudo dentro de secrets/
.PHONY: clean-secrets
clean-secrets:
	@mkdir -p $(SECRETS_DIR)
	@find $(SECRETS_DIR) -mindepth 1 -delete
	@echo "All files removed from $(SECRETS_DIR)/"

# End: JWT area

.PHONY: stamp-frontend-version stamp-backend-version stamp-version

stamp-version: stamp-frontend-version stamp-backend-version

stamp-frontend-version:
	node utils/stamp.js frontend "$(VERSION)"

stamp-backend-version:
	node utils/stamp.js backend "$(VERSION)"
