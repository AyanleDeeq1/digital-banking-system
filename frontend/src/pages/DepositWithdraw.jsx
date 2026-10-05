import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Header from '../components/Header.jsx';
import Footer from '../components/Footer.jsx';
import SideBar from '../components/SideBar.jsx';
import BankCard from '../components/BankCard.jsx';
import { formatBalance } from '../utils/formatBalance.js';
import machine from '../assets/atm.jpg';
import '../style/DepositWithdraw.css';

const api = '/api/customers';
async function readResponse(response) {
    if (response.status === 204) return null;
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        const failure = new Error(response.status < 500 && typeof data?.massage === 'string'
            ? Object.values(data.fieldErrors ?? {}).flat().join(' ') || data.massage
            : 'The ATM is temporarily unavailable. Please try again later.');
        failure.status = response.status;
        throw failure;
    }
    return data;
}
async function fetchAccounts() {
    const data = await readResponse(await fetch(`${api}/accounts`, { credentials: 'include' }));
    if (!Array.isArray(data)) throw new Error('Unable to read your accounts.');
    return data;
}

function DepositWithdraw({ customer, customerStatus , logout}) {
    const [step, setStep] = useState('INSERT_CARD');
    const [card, setCard] = useState(null);
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [pin, setPin] = useState('');
    const [amount, setAmount] = useState('');
    const [accountId, setAccountId] = useState('');
    const [busy, setBusy] = useState(false);
    const [inserted, setInserted] = useState(false);
    const [result, setResult] = useState(null);
    const [balanceError, setBalanceError] = useState('');
    const busyRef = useRef(false);
    const cardRef = useRef(null);
    const slotRef = useRef(null);
    const active = accounts.filter(account => account.status === 'ACTIVE');
    const selected = active.find(account => String(account.id) === accountId);

    useEffect(() => {
        if (!customer) return;
        let cancelled = false;
        Promise.all([
            fetch(`${api}/card`, { credentials: 'include', cache: 'no-store' }).then(readResponse),
            fetchAccounts(),
        ]).then(([data, accountData]) => {
            if (cancelled) return;
            if (!data || !/^[0-9]{4}$/.test(data.lastFour) || typeof data.cardHolderName !== 'string'
                || !/^\d{4}-\d{2}-\d{2}$/.test(data.expiryDate)) throw new Error('Unable to read your debit card.');
            setCard(data); setAccounts(accountData);
        }).catch(failure => {
            if (!cancelled) setError(failure instanceof TypeError ? 'Unable to connect. Please reload and try again.' : failure.message);
        }).finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, [customer]);

    async function post(path, body) {
        const csrf = await readResponse(await fetch(`${api}/csrf`, { credentials: 'include' }));
        if (!csrf?.token || !csrf?.headerName) throw new Error('Unable to verify the security session. Please reload.');
        return readResponse(await fetch(`${api}/atm/${path}`, {
            method: 'POST', credentials: 'include',
            headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
            body,
        }));
    }

    async function run(action) {
        if (busyRef.current) return;
        busyRef.current = true; setBusy(true); setError('');
        try { await action(); }
        catch (failure) {
            setError(failure instanceof TypeError
                ? 'Connection lost. Check your balance before repeating a money operation.' : failure.message);
            if (failure.status === 403) { setStep('PIN'); setPin(''); }
        } finally { busyRef.current = false; setBusy(false); }
    }

    function insert() {
        run(async () => {
            await post('eject'); // Start each insertion without a previous server-side verification.
            const cardBounds = cardRef.current.getBoundingClientRect();
            const slotBounds = slotRef.current.getBoundingClientRect();
            cardRef.current.style.setProperty('--travel-x', `${slotBounds.left + slotBounds.width / 2 - cardBounds.left - cardBounds.width / 2}px`);
            cardRef.current.style.setProperty('--travel-y', `${slotBounds.top + slotBounds.height / 2 - cardBounds.top - cardBounds.height / 2}px`);
            setStep('INSERTING'); setInserted(true);
        });
    }

    function eject() {
        // Sensitive inputs are cleared even if the network prevents server-side ejection.
        setPin(''); setAmount(''); setAccountId(''); setResult(null); setBalanceError('');
        run(async () => {
            await post('eject');
            setStep('INSERT_CARD'); setInserted(false);
        });
    }

    function verify(event) {
        event.preventDefault();
        if (!/^[0-9]{4}$/.test(pin)) { setError('Enter your four-digit PIN.'); return; }
        const entered = pin;
        setPin('');
        run(async () => {
            await post('pin', JSON.stringify({ pin: entered }));
            setStep('MENU');
        });
    }

    async function refreshBalance(id) {
        try {
            const fresh = await fetchAccounts();
            setAccounts(fresh);
            const updated = fresh.find(account => String(account.id) === id);
            if (typeof updated?.balance !== 'number') throw new Error('Balance unavailable.');
            setResult(previous => ({ ...previous, balance: updated.balance }));
            setBalanceError('');
        } catch {
            setBalanceError('Operation completed. Unable to refresh the balance.');
        }
    }

    function operate(event) {
        event.preventDefault();
        if (!selected) { setError('Choose an active account.'); return; }
        const normalized = amount.trim().replace(',', '.');
        if (!/^\d+(?:\.\d{1,2}0*)?$/.test(normalized) || !/[1-9]/.test(normalized)) {
            setError('Enter an amount above zero with at most two meaningful decimal places.'); return;
        }
        const [whole, fraction = ''] = normalized.split('.');
        const integer = whole.replace(/^0+(?=\d)/, '');
        if (integer.length > 17) { setError('The amount is too large.'); return; }
        const money = `${integer}.${fraction.slice(0, 2).padEnd(2, '0')}`;
        const operation = step;
        run(async () => {
            const transaction = await post(`accounts/${accountId}/${operation === 'DEPOSIT' ? 'deposits' : 'withdrawals'}`, `{"amount":${money}}`);
            setResult({ operation, amount: transaction.amount, name: selected.name, balance: null });
            setStep('RESULT'); setAmount('');
            await refreshBalance(accountId);
        });
    }

    function choose(operation) {
        if (operation === 'MENU') { setStep('MENU'); setError(''); return; }
        run(async () => {
            setAccounts(await fetchAccounts());
            setStep(operation); setAmount(''); setResult(null); setBalanceError('');
        });
    }

    const hasCard = card !== null && !loading;
    return <div className="atm-page">
        <Header logout={logout} page="dashboard" customer={customer} />
        <div className="atm-body"><SideBar logout={logout} />
            <main className="atm-main">
                <Link className="atm-back" to="/dashboard">← Back to dashboard</Link>
                <p className="atm-eyebrow">YOUR URBANK ATM</p><h1>Deposit / Withdraw</h1>
                <p className="atm-intro">Insert your debit card to move money into or out of your accounts.</p>
                <div className="atm-stage">
                    <section className="atm-card-panel" aria-label="Your URBank debit card">
                        <h2>Your debit card</h2>
                        {card ? <div ref={cardRef} className={`atm-card ${inserted ? 'atm-card-inserted' : ''}`}
                            onTransitionEnd={event => {
                                if (event.target === event.currentTarget && event.propertyName === 'transform' && step === 'INSERTING') setStep('PIN');
                            }}>
                            <BankCard cardHolderName={card.cardHolderName} lastFour={card.lastFour}
                                validDate={`${card.expiryDate.slice(5, 7)}/${card.expiryDate.slice(2, 4)}`} cvc2="•••" type="Debit" />
                        </div> : <p>{loading ? 'Loading your card…' : 'No card available.'}</p>}
                        <p className="atm-card-caption">{inserted ? 'Card inserted · Eject when finished' : 'Use your four-digit card PIN at the ATM.'}</p>
                        <p className="atm-card-caption">Need your PIN? <Link to="/my-card">Show it on My Card</Link> with your application password.</p>
                        <p className="atm-simulation">Simulated ATM · SEK only</p>
                    </section>
                    <div className="atm-machine-window"><div className="atm-machine">
                        <img className="atm-image" src={machine} alt="URBank ATM machine" />
                        <div ref={slotRef} className="atm-card-slot" aria-hidden="true" />
                        <section className="atm-screen" aria-label="ATM screen" aria-busy={busy}>
                            <div className="atm-screen-brand">URBANK <span>ATM</span></div>
                            {!customer ? <p>{customerStatus === 'unauthenticated'
                                ? <>Please <Link to="/login">sign in</Link> to use the ATM.</>
                                : customerStatus === 'error' ? 'Unable to load your session. Please reload.' : 'Loading your session…'}</p>
                                : loading ? <p role="status">Loading your card and accounts…</p> : <>
                                {step === 'INSERT_CARD' && <><h2>Welcome</h2><p>Insert your card to begin.</p>
                                    <button type="button" disabled={!hasCard || busy} onClick={insert}>{busy ? 'Preparing…' : 'Insert Card'}</button></>}
                                {step === 'INSERTING' && <p role="status">Inserting your card…</p>}
                                {step === 'PIN' && <form onSubmit={verify}><h2>Enter your PIN</h2><label htmlFor="atm-pin">Four-digit card PIN</label>
                                    <input autoFocus id="atm-pin" type="password" inputMode="numeric" autoComplete="off" maxLength={4} required
                                        pattern="[0-9]{4}" value={pin} disabled={busy} onChange={event => setPin(event.target.value)} />
                                    <button disabled={busy} type="submit">{busy ? 'Verifying…' : 'Verify PIN'}</button></form>}
                                {step === 'MENU' && <><h2>Welcome</h2><p>What would you like to do?</p>
                                    <div className="atm-menu"><button type="button" disabled={busy || active.length === 0} onClick={() => choose('DEPOSIT')}>Deposit</button>
                                        <button type="button" disabled={busy || active.length === 0} onClick={() => choose('WITHDRAW')}>Withdraw</button></div>
                                    {active.length === 0 && <p>No active accounts are available.</p>}</>}
                                {(step === 'DEPOSIT' || step === 'WITHDRAW') && <form onSubmit={operate}>
                                    <h2>{step === 'DEPOSIT' ? 'Deposit' : 'Withdraw'}</h2>
                                    <label htmlFor="atm-account">Account</label><select id="atm-account" required value={accountId} disabled={busy}
                                        onChange={event => setAccountId(event.target.value)}><option value="">Select an active account</option>
                                        {active.map(account => <option key={account.id} value={account.id}>{account.name} · {account.accountNumber.slice(-4)}</option>)}</select>
                                    {selected && <p className="atm-available">Available: {formatBalance(selected.balance)}</p>}
                                    <label htmlFor="atm-amount">Amount in SEK</label><input id="atm-amount" required inputMode="decimal" placeholder="0,00"
                                        value={amount} disabled={busy} onChange={event => setAmount(event.target.value)} />
                                    <button disabled={busy || !selected} type="submit">{busy ? 'Processing…' : step === 'DEPOSIT' ? 'Deposit' : 'Withdraw'}</button>
                                    <button className="atm-secondary" disabled={busy} type="button" onClick={() => choose('MENU')}>Back to Menu</button>
                                </form>}
                                {step === 'RESULT' && result && <div role="status"><h2>{result.operation === 'DEPOSIT' ? 'Deposit complete' : 'Withdrawal complete'}</h2>
                                    <p className="atm-result-amount">{formatBalance(result.amount)}</p><p>{result.name}</p>
                                    <p>New balance<br /><strong>{result.balance === null ? busy ? 'Refreshing…' : 'Unavailable' : formatBalance(result.balance)}</strong></p>
                                    {balanceError && <><p className="atm-error">{balanceError}</p><button type="button" disabled={busy}
                                        onClick={() => run(() => refreshBalance(accountId))}>Refresh balance</button></>}
                                    <button type="button" disabled={busy} onClick={() => choose('MENU')}>Back to Menu</button></div>}
                                {error && <p className="atm-error" role="alert">{error}</p>}
                                {step !== 'INSERT_CARD' && step !== 'INSERTING' && <button className="atm-secondary" type="button" disabled={busy} onClick={eject}>Eject Card</button>}
                            </>}
                        </section>
                    </div></div>
                </div>
            </main>
        </div><Footer />
    </div>;
}

export default DepositWithdraw;
