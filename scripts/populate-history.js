const fs = require("node:fs");
const path = require("node:path");

const {
    MIN_DATE,
    MAX_DATE,
    QUOTES,
    SF_APP_SERVICE_SECRET,
    API_URL = "",
} = process.env;

const CURRENCIES_FROM_JSON = process.argv.includes("--currencies-from-json");

if (!MIN_DATE) {
    throw new Error(
        "MIN_DATE is required. Usage: MIN_DATE=2024-01-01 MAX_DATE=2024-12-31 node scripts/populate-history.js"
    );
}

if (!MAX_DATE) {
    throw new Error(
        "MAX_DATE is required. Usage: MIN_DATE=2024-01-01 MAX_DATE=2024-12-31 node scripts/populate-history.js"
    );
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function addOneDay(dateString) {
    const date = new Date(`${dateString}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() + 1);
    return date.toISOString().slice(0, 10);
}

function buildQuotesParams() {
    if (!QUOTES) return "";

    return QUOTES.split(",")
        .map((quote) => quote.trim())
        .filter(Boolean)
        .map((quote) => `quotes=${encodeURIComponent(quote)}`)
        .map((param) => `&${param}`)
        .join("");
}

function loadCurrenciesFromJson() {
    const currenciesPath = path.resolve(
        __dirname,
        "../backend/src/main/resources/currencies.json"
    );

    const fileContent = fs.readFileSync(currenciesPath, "utf8");
    const currenciesJson = JSON.parse(fileContent);

    return Object.keys(currenciesJson);
}

async function syncExchangeRates(date, quotesParams, currency) {
    const currencyParam = currency
        ? `&quotes=${encodeURIComponent(currency)}`
        : "";

    const url = `${API_URL}/exchange-rates/sync?date=${date}${quotesParams}${currencyParam}`;

    const response = await fetch(url, {
        method: "POST",
        headers: {
            Authorization: SF_APP_SERVICE_SECRET,
        },
    });

    const body = await response.text();

    console.log(
        `HTTP ${response.status} - ${body}`
    );
}

async function main() {
    const currencies = CURRENCIES_FROM_JSON ? loadCurrenciesFromJson() : null;
    const quotesParams = CURRENCIES_FROM_JSON ? "" : buildQuotesParams();

    console.log(
        `Populating exchange rate history from ${MIN_DATE} to ${MAX_DATE}${
            CURRENCIES_FROM_JSON
                ? ` (currencies from currencies.json: ${currencies.length})`
                : QUOTES
                    ? ` (quotes: ${QUOTES})`
                    : ""
        }`
    );

    let current = MIN_DATE;

    while (current <= MAX_DATE) {
        if (CURRENCIES_FROM_JSON) {
            for (const currency of currencies) {
                process.stdout.write(`Syncing ${current} / ${currency} ... `);

                await syncExchangeRates(current, "", currency);

                await sleep(2000);
            }
        } else {
            process.stdout.write(`Syncing ${current} ... `);

            await syncExchangeRates(current, quotesParams);

            await sleep(2000);
        }

        current = addOneDay(current);
    }

    console.log("Done.");
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});