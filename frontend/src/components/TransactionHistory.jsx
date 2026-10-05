import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import transfer from '../assets/transfer.png';
import deposit from '../assets/deposit.png';
import withdrawal from '../assets/withdrawal.png';
import { formatBalance } from '../utils/formatBalance.js';
import '../style/TransactionHistory.css';

const types = {
    TRANSFER: { label: 'Transfer', icon: transfer },
    DEPOSIT: { label: 'Deposit', icon: deposit },
    WITHDRAWAL: { label: 'Withdrawal', icon: withdrawal },
};
const dateFormatter = new Intl.DateTimeFormat('en-GB', {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    timeZone: 'Europe/Stockholm',
});

function TransactionHistory({ accountId, accountName, recent = false }) {
    const [result, setResult] = useState({ entries: [], loading: true, error: '' });
    const [retry, setRetry] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        async function load() {
            try {
                const path = recent ? '/transactions/recent' : `/accounts/${accountId}/transactions`;
                const response = await fetch(`/api/customers${path}`, {
                    credentials: 'include', signal: controller.signal,
                });
                if (!response.ok) {
                    const error = await response.json().catch(() => null);
                    throw new Error(response.status < 500 && typeof error?.massage === 'string'
                        ? error.massage : 'Unable to load transactions. Please try again.');
                }
                const entries = await response.json();
                if (!Array.isArray(entries)) throw new Error('Unable to read transaction history.');
                if (!controller.signal.aborted) setResult({ entries, loading: false, error: '' });
            } catch (error) {
                if (!controller.signal.aborted) setResult({ entries: [], loading: false,
                    error: error instanceof TypeError ? 'Unable to load transactions. Check your connection and try again.' : error.message });
            }
        }
        load();
        return () => controller.abort();
    }, [accountId, recent, retry]);

    return <section className="transaction-history" aria-label={recent ? 'Recent transactions' : `${accountName} transaction history`}>
        <div className="history-heading">
            <div><h2>{recent ? 'Recent Transactions' : 'Transaction History'}</h2>
                <p>{recent ? 'Latest movements across your accounts' : `${accountName} · All recorded movements`} · Stockholm time</p></div>
            {recent && <Link to="/accounts">View account history →</Link>}
        </div>
        {result.loading ? <p className="history-state" role="status">Loading transactions…</p>
            : result.error ? <div className="history-state"><p role="alert">{result.error}</p>
                <button type="button" onClick={() => {
                    setResult({ entries: [], loading: true, error: '' });
                    setRetry(value => value + 1);
                }}>Try again</button></div>
            : result.entries.length === 0 ? <div className="history-state">
                <img src={transfer} alt="" /><h3>No transactions yet</h3>
                <p>Your transfers, deposits and withdrawals will appear here.</p></div>
            : <div className="history-scroll" tabIndex={0} role="region" aria-label="Transaction table">
                <table className="history-table">
                    <caption className="history-sr-only">{recent ? 'Latest five account movements' : `${accountName} history`}</caption>
                    <thead><tr><th scope="col">Date</th><th scope="col">Type</th><th scope="col">Account</th>
                        <th scope="col" className="history-amount">Amount</th><th scope="col">Status</th></tr></thead>
                    <tbody>{result.entries.map(entry => {
                        const type = types[entry.type];
                        const incoming = entry.amount > 0;
                        return <tr key={entry.ledgerEntryId}>
                            <td><time dateTime={entry.createdAt}>{dateFormatter.format(new Date(entry.createdAt))}</time></td>
                            <td><span className="history-type">{type && <img src={type.icon} alt="" />}{type?.label || entry.type}</span></td>
                            <td>{recent ? <Link to={`/accounts?account=${entry.accountId}`}>{entry.accountName}</Link> : entry.accountName}</td>
                            <td className={`history-amount ${incoming ? 'incoming' : 'outgoing'}`}>
                                <span className="history-sr-only">{incoming ? 'Incoming' : 'Outgoing'} </span>
                                {incoming ? '+' : '−'}{formatBalance(Math.abs(entry.amount))}</td>
                            <td><span className={`history-status ${entry.status.toLowerCase()}`}>{entry.status.charAt(0) + entry.status.slice(1).toLowerCase()}</span></td>
                        </tr>;
                    })}</tbody>
                </table>
            </div>}
    </section>;
}
export default TransactionHistory;
