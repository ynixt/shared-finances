const {
    MIN_DATE,
    MAX_DATE,
    SF_APP_SERVICE_SECRET,
    API_URL = "",
    DELAY_MS = "1000",
} = process.env;

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

async function syncExchangeRates(date) {
    const url = `${API_URL}/exchange-rates/sync?date=${date}`;

    try {
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
    } catch (err) {
        console.error(err);
    }
}

async function main() {
    console.log(`Populating exchange rate history from ${MIN_DATE} to ${MAX_DATE}`);

    let current = MIN_DATE;

    while (current <= MAX_DATE) {
        process.stdout.write(`Syncing ${current} ... `);
        await syncExchangeRates(current);
        await sleep(DELAY_MS);

        current = addOneDay(current);
    }

    console.log("Done.");
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
