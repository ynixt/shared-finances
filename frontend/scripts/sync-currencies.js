#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const backendCurrencies = path.resolve(__dirname, '../../backend/src/main/resources/currencies.json');
const frontendCurrencies = path.resolve(__dirname, '../public/currencies.json');

const data = JSON.parse(fs.readFileSync(backendCurrencies, 'utf-8'));

fs.writeFileSync(frontendCurrencies, JSON.stringify(data, null, 2) + '\n', 'utf-8');

console.log(`Synced ${Object.keys(data).length} currencies to ${frontendCurrencies}`);
