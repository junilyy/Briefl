# BRIEFL Frontend

BRIEFL React frontend workspace.

## Tech Stack

- React
- TypeScript
- Vite
- ESLint

## Local Development

```bash
npm install
npm run dev
```

Default dev server:

```text
http://localhost:5173
```

## Verification

```bash
npm run lint
npm run build
```

## Notes

- Google Apps Script submissions use `VITE_GOOGLE_APPS_SCRIPT_URL`.
- Visitor rows are inserted into `visitors_ver2`.
- Feedback and service signup rows are inserted into `beta_testers_ver2`.
- If `VITE_GOOGLE_APPS_SCRIPT_URL` is empty, payloads are logged to the console without sending a request.
- Backend feedback APIs should not be added.
- The root `index.html` file is a local reference artifact and must not be committed.
