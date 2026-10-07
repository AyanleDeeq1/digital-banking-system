# Responsive UI review

The shared `src/style/Responsive.css` loads after page styles. Banking sidebars
become a three-link navigation grid below 800px. Forms and cards fit the available
width; account and transaction tables become labeled rows below 600px while
retaining their column headers and explicit table/row/cell semantics.

The combined deposit/withdrawal route preserves the ATM image and card-slot
animation. Below 1200px, or in short landscape viewports, the upper machine is
shown above a full-width ATM console. The pictured screen mirrors the current
step, and the same live controls and handlers serve both layouts. Tap Insert Card,
verify the PIN, choose an operation, then eject the card. No drag gesture is needed.
An insertion timer also completes the transition if a resize interrupts it.
Authentication, CSRF, monetary validation, error handling, and server balance
refresh remain in the existing handlers.

## Run locally

Start the backend using the development instructions in the root README and a
dedicated development database. In a separate PowerShell terminal:

```powershell
cd frontend
npm.cmd run dev
```

Review Home, Login, Register, Dashboard, Accounts, Create Account, My Card,
Transfer, Deposit / Withdraw, and Profile at 320, 375, 390, 430, 768, 1024, and
1440px, plus landscape. Start with the ATM and account history. Check both
transfer modes, long names, touch navigation, focus indicators, and form fields
while the phone keyboard is open. On desktop, verify the ATM overlay still scrolls
to its lower controls.

## Automated checks

```powershell
npm.cmd run lint
npm.cmd run build
node scripts/responsive-check.mjs http://127.0.0.1:5173
```

Use the port printed by Vite. The browser smoke checks require Node 22 and an
installed Chrome or Edge. The default executable is Windows Chrome. To override:

```powershell
$env:BROWSER_PATH = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
$env:SCREENSHOT_DIR = "$env:TEMP/urbank-responsive-screenshots"
node scripts/responsive-check.mjs http://127.0.0.1:5173
```

This uses Node's built-in assertions and Chrome's debugging protocol, with no
new test framework or dependencies. It launches an isolated headless browser and
intercepts every `/api/*` request with test-only fixtures. It never sends money
operations to the backend; fixtures are not included in the application bundle.
Temporary browser profiles and optional screenshots are stored outside the repo.

Coverage includes all ten routes at eight viewport sizes, page overflow and
off-screen controls, touch ATM insertion, rejected/correct PINs, zero-amount
validation, deposit/withdrawal completion, disabled and duplicate-submission
behavior, insufficient funds, ejection, reduced motion, resizing during insertion,
loading/error/empty states, navigation, account selection, PIN reveal/hide, and
both transfer modes. Screenshots are optional for visual review.

There is no existing frontend unit-test suite or type-check script. Backend tests
require a disposable MySQL test database and are separate from these frontend
checks. Mocked browser checks do not verify backend security or ledger persistence.
Real-device Safari/Android keyboard, screen-reader behavior, and end-to-end flows
against a development backend still require manual review.
