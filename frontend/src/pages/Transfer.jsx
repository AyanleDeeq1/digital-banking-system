import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Header from '../components/Header.jsx';
import Footer from '../components/Footer.jsx';
import SideBar from '../components/SideBar.jsx';
import Button from '../components/Button.jsx';
import { formatBalance } from '../utils/formatBalance.js';
import '../style/Transfer.css';

const api = 'http://localhost:8080/api/customers';
const accountDigits = account => account?.accountNumber?.split(',')[1] ?? '';
const accountLabel = account => `${account.name} · ${account.type === 'SAVINGS' ? 'Savings' : 'Checking'} · •••• ${accountDigits(account).slice(-4)}`;

async function fetchAccounts() {
    const response = await fetch(`${api}/accounts`, { credentials: 'include' });
    if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(response.status === 401 || response.status === 403
            ? 'Your session has expired. Please sign in again.'
            : response.status < 500 && typeof body?.massage === 'string'
                ? body.massage : 'Unable to refresh accounts. Please try again.');
    }
    const data = await response.json();
    if (!Array.isArray(data)) throw new Error('Unable to read accounts. Please try again.');
    return data;
}

const accountLoadMessage = failure => failure instanceof TypeError
    ? 'Unable to load accounts. Check your connection and try again.' : failure.message;

function Transfer({ customer, customerStatus , logout}) {
    const [accounts, setAccounts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [mode, setMode] = useState('own');
    const [sourceId, setSourceId] = useState('');
    const [destinationId, setDestinationId] = useState('');
    const [recipient, setRecipient] = useState('');
    const [amount, setAmount] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const busy = useRef(false);
    const active = accounts.filter(account => account.status === 'ACTIVE');
    const source = active.find(account => String(account.id) === sourceId);
    const destination = active.find(account => String(account.id) === destinationId);

    async function loadAccounts() {
        try {
            const data = await fetchAccounts();
            setLoadError('');
            setAccounts(data);
        } catch (failure) {
            setLoadError(accountLoadMessage(failure));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        if (!customer) return;
        let cancelled = false;
        fetchAccounts().then(data => {
            if (!cancelled) { setAccounts(data); setLoadError(''); }
        }).catch(failure => {
            if (!cancelled) setLoadError(accountLoadMessage(failure));
        }).finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, [customer]);

    function changeMode(next) {
        setMode(next);
        setError('');
        setSuccess('');
    }

    async function submit(event) {
        event.preventDefault();
        if (busy.current) return;
        setError(''); setSuccess('');
        const normalized = amount.trim().replace(',', '.');
        const recipientNumber = mode === 'own' ? accountDigits(destination) : recipient.trim();
        if (!source || (mode === 'own' && (!destination || sourceId === destinationId))) {
            setError('Choose two different active accounts.'); return;
        }
        if (!/^[0-9]{10}$/.test(recipientNumber)) {
            setError('Enter the recipient’s 10-digit URBank account number.'); return;
        }
        if (!/^\d+(?:\.\d{1,2}0*)?$/.test(normalized) || !/[1-9]/.test(normalized)) {
            setError('Enter an amount greater than zero with at most two meaningful decimal places.'); return;
        }
        // Keep the decimal text exact in JSON; do not round money through a JavaScript Number.
        const [whole, fraction = ''] = normalized.split('.');
        const integer = whole.replace(/^0+(?=\d)/, '');
        if (integer.length > 17) { setError('The amount is too large.'); return; }
        const money = `${integer}.${fraction.slice(0, 2).padEnd(2, '0')}`;
        busy.current = true; setSubmitting(true);
        try {
            // Retrieve the current session token, including after login rotates CSRF credentials.
            const tokenResponse = await fetch(`${api}/csrf`, { credentials: 'include' });
            if (!tokenResponse.ok) throw new Error('Unable to prepare the transfer. Please try again.');
            const csrf = await tokenResponse.json();
            if (!csrf.token || !csrf.headerName) throw new Error('Unable to prepare the transfer. Please try again.');
            const response = await fetch(`${api}/accounts/${sourceId}/transfers`, {
                method: 'POST', credentials: 'include',
                headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
                body: `{"destinationAccountNumber":${JSON.stringify(recipientNumber)},"amount":${money}}`,
            });
            if (!response.ok) {
                const body = await response.json().catch(() => null);
                const fields = Object.values(body?.fieldErrors ?? {}).flat().join(' ');
                throw new Error(response.status < 500 && typeof body?.massage === 'string'
                    ? fields || body.massage : response.status === 403
                        ? 'Your session could not authorize this transfer. Please sign in again.'
                        : 'Unable to complete the transfer. Please check your accounts before trying again.');
            }
            setSuccess(`Transfer completed: ${formatBalance(Number(money))} sent successfully.`);
            setAmount('');
            await loadAccounts();
        } catch (failure) {
            setError(failure instanceof TypeError
                ? 'Connection lost. The transfer result could not be confirmed. Check your balances before trying again.'
                : failure.message);
        } finally {
            busy.current = false; setSubmitting(false);
        }
    }

    if (!customer) return <main className="transfer-session" aria-live="polite">
        {customerStatus === 'unauthenticated' ? <p>Please <Link to="/login">sign in</Link> to transfer money.</p>
            : customerStatus === 'error' ? <p role="alert">Unable to load your session. Please reload and try again.</p>
                : <p>Loading your session…</p>}
    </main>;

    return <div className="transfer-page">
        <Header logout={logout} page="dashboard" customer={customer} />
        <div className="transfer-body"><SideBar logout={logout} />
            <main className="transfer-main">
                <Link className="transfer-back" to="/dashboard">← Back to dashboard</Link>
                <p className="transfer-eyebrow">MOVE YOUR MONEY</p>
                <h1>Transfer Money</h1>
                <p className="transfer-intro">Between your accounts or to another URBank account. All transfers are in SEK.</p>
                <section className="transfer-panel" aria-label="Transfer details">
                    <div className="transfer-modes" aria-label="Transfer destination">
                        <button type="button" aria-pressed={mode === 'own'} disabled={submitting} onClick={() => changeMode('own')}>My Accounts</button>
                        <button type="button" aria-pressed={mode === 'other'} disabled={submitting} onClick={() => changeMode('other')}>Another Account</button>
                    </div>
                    {success && <p className="transfer-success" role="status">{success}</p>}
                    {loadError && <div className="transfer-error" role="alert"><p>{loadError}</p>
                        <Button variant="secondary" disabled={submitting} onClick={() => { setLoading(true); loadAccounts(); }}>Refresh accounts</Button>
                        <p><Link to="/login">Sign in</Link></p></div>}
                    {loading ? <p role="status">Loading balances…</p> : <>
                        {active.length === 0 && !loadError && <p>No active accounts are available for transfers.</p>}
                        {mode === 'own' && active.length === 1 && <p>You need two active accounts to transfer between your accounts.</p>}
                        <form onSubmit={submit}>
                            <fieldset disabled={submitting || !!loadError || active.length === 0}>
                                <label htmlFor="transfer-from">From account</label>
                                <select id="transfer-from" required value={sourceId} onChange={event => {
                                    setSourceId(event.target.value);
                                    if (event.target.value === destinationId) setDestinationId('');
                                    setSuccess('');
                                }}><option value="">Select an account</option>
                                    {active.map(account => <option key={account.id} value={account.id}>{accountLabel(account)}</option>)}
                                </select>
                                {source && <p className="transfer-available">Available: <strong>{formatBalance(source.balance)}</strong></p>}
                                {mode === 'own' ? <>
                                    <label htmlFor="transfer-to">To account</label>
                                    <select id="transfer-to" required value={destinationId} onChange={event => setDestinationId(event.target.value)}>
                                        <option value="">Select an account</option>
                                        {active.filter(account => String(account.id) !== sourceId).map(account =>
                                            <option key={account.id} value={account.id}>{accountLabel(account)}</option>)}
                                    </select>
                                </> : <>
                                    <label htmlFor="transfer-recipient">Recipient account number</label>
                                    <input id="transfer-recipient" inputMode="numeric" autoComplete="off" required
                                        maxLength={10} pattern="[0-9]{10}" value={recipient}
                                        onChange={event => setRecipient(event.target.value)} aria-describedby="recipient-hint" placeholder="1234567890" />
                                    <p id="recipient-hint" className="transfer-hint">Enter only the 10-digit URBank account number.</p>
                                </>}
                                <label htmlFor="transfer-amount">Amount in SEK</label>
                                <input id="transfer-amount" inputMode="decimal" required value={amount}
                                    onChange={event => setAmount(event.target.value)} placeholder="0,00" aria-describedby="amount-hint" />
                                <p id="amount-hint" className="transfer-hint">Use a comma or decimal point. No overdraft is available.</p>
                                {error && <p className="transfer-error" role="alert">{error}</p>}
                                <div className="transfer-actions"><Button variant="action" type="submit"
                                    disabled={!source || (mode === 'own' && !destination)}>{submitting ? 'Transferring…' : 'Transfer money'}</Button></div>
                            </fieldset>
                        </form>
                    </>}
                </section>
            </main>
        </div><Footer />
    </div>;
}

export default Transfer;
