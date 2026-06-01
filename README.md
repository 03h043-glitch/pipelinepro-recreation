# PipelinePro Recreation

Native Android recreation of the extracted SalesTracker/Base44 web app.

This project is intentionally implemented with plain Android Java views and local JSON persistence so it is native, lightweight, and close to the original app's workflows without depending on React, Compose, or the Base44 runtime.

## What is included

- Dark SalesTracker shell inspired by the React layout.
- Store/employee picker with local region and store management.
- Dashboard with Today/Week/MTD/YTD and My Store/Full Region filters.
- Share-of-value banner, KPI cards, brand value chart, and weekly SOV chart.
- Log Sale workflow for TVs, soundbars, refunds, model year, model, size, tier, multi-sale quantity, and price.
- Today's Sales view with summaries, edit, and delete.
- Leaderboard, barcode/tools pages, commission tracker, and an ASM hub.
- Local persistence via `SharedPreferences` JSON arrays for sales, stores, accounts, barcodes, incentives, questions, and interactions.

## Open in Android Studio

1. Open this folder in Android Studio: `C:\Users\James\Documents\pipelinepro recreation`.
2. Let Android Studio install or locate an Android SDK if prompted.
3. Sync Gradle and run the `app` configuration.

The current machine did not expose an Android SDK or Gradle CLI on `PATH`, so this repo is set up for Android Studio sync rather than verified by a local command-line build.
