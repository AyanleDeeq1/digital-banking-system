// Dependency-free Chrome/Edge smoke checks. All /api requests are intercepted.
// Start Vite first; run: node scripts/responsive-check.mjs http://127.0.0.1:5173
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdtemp, writeFile, mkdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const origin = process.argv[2] || 'http://127.0.0.1:5173';
assert(['localhost', '127.0.0.1', '[::1]'].includes(new URL(origin).hostname), 'Only local test servers are supported');
const executable = process.env.BROWSER_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const profile = await mkdtemp(join(tmpdir(), 'urbank-responsive-'));
const chrome = spawn(executable, ['--headless=new', '--remote-debugging-port=0', `--user-data-dir=${profile}`, '--no-first-run', '--no-default-browser-check', 'about:blank'], { windowsHide: true });
const endpoint = await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Browser startup timed out')), 15000);
    chrome.once('error', error => { clearTimeout(timer); reject(error); });
    chrome.stderr.on('data', chunk => {
        const match = chunk.toString().match(/DevTools listening on (ws:\/\/\S+)/);
        if (match) { clearTimeout(timer); resolve(match[1]); }
    });
});
const ws = new WebSocket(endpoint);
await new Promise(resolve => ws.addEventListener('open', resolve, { once: true }));
let sequence = 0;
const pending = new Map();
let onPaused;
const failures = [];
ws.addEventListener('message', event => {
    const message = JSON.parse(event.data);
    if (message.id) {
        const promise = pending.get(message.id);
        pending.delete(message.id);
        if (message.error) promise.reject(new Error(message.error.message));
        else promise.resolve(message.result);
    } else if (message.method === 'Fetch.requestPaused') {
        onPaused(message.params).catch(error => failures.push(error));
    } else if (message.method === 'Runtime.exceptionThrown') {
        failures.push(new Error(message.params.exceptionDetails.text));
    }
});
function command(method, params = {}, sessionId) {
    const id = ++sequence;
    return new Promise((resolve, reject) => {
        pending.set(id, { resolve, reject });
        ws.send(JSON.stringify({ id, method, params, sessionId }));
    });
}
const { targetId } = await command('Target.createTarget', { url: 'about:blank' });
const { sessionId } = await command('Target.attachToTarget', { targetId, flatten: true });
const send = (method, params) => command(method, params, sessionId);
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
async function evaluate(expression) {
    const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    assert(!result.exceptionDetails, JSON.stringify(result.exceptionDetails));
    return result.result.value;
}
async function until(expression) {
    for (let i = 0; i < 100; i++) {
        if (await evaluate(expression)) return;
        await sleep(50);
    }
    console.error(await evaluate(`({url: location.href, visibleText: document.body.innerText.slice(0, 1500)})`));
    await screenshot('failure');
    throw new Error(`Timed out: ${expression}`);
}
const customer = { id: 1, firstName: 'Alexandria', lastName: 'Mobile Test', email: 'responsive-test@example.invalid' };
const accounts = [
    { id: 1, name: 'Main Account with a long descriptive name', accountNumber: '3424-5,1234567890', type: 'CHECKING', status: 'ACTIVE', balance: 2500 },
    { id: 2, name: 'Savings', accountNumber: '3424-5,0987654321', type: 'SAVINGS', status: 'ACTIVE', balance: 500 },
];
let authenticated = true;
let verified = false;
let apiFailure = false;
let historyEmpty = false;
let rejectWithdrawal = false;
let delay = 0;
let operations = 0;
const requests = [];
onPaused = async ({ requestId, request }) => {
    const path = new URL(request.url).pathname.replace('/api/customers', '');
    requests.push({ path, method: request.method });
    let status = 200;
    let data;
    if (path === '/csrf') data = { token: 'mock-csrf', headerName: 'X-XSRF-TOKEN' };
    else if (path === '/me') { status = authenticated ? 200 : 403; data = authenticated ? customer : { massage: 'Sign in required' }; }
    else if (!authenticated) { status = 403; data = { massage: 'Sign in required' }; }
    else if (apiFailure && request.method === 'GET') { status = 503; data = { massage: 'Unavailable' }; }
    else if (path === '/accounts') data = accounts;
    else if (path === '/card') data = { cardNumber: '0000000000000123', lastFour: '0123', cardHolderName: 'Alexandria Mobile Test', expiryDate: '2029-10-01', cvc2: '007', type: 'DEBIT' };
    else if (path.includes('transactions')) data = historyEmpty ? [] : [{ ledgerEntryId: 1, accountId: 1, accountName: accounts[0].name, amount: -250, type: 'TRANSFER', status: 'COMPLETED', createdAt: '2026-10-01T10:00:00Z' }];
    else if (request.method === 'POST') {
        assert.equal(request.headers['X-XSRF-TOKEN'] ?? request.headers['x-xsrf-token'], 'mock-csrf');
        if (path === '/atm/eject') { verified = false; status = 204; }
        else if (path === '/atm/pin') { verified = JSON.parse(request.postData).pin === '0123'; status = verified ? 204 : 401; data = { massage: 'Incorrect card PIN.' }; }
        else if (/\/atm\/accounts\/\d+\/(deposits|withdrawals)$/.test(path)) {
            operations++;
            if (!verified) { status = 403; data = { massage: 'Verify your card PIN.' }; }
            else if (rejectWithdrawal && path.endsWith('withdrawals')) { status = 409; data = { massage: 'Insufficient funds.' }; }
            else {
                const amount = JSON.parse(request.postData).amount;
                const withdrawal = path.endsWith('withdrawals');
                accounts[0].balance += withdrawal ? -amount : amount;
                status = 201; data = { id: operations, amount, type: withdrawal ? 'WITHDRAWAL' : 'DEPOSIT', status: 'COMPLETED' };
            }
        } else if (path.endsWith('/transfers')) { status = 201; data = { amount: 10 }; }
        else if (path === '/card/pin') data = { pin: '0123' };
        else if (path === '/logout') { authenticated = false; status = 204; }
        else { status = 404; data = { massage: `Unhandled mock ${path}` }; }
    } else { status = 404; data = { massage: `Unhandled mock ${path}` }; }
    if (delay) await sleep(delay);
    try {
        await send('Fetch.fulfillRequest', { requestId, responseCode: status, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: status === 204 ? '' : Buffer.from(JSON.stringify(data)).toString('base64') });
    } catch (error) {
        // React StrictMode effect cleanup and navigation abort intercepted GETs.
        if (request.method !== 'GET' || error.message !== 'Invalid InterceptionId.') throw error;
    }
};
await send('Page.enable');
await send('Runtime.enable');
await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/*' }] });
async function viewport(width, height, touch = width <= 800) {
    await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: touch });
    await send('Emulation.setTouchEmulationEnabled', { enabled: touch });
}
async function navigate(path) {
    await send('Page.navigate', { url: origin + path });
    await until(`location.pathname === ${JSON.stringify(path)} && !!document.querySelector('main')`);
    await until(`!Array.from(document.querySelectorAll('[role="status"]')).some(el => el.textContent.includes('Loading')) && !document.body.innerText.includes('Loading your session') && !document.body.innerText.includes('loading....')`);
    await until(`Array.from(document.images).every(image => image.complete)`);
}
async function layout(label) {
    const result = await evaluate(`({ width: innerWidth, scroll: document.documentElement.scrollWidth,
        outside: Array.from(document.querySelectorAll('main input, main select, main button, .sidebar a')).filter(el => {
            const r = el.getBoundingClientRect(); return r.width && (r.left < -1 || r.right > innerWidth + 1);
        }).map(el => el.id || el.textContent) })`);
    assert(result.scroll <= result.width + 1, `${label}: horizontal overflow ${JSON.stringify(result)}`);
    assert.deepEqual(result.outside, [], `${label}: controls outside viewport`);
}
async function click(text) {
    const selector = await evaluate(`(() => { const normalize = value => value.replace(/\\s+/g, ' ').trim(); const el = Array.from(document.querySelectorAll('button')).find(el => normalize(el.textContent) === normalize(${JSON.stringify(text)})); if (!el || el.disabled) return null; el.scrollIntoView({block:'center'}); const r = el.getBoundingClientRect(); return {x:r.x+r.width/2,y:r.y+r.height/2}; })()`);
    assert(selector, `Enabled button missing: ${text}`);
    await send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [selector] });
    await send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}
async function fill(id, value) {
    await evaluate(`(() => { const el = document.getElementById(${JSON.stringify(id)}); const setter = Object.getOwnPropertyDescriptor(el.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype, 'value').set; setter.call(el, ${JSON.stringify(value)}); el.dispatchEvent(new Event('input', {bubbles:true})); el.dispatchEvent(new Event('change', {bubbles:true})); })()`);
}
async function tapSelector(selector) {
    await evaluate(`document.querySelector(${JSON.stringify(selector)})?.scrollIntoView({block:'center'})`);
    // Let layout/scroll settle before measuring a touch target after navigation.
    await evaluate(`new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))`);
    const point = await evaluate(`(() => { const el = document.querySelector(${JSON.stringify(selector)}); if (!el) return null; const r = el.getBoundingClientRect(); return {x:r.x+r.width/2,y:r.y+r.height/2}; })()`);
    assert(point, `Missing control: ${selector}`);
    await send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [point] });
    await send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
}
const screenshotDir = process.env.SCREENSHOT_DIR;
async function screenshot(name) {
    if (!screenshotDir) return;
    await mkdir(screenshotDir, { recursive: true });
    const { data } = await send('Page.captureScreenshot', { captureBeyondViewport: true });
    await writeFile(join(screenshotDir, name + '.png'), Buffer.from(data, 'base64'));
}
try {
    const routes = ['/', '/login', '/register', '/dashboard', '/accounts', '/createAccount', '/my-card', '/transfer', '/deposit-withdraw', '/profile'];
    for (const [width, height] of [[320, 568], [375, 667], [390, 844], [430, 932], [768, 1024], [1024, 768], [1440, 1000], [844, 390]]) {
        await viewport(width, height);
        for (const route of routes) {
            authenticated = !['/login', '/register'].includes(route);
            await navigate(route);
            await layout(`${width}x${height} ${route}`);
        }
        console.log(`PASS layouts: ${width}x${height}, all 10 routes`);
    }
    authenticated = false;
    await viewport(320, 568);
    await navigate('/'); await layout('Anonymous home');
    await navigate('/deposit-withdraw');
    assert(await evaluate(`document.body.innerText.includes('sign in')`));
    console.log('PASS anonymous home and ATM sign-in state');

    authenticated = true;
    for (const [width, height, reduced] of [[320, 568, false], [390, 844, true], [1440, 1000, false], [844, 390, false]]) {
        await viewport(width, height, true);
        await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: reduced ? 'reduce' : 'no-preference' }] });
        await navigate('/deposit-withdraw');
        if (width === 390) await screenshot('atm-insert-390');
        await click('Insert Card');
        await until(`!!document.getElementById('atm-pin')`);
        await fill('atm-pin', '9999'); await click('Verify PIN');
        await until(`document.body.innerText.includes('Incorrect card PIN.')`);
        await fill('atm-pin', '0123'); await click('Verify PIN');
        await until(`!!document.querySelector('.atm-menu')`);
        for (const operation of ['Deposit', 'Withdraw']) {
            await click(operation); await until(`!!document.getElementById('atm-account')`);
            await fill('atm-account', '1'); await fill('atm-amount', '0');
            await click(operation);
            await until(`document.body.innerText.includes('Enter an amount above zero')`);
            await fill('atm-amount', '10,50');
            const before = operations;
            delay = 200;
            await click(operation);
            assert(await evaluate(`document.querySelector('.atm-screen [type=submit]').disabled`));
            // A second click while the API is pending must never issue another request.
            await evaluate(`document.querySelector('.atm-screen form').requestSubmit()`);
            await until(`document.body.innerText.includes(${JSON.stringify(operation === 'Deposit' ? 'Deposit complete' : 'Withdrawal complete')})`);
            await until(`!document.querySelector('.atm-screen').getAttribute('aria-busy').includes('true')`);
            delay = 0;
            assert.equal(operations, before + 1, 'Duplicate transaction');
            await layout(`${width} ATM ${operation} result`);
            if (width === 390) await screenshot(`atm-${operation.toLowerCase()}-390`);
            await click('Back to Menu');
            await until(`!!document.querySelector('.atm-menu')`);
        }
        await click('Withdraw'); await until(`!!document.getElementById('atm-account')`);
        await fill('atm-account', '1'); await fill('atm-amount', '99999');
        rejectWithdrawal = true;
        await click('Withdraw'); await until(`document.body.innerText.includes('Insufficient funds.')`);
        rejectWithdrawal = false;
        await click('Eject Card'); await until(`document.body.innerText.includes('Insert Card')`);
        assert.equal(verified, false);
        assert.equal(await evaluate(`document.querySelector('.atm-card').classList.contains('atm-card-inserted')`), false);
        console.log(`PASS ATM touch: ${width}x${height}, PIN rejection, validation, deposit, withdrawal, duplicate guard, funds error, ejection${reduced ? ', reduced motion' : ''}`);
    }
    await viewport(390, 844);
    await navigate('/accounts'); await screenshot('accounts-390');
    await navigate('/dashboard'); await screenshot('dashboard-390');
    await tapSelector('.sidebar a[href="/accounts"]');
    await until(`location.pathname === '/accounts' && !!document.querySelector('.account-select')`);
    await tapSelector('.accounts-table tr:last-child .account-select');
    await until(`location.search.includes('account=2')`);
    await tapSelector('.sidebar a[href="/my-card"]');
    await until(`location.pathname === '/my-card' && !!document.querySelector('.my-card-pin button')`);
    await click('Show PIN'); await fill('pin-password', 'mock-password'); await click('Show PIN');
    await until(`document.querySelector('.my-card-pin-value')?.textContent === '0123'`);
    await click('Hide PIN');
    await tapSelector('.header-nav a[href="/profile"]');
    await until(`location.pathname === '/profile' && !!document.querySelector('.profile-details')`);
    await tapSelector('.sidebar a[href="/dashboard"]');
    await until(`location.pathname === '/dashboard' && !!document.querySelector('.quick-actions')`);
    await tapSelector('.quick-action-card:has(img[alt="transfer"])');
    await until(`!!document.getElementById('transfer-from')`);
    await fill('transfer-from', '1'); await fill('transfer-to', '2'); await fill('transfer-amount', '10');
    await click('Transfer money'); await until(`document.body.innerText.includes('Transfer completed:')`);
    await click('Another Account'); await fill('transfer-recipient', '1234567890'); await fill('transfer-amount', '10');
    await click('Transfer money'); await until(`document.body.innerText.includes('Transfer completed:')`);
    await layout('Transfer completion');
    console.log('PASS mobile navigation, account selection, PIN reveal/hide, both transfer modes');
    delay = 300;
    await send('Page.navigate', { url: origin + '/deposit-withdraw' });
    await until(`document.body.innerText.includes('Loading your')`);
    await layout('ATM loading');
    await until(`!!document.querySelector('.atm-screen button') && !document.querySelector('.atm-screen button').disabled`);
    delay = 0;
    await click('Insert Card');
    await until(`document.body.innerText.includes('Inserting your card')`);
    await viewport(430, 932);
    await until(`!!document.getElementById('atm-pin')`);
    await layout('Resize during insertion');
    await click('Eject Card'); await until(`document.body.innerText.includes('Insert Card')`);
    console.log('PASS loading layout and resizing during card insertion');
    historyEmpty = true;
    await navigate('/accounts'); assert(await evaluate(`document.body.innerText.includes('No transactions yet')`));
    historyEmpty = false;
    apiFailure = true;
    for (const route of ['/accounts', '/my-card', '/transfer', '/deposit-withdraw']) { await navigate(route); await layout(`Error ${route}`); }
    apiFailure = false;
    assert.deepEqual(failures, [], 'Browser or mock errors');
    console.log(`PASS empty/error layouts; ${requests.length} API requests intercepted. No backend contacted.`);
} finally {
    await command('Browser.close').catch(() => {});
    ws.close();
    chrome.kill();
}
