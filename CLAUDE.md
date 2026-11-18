# CLAUDE.md

## Fixing the expired Orange live-API test

`PaymentControllerIT` (`can_create_payment_beforeOrangeDailyRetrieval_then_verify_it`,
`can_create_payment_afterOrangeDailyRetrieval_then_verify_it`) calls the live Orange API for a fixed
date. Once that date expires the API times out (`OrangeApiException`). To make it green again, point
the tests at a recent transaction:

1. Fetch a recent day (not today; yesterday works):
   `curl "https://cbf314fda3.execute-api.eu-west-3.amazonaws.com/Prod/transactions?date=YYYY-MM-DD"`
2. Pick a random transaction with `status == "Succès"` (note the `è`; filter with
   `startswith('Succ')` if the shell mangles encoding). Keep its `ref`, `date`, `time`, `amount`.
3. Update the test data, as in commit `18de7a2`:
   - `src/test/java/school/hei/vola/conf/TestData.java`: `ORANGE_REF_SUCCEEDED` = `ref`.
   - `PaymentControllerIT`: every `LocalDate.of(...)` passed to
     `OrangeDailyTransactionsRetrievalRequested` = the date, `.amount(...)` = `amount`,
     `Instant.parse("<date>T<time>Z")` = `date` + `time` (time is taken as-is, with `Z`).
4. Run it live (needs the env var, otherwise the context fails to load with
   `Could not resolve placeholder 'orange.api.url'`):
   `ORANGE_API_URL=https://cbf314fda3.execute-api.eu-west-3.amazonaws.com/Prod ./gradlew test --tests '*PaymentControllerIT'`
   A `jacocoTestCoverageVerification` failure on a partial run is expected; only test failures matter.
5. Commit as `chore: fix monthly failing test` and open the PR with base `prod`.

## Flaky live-API test (504)

The Orange API is a scraper behind API Gateway: one call takes ~12s and concurrent calls can return
`504` after 30s. On a push to `prod`/`preprod`, `CI` and `CD Compute` run at the same time, so
`PaymentControllerIT` can fail with `OrangeApiException` while the values are still valid. The test
retries the retrieval up to 3 times (`retrieveOrangeDailyTransactions`); if it still fails, re-run
the failed jobs (`gh run rerun <run-id> --failed`) before changing any test data. Never edit
`ci.yml`, `cd-compute.yml` or `build.gradle` to work around it.
