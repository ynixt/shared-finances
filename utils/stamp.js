"use strict";

const fs = require("fs");

function fail(message) {
  throw new Error(message);
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, data) {
  fs.writeFileSync(file, `${JSON.stringify(data, null, 2)}\n`);
}

function stampFrontend(version) {
  for (const file of ["frontend/package.json", "frontend/package-lock.json"]) {
    const data = readJson(file);
    data.version = version;

    if (data.packages && data.packages[""]) {
      data.packages[""].version = version;
    }

    writeJson(file, data);
  }
}

function stampBackend(version) {
  const file = "backend/build.gradle.kts";
  const content = fs.readFileSync(file, "utf8");
  const versionPattern = /^version\s*=\s*"[^"]*"\s*$/m;

  if (!versionPattern.test(content)) {
    fail("Could not update version in backend/build.gradle.kts");
  }

  const next = content.replace(versionPattern, `version = "${version}"`);

  if (next === content) {
    return;
  }

  fs.writeFileSync(file, next);
}

function main() {
  const [target, version] = process.argv.slice(2);

  if (!target || !version) {
    fail('Usage: node utils/stamp.js <frontend|backend> "<version>"');
  }

  if (target === "frontend") {
    stampFrontend(version);
    return;
  }

  if (target === "backend") {
    stampBackend(version);
    return;
  }

  fail(`Unknown target "${target}". Use "frontend" or "backend".`);
}

main();
